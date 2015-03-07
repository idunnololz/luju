package com.ggstudios.luju;

import com.ggstudios.asm.IntermediateSource;
import com.ggstudios.asm.Operator;
import com.ggstudios.asm.Register;
import com.ggstudios.asm.RegisterExpression;
import com.ggstudios.asm.Section;
import com.ggstudios.env.BaseEnvironment;
import com.ggstudios.env.Class;
import com.ggstudios.env.Constructor;
import com.ggstudios.env.Field;
import com.ggstudios.env.Literal;
import com.ggstudios.env.Method;
import com.ggstudios.env.Modifier;
import com.ggstudios.env.Variable;
import com.ggstudios.types.ArrayAccessExpression;
import com.ggstudios.types.ArrayCreationExpression;
import com.ggstudios.types.AssignExpression;
import com.ggstudios.types.BinaryExpression;
import com.ggstudios.types.Block;
import com.ggstudios.types.CastExpression;
import com.ggstudios.types.ConstructorDecl;
import com.ggstudios.types.Expression;
import com.ggstudios.types.ExpressionStatement;
import com.ggstudios.types.FieldVariable;
import com.ggstudios.types.ForStatement;
import com.ggstudios.types.ICreationExpression;
import com.ggstudios.types.IfStatement;
import com.ggstudios.types.LiteralExpression;
import com.ggstudios.types.MethodDecl;
import com.ggstudios.types.NameVariable;
import com.ggstudios.types.ReturnStatement;
import com.ggstudios.types.Statement;
import com.ggstudios.types.UnaryExpression;
import com.ggstudios.types.VarDecl;
import com.ggstudios.types.VarInitDecl;
import com.ggstudios.types.VariableExpression;
import com.ggstudios.types.WhileStatement;
import com.ggstudios.utils.FileUtils;
import com.ggstudios.utils.ListUtils;
import com.ggstudios.utils.Print;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class CodeGenerator {
    private static final String OUTPUT_DIRECTORY = "output";
    private static final String OBJECT_INSTANCE_INIT_LABEL = "..@init";

    private static final int POINTER_SIZE = 4; // in bytes
    private static final int OBJECT_OVERHEAD = 2; // in bytes
    private static final int OBJECT_OVERHEAD_BYTES = OBJECT_OVERHEAD * 4; // in bytes

    private boolean comment = true;

    private List<Class> declaredClasses;
    private IntermediateSource[] sources;
    private IntermediateSource curSrc;

    private List<String> classLoaders;

    public void generateCode(Ast ast, Assembler assembler) {
        // Strategy for generating our sources
        // 1. For each class:
        // 2.   Generate class information. Write into .data section
        // 3.   Gather all static variables. Write into .bss section
        // 4.   Generate 'function' block to initialize all static variables
        // 5.   Generate all methods...
        // 4. Generate _start block...
        // 5.   Call all static initialization functions
        // 6.   Call test
        // 7.   Exit (with exit code in EAX)

        classLoaders = new ArrayList<>();

        declaredClasses = new ArrayList<>();
        for (FileNode fn : ast) {
            Class c = fn.getThisClass();
            if (c != null) {
                declaredClasses.add(c);
            }
        }

        sources = new IntermediateSource[declaredClasses.size()];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = new IntermediateSource();
        }

        generateSourcesForClasses();


        HashMap<String, String> fileNameToText = new HashMap<>();

        for (IntermediateSource s : sources) {
            fileNameToText.put(s.getFileName(), s.toString());
        }

        StringBuilder sb = new StringBuilder();
        for (String s : classLoaders) {
            sb.append("call\t");
            sb.append(s);
            sb.append('\n');
        }

        IntermediateSource testSource = new IntermediateSource();
        testSource.setFileName("main.s");

        testSource.setActiveSectionId(Section.TEXT);
        testSource.glabel("_start");
        for (String s : classLoaders) {
            testSource.call(s);
        }
        testSource.call("_test");
        testSource.call("__debexit");

        fileNameToText.put(testSource.getFileName(), testSource.toString());

        FileUtils.emptyDirectory(OUTPUT_DIRECTORY);
        FileUtils.writeStringsToFiles(OUTPUT_DIRECTORY, fileNameToText);

        try {
            assembler.assemble(OUTPUT_DIRECTORY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Naming conventions used in assembly files:
     * Classes will be named normally. For instance, a.b.C (where a.b is the package and C is the
     * class name) will still be named a.b.C in the assembly. Note that the default package has name
     * '@default'.
     *
     * Method names will have the form _methodName@version. For instance, if class C has a method
     * named 'a', the method will have label '_a@1'. If another method also has the name 'a' or if 'a'
     * is overloaded, '_a@2' will be used and so on.
     *
     * Constructor names will have the form _newConstructorName@version. For instance, if class C
     * has two constructors, the first constructor will have label '_newC@1' and the second '_newC@2'
     *
     * Field names will have the form ?fieldName[@verion]. For instance if class C has field 'b',
     * the field will have label '?b'.
     *
     * To avoid name clashes, labels generated by the compiler begin with @
     */

    private void generateSourcesForClasses() {
        // 1. For each class:
        // 2.   Generate class information. Write into .data section
        // 3.   Gather all static variables. Write into .bss section
        // 4.   Generate 'function' block to initialize all static variables
        // 5.   Generate all methods...
        Method entryPoint = null;

        for (Class c : declaredClasses) {
            // Before we begin, ask the class to pre generate some information that will be useful
            // to us in code generation...

            IntermediateSource src = sources[c.getId()];
            curSrc = src;
            src.setFileName(c.getCanonicalName().replace('.', '@') + ".s");

            generateStaticClassData(c);
            declareAllStaticFields(c);
            generateStaticInitCode(c);
            generateInstanceInitCode(c);

            for (Constructor constructor : c.getDeclaredConstructors()) {
                generateCodeForConstructor(constructor);
            }

            for (Method method : c.getDeclaredMethods()) {
                // entry point looks like this: static int test()
                if (Modifier.isAbstract(method.getModifiers())) continue;

                if (entryPoint == null && Modifier.isStatic(method.getModifiers()) &&
                        method.getReturnType() == BaseEnvironment.TYPE_INT &&
                        method.getParameterTypes().length == 0 && method.getName().equals("test")) {
                    entryPoint = method;

                    generateCodeForMethod("_test", entryPoint);

                } else {
                    generateCodeForMethod(method.getUniqueName(), method);
                }
            }
        }
    }

    private void generateCodeForConstructor(Constructor constructor) {
        curSrc.glabel(constructor.getUniqueName());

        ConstructorDecl cd = constructor.getConstructorDecl();
        Block b = cd.getBlock();

        curSrc.resetBpOffset();
        curSrc.push(Register.EBP);
        curSrc.mov(Register.EBP, Register.ESP);

        curSrc.call(OBJECT_INSTANCE_INIT_LABEL);

        linkArgumentAddresses(true, cd.getArguments());
        allocateSpaceForLocalVariables(b.getStatements());

        for (Statement s : b.getStatements()) {
            generateCodeForStatement(s);
        }

        RegisterExpression instanceAddr = new RegisterExpression();
        instanceAddr.set(Register.EBP, Operator.PLUS, 8);

        curSrc.mov(Register.EAX, "[" + instanceAddr.toString() + "]");

        restoreStackAndReturn();
    }

    private void generateCodeForMethod(String methodLabel, Method method) {
        curSrc.glabel(methodLabel);

        MethodDecl md = method.getMethodDecl();
        Block b = md.getBlock();

        curSrc.resetBpOffset();
        curSrc.push(Register.EBP);
        curSrc.mov(Register.EBP, Register.ESP);

        linkArgumentAddresses(!Modifier.isStatic(method.getModifiers()), md.getArguments());

        if (Modifier.isNative(method.getModifiers())) {
            // TODO
        } else {
            allocateSpaceForLocalVariables(b.getStatements());

            for (Statement s : b.getStatements()) {
                generateCodeForStatement(s);
            }
        }

        if (method.getReturnType() == BaseEnvironment.TYPE_VOID) {
            restoreStackAndReturn();
        }
    }

    private void restoreStackAndReturn() {
        curSrc.mov(Register.ESP, Register.EBP);
        curSrc.pop(Register.EBP);
        curSrc.ret();
    }

    private void generateStaticClassData(Class c) {
        // There will be one block of constant class data defined in the .data section per class
        // The structure of each class info will be:
        // <vptr>
        // <inheritance tree> = list of (<ptr_to_class>, <offset>)

        curSrc.setActiveSectionId(Section.DATA);

        curSrc.glabel(c.getVtableLabel());

        // TODO generate vtable

        curSrc.glabel(c.getCanonicalName());

        curSrc.dd(c.getVtableLabel());

        Class superClass;
        if ((superClass = c.getSuperClass()) != null) {
            curSrc.dd(superClass.getCanonicalName());
            curSrc.dd(superClass.getDeclaredMethods().size() * POINTER_SIZE);
        }

        for (Class i : c.getInterfaces()) {
            curSrc.dd(i.getCanonicalName());
            curSrc.dd(i.getDeclaredMethods().size() * POINTER_SIZE);
        }
    }

    private void generateStaticInitCode(Class c) {
        IntermediateSource src = curSrc;

        src.setActiveSectionId(Section.TEXT);
        String s = "@init" + c.getId();
        classLoaders.add(s);
        src.glabel(s);

        StringBuilder sb = new StringBuilder();

        for (Field f : c.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) continue;

            VarDecl vd = f.getVarDecl();

            if (vd instanceof VarInitDecl) {
                VarInitDecl vid = (VarInitDecl) vd;

                if (comment) {
                    curSrc.addComment(vid.toPrettyString(sb).toString());
                    sb.setLength(0);
                }

                Register r = generateCodeForExpression(vid.getExpr());

                curSrc.mov(String.format("dword [%s]", f.getUniqueName()), r.getAsm());
            } else {
                src.mov(f.getUniqueName(), 0);
            }
        }
        src.ret();
    }

    private void generateInstanceInitCode(Class c) {
        curSrc.setActiveSectionId(Section.TEXT);

        curSrc.label(OBJECT_INSTANCE_INIT_LABEL);

        // Address of instance is stored at ebp + 8
        RegisterExpression instanceAddr = new RegisterExpression();
        instanceAddr.set(Register.EBP, Operator.PLUS, 8);

        curSrc.mov(Register.EAX, "[" + instanceAddr.toString() + "]");

        StringBuilder sb = new StringBuilder();

        for (Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;

            VarDecl vd = f.getVarDecl();

            if (vd instanceof VarInitDecl) {
                curSrc.push(Register.EAX);
                VarInitDecl vid = (VarInitDecl) vd;

                if (comment) {
                    curSrc.addComment(vid.toPrettyString(sb).toString());
                    sb.setLength(0);
                }

                Register r2 = generateCodeForExpression(vid.getExpr());
                Register r1 = Register.getNextRegisterFrom(r2);
                curSrc.pop(r1);

                RegisterExpression re = getReForField(r1, f);

                curSrc.movRef(re, r2);
            } else {
                RegisterExpression re = getReForField(Register.EAX, f);
                curSrc.movRef(re, 0);
            }
        }
        curSrc.ret();
    }

    private void generateCodeForStatement(Statement s) {
        switch (s.getStatementType()) {
            case Statement.TYPE_BLOCK: {
                Block b = (Block) s;

                for (Statement st : b.getStatements()) {
                    generateCodeForStatement(st);
                }
                break;
            }
            case Statement.TYPE_EXPRESSION: {
                ExpressionStatement expr = (ExpressionStatement) s;

                if (comment) {
                    curSrc.addComment(expr.getExpr().toString());
                }

                generateCodeForExpression(expr.getExpr());
                break;
            }
            case Statement.TYPE_FOR: {
                ForStatement forStatement = (ForStatement) s;

                String label = curSrc.getFreshLabel();
                String start = curSrc.getFreshLabel();
                String exit = curSrc.getFreshLabel();

                if (comment) {
                    curSrc.addComment(forStatement.toPrettyStringNoNewLine());
                }
                if (forStatement.getForInit() != null) {
                    generateCodeForStatement(forStatement.getForInit());
                    curSrc.jmp(label);
                }
                curSrc.label(start);

                if (forStatement.getBody() != null) {
                    generateCodeForStatement(forStatement.getBody());
                }

                curSrc.label(label);
                if (forStatement.getCondition() != null) {
                    if (comment) {
                        curSrc.addComment(forStatement.getCondition().toString());
                    }

                    Register r = generateCodeForExpression(forStatement.getCondition());
                    curSrc.test(r, r);
                    curSrc.je(exit);
                }

                if (forStatement.getForUpdate() != null) {
                    generateCodeForStatement(forStatement.getForUpdate());
                }

                curSrc.jmp(start);
                curSrc.label(exit);
                break;
            }
            case Statement.TYPE_IF: {
                IfStatement ifStatement = (IfStatement) s;

                String exit = curSrc.getFreshLabel();

                String next = null;
                for (IfStatement.IfBlock b : ifStatement.getIfBlocks()) {
                    if (comment) {
                        if (next == null)
                            curSrc.addComment(String.format("if (%s)", b.getCondition()));
                        else
                            curSrc.addComment(String.format("else if (%s)", b.getCondition()));
                    }
                    next = curSrc.getFreshLabel();

                    Register r = generateCodeForExpression(b.getCondition());
                    curSrc.test(r, r);
                    curSrc.je(next);

                    generateCodeForStatement(b.getBody());
                    curSrc.jmp(exit);

                    curSrc.label(next);
                }
                if (ifStatement.getElseBlock() != null) {
                    curSrc.addComment("else");
                    generateCodeForStatement(ifStatement.getElseBlock());
                }
                curSrc.label(exit);
                break;
            }
            case Statement.TYPE_RETURN: {
                ReturnStatement returnStatement = (ReturnStatement) s;

                if (comment) {
                    curSrc.addComment(returnStatement.toString());
                }

                Expression e = returnStatement.getExpression();
                Register r = generateCodeForExpression(e);

                if (r != Register.EAX) {
                    curSrc.mov(Register.EAX, r);
                }

                restoreStackAndReturn();
                break;
            }
            case Statement.TYPE_VARDECL: {
                VarInitDecl vd = (VarInitDecl) s;

                Variable var = (Variable) vd.getProper();

                if (comment) {
                    curSrc.addComment(vd.toString());
                }

                Register r1 = generateCodeForExpression(vd.getExpr());
                curSrc.movRef(var.getAddress(), r1);
                break;
            }
            case Statement.TYPE_WHILE: {
                WhileStatement ws = (WhileStatement) s;

                if (comment) {
                    curSrc.addComment(String.format("while (%s)", ws.getCondition()));
                }

                String start = curSrc.getFreshLabel();
                String exit = curSrc.getFreshLabel();

                curSrc.label(start);
                Register r = generateCodeForExpression(ws.getCondition());
                curSrc.test(r, r);
                curSrc.je(exit);

                generateCodeForStatement(ws.getBody());

                curSrc.jmp(start);
                curSrc.label(exit);
                break;
            }
        }
    }

    private RegisterExpression generateAddressForExpression(Expression ex) {
        switch (ex.getExpressionType()) {
            case Expression.ARRAY_ACCESS_EXPRESSION: {
                ArrayAccessExpression arrayAccess = (ArrayAccessExpression) ex;
                return (RegisterExpression) generateCodeForArrayAccessExpression(arrayAccess, true);
            }
            case Expression.VARIABLE_EXPRESSION: {
                VariableExpression varExpr = (VariableExpression) ex;
                return (RegisterExpression) generateCodeForVariableExpression(varExpr, true);
            }
            default:
                throw new RuntimeException(
                        String.format("Error. Cannot generate code to get the address of an expression with type '%s'",
                                ex.getClass().getSimpleName()));
        }
    }

    private Register generateCodeForExpression(Expression ex) {
        switch (ex.getExpressionType()) {
            case Expression.ARRAY_ACCESS_EXPRESSION: {
                ArrayAccessExpression arrayAccess = (ArrayAccessExpression) ex;
                return (Register) generateCodeForArrayAccessExpression(arrayAccess, false);
            }
            case Expression.ARRAY_CREATION_EXPRESSION: {
                ArrayCreationExpression arrayCreation = (ArrayCreationExpression) ex;
                Register arraySize = generateCodeForExpression(arrayCreation.getDimExpr());
                if (arraySize != Register.EAX) {
                    curSrc.mov(Register.EAX, arraySize);
                }

                curSrc.mov(Register.EBX, Register.EAX);

                curSrc.add(Register.EAX, 1);    // for the length variable...
                curSrc.shl(Register.EAX, 2);
                curSrc.call("__malloc");

                curSrc.movRef(Register.EAX, Register.EBX);

                return Register.EAX;
            }
            case Expression.ASSIGN_EXPRESSION: {
                AssignExpression assign = (AssignExpression) ex;
                Register r2 = generateCodeForExpression(assign.getRhs());
                curSrc.pushPoint();
                curSrc.push(r2);
                curSrc.record();
                RegisterExpression r1 = generateAddressForExpression(assign.getLhs());
                r2 = Register.getNextRegisterFrom(r1);
                curSrc.stop();
                curSrc.pop(r2);
                curSrc.restorePointIfClean();

                curSrc.movRef(r1, r2);

                return r2;
            }
            case Expression.BINARY_EXPRESSION: {
                BinaryExpression binEx = (BinaryExpression) ex;
                Register r2 = generateCodeForExpression(binEx.getRightExpr());
                curSrc.push(r2);
                Register r1 = generateCodeForExpression(binEx.getLeftExpr());
                if (r1 != Register.EAX) {
                    curSrc.mov(Register.EAX, r1);
                }
                r2 = Register.EBX;
                curSrc.pop(r2);
                switch (binEx.getOp().getType()) {
                    case LT:
                    case LT_EQ:
                    case GT:
                    case GT_EQ: {
                        String l1 = curSrc.getFreshLabel();
                        String l2 = curSrc.getFreshLabel();
                        curSrc.cmp(r1, r2);
                        switch (binEx.getOp().getType()) {
                            case LT:
                                curSrc.jge(l1);
                                break;
                            case LT_EQ:
                                curSrc.jg(l1);
                                break;
                            case GT:
                                curSrc.jle(l1);
                                break;
                            case GT_EQ:
                                curSrc.jl(l1);
                                break;
                        }
                        curSrc.mov(Register.EAX, 1);
                        curSrc.jmp(l2);
                        curSrc.label(l1);
                        curSrc.mov(Register.EAX, 0);
                        curSrc.label(l2);
                        return Register.EAX;
                    }
                    case PIPE:
                        curSrc.or(r1, r2);
                        return r1;
                    case AMP:
                        curSrc.and(r1, r2);
                        return r1;
                    case PIPE_PIPE: {
                        String l1 = curSrc.getFreshLabel();
                        String l2 = curSrc.getFreshLabel();

                        curSrc.test(r1, r2);
                        curSrc.jne(l1);
                        curSrc.mov(Register.EAX, 0);
                        curSrc.jmp(l2);
                        curSrc.label(l1);
                        curSrc.mov(Register.EAX, 1);
                        curSrc.label(l2);

                        return Register.EAX;
                    }
                    case AMP_AMP: {
                        String l1 = curSrc.getFreshLabel();
                        String l2 = curSrc.getFreshLabel();

                        curSrc.test(r1, r2);
                        curSrc.je(l1);
                        curSrc.mov(Register.EAX, 1);
                        curSrc.jmp(l2);
                        curSrc.label(l1);
                        curSrc.mov(Register.EAX, 0);
                        curSrc.label(l2);

                        return Register.EAX;
                    }
                    case EQ_EQ:
                    case NEQ: {
                        String l1 = curSrc.getFreshLabel();
                        String l2 = curSrc.getFreshLabel();
                        curSrc.cmp(r1, r2);
                        if (binEx.getOp().getType() == Token.Type.NEQ) {
                            curSrc.je(l1);
                        } else {
                            curSrc.jne(l1);
                        }
                        curSrc.mov(Register.EAX, 1);
                        curSrc.jmp(l2);
                        curSrc.label(l1);
                        curSrc.mov(Register.EAX, 0);
                        curSrc.label(l2);

                        return Register.EAX;
                    }
                    case PLUS:
                        // TODO handle string concat
                        curSrc.add(r1, r2);
                        return r1;
                    case MINUS:
                        curSrc.sub(r1, r2);
                        return r1;
                    case STAR:
                        curSrc.imul(r1, r2);
                        return r1;
                    case MOD:
                        curSrc.cdq();
                        curSrc.idiv(r2);
                        return Register.EDX;
                    case FSLASH:
                        curSrc.cdq();
                        curSrc.idiv(r2);
                        return r1;
                    case INSTANCEOF:
                        // TODO
                        return Register.EAX;
                    default:
                        throw new RuntimeException(
                                String.format("Error. Name resolver does not support the '%s' operator",
                                        binEx.getOp().getType()));
                }
            }
            case Expression.ICREATION_EXPRESSION: {
                ICreationExpression instanceCreation = (ICreationExpression) ex;

                // Class memory structure:
                // <cptr> -> class_info_table
                // <vptr> -> vtable [+offset]
                // [non-static-fields]

                Class c = instanceCreation.getType().getProper();

                Constructor constructor = instanceCreation.getProper();

                List<Expression> args = instanceCreation.getArgList();
                for (Expression arg : ListUtils.reverse(args)) {
                    // since we can only store two args in registers, push the rest...
                    // we need to reserve EAX for the address of the object
                    Register r = generateCodeForExpression(arg);

                    curSrc.push(r);
                }

                int fields = c.getCompleteNonStaticFields().size();

                curSrc.mov(Register.EAX, fields + 2);
                curSrc.shl(Register.EAX, 2);
                curSrc.call("__malloc");
                curSrc.push(Register.EAX);
                curSrc.call(constructor.getUniqueName());
                int argsCount = args.size() + 1;
                curSrc.add(Register.ESP, argsCount * 4);
                return Register.EAX;
            }
            case Expression.LITERAL_EXPRESSION: {
                LiteralExpression literalExpression = (LiteralExpression) ex;
                if (literalExpression.getLiteral().getType() == Token.Type.SEMI) return Register.EAX;
                Literal lit = literalExpression.getProper();
                Register toUse = Register.EAX;
                switch (lit.getTokenType()) {
                    case STRINGLIT:
                        // TODO
                        break;
                    case CHARLIT:
                        curSrc.mov(toUse, (char)lit.getValue());
                        break;
                    case INTLIT:
                        curSrc.mov(toUse, (int)lit.getValue());
                        break;
                    case FALSE:
                        curSrc.mov(toUse, 0);
                        break;
                    case TRUE:
                        curSrc.mov(toUse, 1);
                        break;
                    case NULL:
                        curSrc.mov(toUse, 0);
                        break;
                }
                return toUse;
            }
//            case Expression.METHOD_EXPRESSION: {
//                MethodExpression meth = (MethodExpression) ex;
//                Expression e = meth.getPrefixExpression();
//                List<Class> argTypes = new ArrayList<>();
//                boolean b = inStaticContext;
//                for (Expression expr : meth.getArgList()) {
//                    argTypes.add(resolveExpression(expr, env));
//                    inStaticContext = b;
//                }
//                String methSig = Method.getMethodSignature(meth.getMethodName(), argTypes);
//                Method m;
//
//                if (e != null) {
//                    Class type;
//                    boolean methodAccessFromVariable = true;
//                    boolean staticContext = false;
//                    if (e.getExpressionType() == Expression.TYPE_OR_VARIABLE_EXPRESSION) {
//                        Object o = getFieldOrType((TypeOrVariableExpression) e, env);
//                        if (o instanceof Class) {
//                            type = (Class) o;
//                            methodAccessFromVariable = false;
//                            staticContext = true;
//                        } else {
//                            type = ((Field)o).getType();
//                        }
//                    } else {
//                        type = resolveExpression(e, env);
//                    }
//
//                    m = type.getEnvironment().lookupMethod(methSig);
//                    if (Modifier.isProtected(m.getModifiers()) && !curClass.isSubClassOf(m.getDeclaringClass())) {
//                        throw new TypeException(curClass.getFileName(), lastNode,
//                                String.format("'%s' has protected access in '%s'",
//                                        m.getName(), m.getDeclaringClass()));
//                    } else if (Modifier.isProtected(m.getModifiers()) && methodAccessFromVariable && !type.isSubClassOf(curClass)
//                            && type.getPackage() != curClass.getPackage()) {
//                        throw new TypeException(curClass.getFileName(), lastNode,
//                                String.format("'%s' has protected access in '%s'",
//                                        m.getName(), m.getDeclaringClass()));
//                    }
//
//                    if (staticContext) {
//                        if (!Modifier.isStatic(m.getModifiers())) {
//                            throw new EnvironmentException("Non static method referenced from static context",
//                                    EnvironmentException.ERROR_NON_STATIC_METHOD_FROM_STATIC_CONTEXT,
//                                    m);
//                        }
//                    } else {
//                        if (Modifier.isStatic(m.getModifiers())) {
//                            throw new EnvironmentException("Static method referenced from non static context",
//                                    EnvironmentException.ERROR_STATIC_METHOD_FROM_NON_STATIC_CONTEXT,
//                                    m);
//                        }
//                    }
//                } else {
//                    m = env.lookupMethod(methSig);
//
//                    if (inStaticContext) {
//
//                        if (!Modifier.isStatic(m.getModifiers())) {
//                            throw new EnvironmentException("Static method referenced from non static context",
//                                    EnvironmentException.ERROR_STATIC_METHOD_FROM_NON_STATIC_CONTEXT,
//                                    m);
//                        }
//                    }
//                    if (Modifier.isStatic(m.getModifiers())) {
//                        throw new TypeException(curClass.getFileName(), meth,
//                                String.format("Static method '%s' cannot be referenced from non-static context",
//                                        m.getName()));
//                    }
//                }
//                if (!inStaticContext) {
//                    if (Modifier.isStatic(m.getModifiers())) {
//                        throw new EnvironmentException("Static method referenced from non static context",
//                                EnvironmentException.ERROR_STATIC_METHOD_FROM_NON_STATIC_CONTEXT,
//                                m);
//                    }
//                }
//                return m.getReturnType();
//            }
//            case Expression.REFERENCE_TYPE: {
//                ReferenceType refType = (ReferenceType) ex;
//                Class type = env.lookupClazz(refType);
//                refType.setProper(type);
//                return type;
//            }
//            case Expression.THIS_EXPRESSION: {
//                if (inStaticContext) {
//                    throw new TypeException(curClass.getFileName(), ex,
//                            String.format("'%s' cannot be referenced from a static context",
//                                    curClass.getCanonicalName() + ".this"));
//                }
//                return curClass;
//            }
            case Expression.UNARY_EXPRESSION: {
                UnaryExpression unary = (UnaryExpression) ex;
                Register r = generateCodeForExpression(unary.getExpression());
                if (unary instanceof CastExpression) {
                    // TODO
                   return r;
                }
                // TODO

//                switch (unary.getOp().getType()) {
//                    case NOT:
//                        if (k != BaseEnvironment.TYPE_BOOLEAN) {
//                            throw new TypeException(curClass.getFileName(), unary,
//                                    String.format("Operator '!' cannot be applied to '%s'", k.getName()));
//                        }
//                        return BaseEnvironment.TYPE_BOOLEAN;
//                    case MINUS:
//                        if (Class.getCategory(k) != Class.CATEGORY_NUMBER) {
//                            throw new TypeException(curClass.getFileName(), unary,
//                                    String.format("Operator '-' cannot be applied to '%s'", k.getName()));
//                        }
//                        return k;
//                    default:
//                        throw new RuntimeException(
//                                String.format("Error. Name resolver does not support the unary '%s' operator",
//                                        unary.getOp().getType()));
//                }
                break;
            }
            case Expression.VARIABLE_EXPRESSION: {
                VariableExpression varExpr = (VariableExpression) ex;
                return (Register) generateCodeForVariableExpression(varExpr, false);
            }
//            default:
//                throw new RuntimeException(
//                        String.format("Error. Name resolver does not support the expression type '%s'",
//                                ex.getClass().getSimpleName()));
        }

        return Register.EAX;
    }

    /**
     * Generates the code to get either the value of a variable expression or the address of a
     * variable expression.
     * @param varExpr
     * @param returnAddress
     * @return Register in which the value is stored or RegisterExpression for address.
     */
    private Object generateCodeForVariableExpression(VariableExpression varExpr, boolean returnAddress) {
        if (varExpr instanceof FieldVariable) {
            FieldVariable fVar = (FieldVariable) varExpr;
            Register r = generateCodeForExpression(fVar.getPrefixExpr());

            Field f = fVar.getProper().get(0);
            RegisterExpression re = getReForField(r, f);

            //
            curSrc.lea(r, re);
            if (returnAddress) {
                re.set(r);
                return re;
            } else {
                curSrc.mov(r.getAsm(), "[" + r.getAsm()+ "]");
                return r;
            }
        } else if (varExpr instanceof NameVariable) {
            NameVariable nVar = (NameVariable) varExpr;

            RegisterExpression re = new RegisterExpression();

            List<Field> fields = varExpr.getProper();
            // TODO support form method().field
            int level = 0;
            for (Field f : fields) {
                if (Modifier.isStatic(f.getModifiers())) {
                    curSrc.linkLabel(f.getUniqueName());
                    re.set(f.getUniqueName());
                } else {
                    if (f instanceof Variable) {
                        Variable var = (Variable) f;

                        if (level != 0) {
                            throw new IllegalStateException("Nested local variable");
                        }
                        re.set(var.getAddress());
                    } else {
                        if (level == 0) {
                            // this must be accessing a field of "this" object
                            // so we must load "this" into EAX
                            re = getThisField();
                            curSrc.movRef(Register.EAX, re);
                        }

                        re = getReForField(Register.EAX, f);

                        curSrc.lea(Register.EAX, re);
                        re.set(Register.EAX);
                    }
                }

                if (!f.getType().isPrimitive()) {
                    curSrc.mov(Register.EAX, "dword [" + re.toString() + "]");
                    re.set(Register.EAX);
                }

                level++;
            }

            if (returnAddress) {
                return re;
            } else {
                curSrc.mov(Register.EAX, "dword [" + re.toString() + "]");
                return Register.EAX;
            }
        }
        return Register.EAX;
    }

    public RegisterExpression getReForField(Register objectRegister, Field field) {
        RegisterExpression re = new RegisterExpression();
        int index = field.getDeclaringClass().getFieldIndex(field);
        if (!field.getDeclaringClass().isArray()) {
            index += OBJECT_OVERHEAD;
        }
        if (index == 0) {
            re.set(objectRegister);
            return re;
        }

        index *= POINTER_SIZE;

        re.set(objectRegister, Operator.PLUS, index);

        return re;
    }

    public Object generateCodeForArrayAccessExpression(ArrayAccessExpression arrayAccess, boolean returnAddress) {
        Register r2 = generateCodeForExpression(arrayAccess.getIndexExpr());
        curSrc.push(r2);
        RegisterExpression r1 = generateAddressForExpression(arrayAccess.getArrayExpr());
        if (!r1.isUnary()) {
            curSrc.lea(Register.EAX, r1);
        } else if (r1.isLabel()) {
            curSrc.mov(Register.EAX, r1.toString());
        } else if (!r1.isRegisterUsed(Register.EAX)) {
            curSrc.mov(Register.EAX, r1.toString());
        }
        r2 = Register.EBX;
        curSrc.pop(r2);

        curSrc.call("__arrayBoundCheck");

        curSrc.add(r2, 1); // account for length var
        curSrc.shl(r2, 2);

        if (returnAddress) {
            r1.set(Register.EAX, Operator.PLUS, Register.EBX);
            return r1;
        } else {
            curSrc.mov(Register.EAX, String.format("dword [%s+%s]", Register.EAX, r2.getAsm()));
            return Register.EAX;
        }
    }

    private void linkArgumentAddresses(boolean isInstanceMethod, List<VarDecl> arguments) {
        int i;
        if (isInstanceMethod) {
            i = 12; // cause 'this' is stored at 8...
        } else {
            i = 8;
        }

        for (VarDecl vd : arguments) {
            Variable v = (Variable) vd.getProper();
            RegisterExpression re = new RegisterExpression();
            re.set(Register.EBP, Operator.PLUS, i);
            v.setAddress(re);

            i += 4;
        }
    }

    private int allocateSpaceForLocalVariables(List<Statement> statements) {
        int localVarsInBlock = 0;
        Queue<Statement> toProcess = new LinkedList<>();

        for (Statement s : statements) {
            toProcess.add(s);
        }

        while (toProcess.size() != 0) {
            Statement s = toProcess.poll();
            switch (s.getStatementType()) {
                case Statement.TYPE_BLOCK: {
                    Block b = (Block) s;
                    for (Statement ss : b.getStatements()) {
                        toProcess.add(ss);
                    }
                    break;
                }
                case Statement.TYPE_FOR: {
                    ForStatement fs = (ForStatement) s;
                    Statement st;
                    if ((st = fs.getForInit()) != null)
                        toProcess.add(st);
                    if ((st = fs.getBody()) != null)
                        toProcess.add(st);
                    break;
                }
                case Statement.TYPE_IF: {
                    IfStatement is = (IfStatement) s;

                    for (IfStatement.IfBlock b : is.getIfBlocks()) {
                        toProcess.add(b);
                    }
                    if (is.getElseBlock() != null) {
                        toProcess.add(is.getElseBlock());
                    }
                    break;
                }
                case Statement.TYPE_IF_BLOCK: {
                    IfStatement.IfBlock ib = (IfStatement.IfBlock) s;

                    if (ib.getBody() != null) {
                        toProcess.add(ib.getBody());
                    }
                    break;
                }
                case Statement.TYPE_VARDECL: {
                    VarDecl vd = (VarDecl) s;
                    Variable v = (Variable) vd.getProper();

                    RegisterExpression re = new RegisterExpression();
                    re.set(Register.EBP, Operator.MINUS, curSrc.getBpOffset() + (localVarsInBlock << 2));
                    v.setAddress(re);
                    localVarsInBlock++;
                    break;
                }
                case Statement.TYPE_WHILE: {
                    WhileStatement ws = (WhileStatement) s;
                    if (ws.getBody() != null) {
                        toProcess.add(ws.getBody());
                    }
                }
            }
        }

        int localVarSize = localVarsInBlock << 2;
        if (localVarsInBlock != 0) {
            curSrc.sub(Register.ESP, localVarSize);
        }
        return localVarSize;
    }

    private void declareAllStaticFields(Class c) {
        curSrc.setActiveSectionId(Section.BSS);

        for (Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) {
                curSrc.global(f.getUniqueName());
                curSrc.resd(f.getUniqueName());
            }
        }
    }

    public RegisterExpression getThisField() {
        // by our calling convention, "this" is stored at EBP + 8
        RegisterExpression re = new RegisterExpression();
        re.set(Register.EBP, Operator.PLUS, 8);
        return re;
    }
}
