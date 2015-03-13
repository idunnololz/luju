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
import com.ggstudios.env.Interface;
import com.ggstudios.env.Literal;
import com.ggstudios.env.Method;
import com.ggstudios.env.Modifier;
import com.ggstudios.env.Variable;
import com.ggstudios.error.TestFailedException;
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
import com.ggstudios.types.MethodExpression;
import com.ggstudios.types.NameVariable;
import com.ggstudios.types.ReferenceType;
import com.ggstudios.types.ReturnStatement;
import com.ggstudios.types.Statement;
import com.ggstudios.types.TypeOrVariableExpression;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
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
    private Class curClass;

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

        String result;
        do {
            result = assembler.getResult();
        } while (result.length() == 0);

        int res = Integer.valueOf(result);
        if (res != 123) {
            throw new TestFailedException(res);
        }
    }

    /**
     * Naming conventions used in assembly files:
     * Classes will be named normally. For instance, a.b.C (where a.b is the package and C is the
     * class name) will still be named a.b.C in the assembly. Note that the default package has name
     * '@default'.
     *
     * Each class will also define an array class (C[]). The naming of such classes will have the
     * form className#Array (eg, com.example.C#Array)
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

            if (c.isCached()) continue;

            curClass = c;

            IntermediateSource src = sources[c.getId() - 1];
            curSrc = src;
            src.setFileName(c.getCanonicalName().replace('.', '@') + ".s");

//            Print.ln(c.getName());
//            Print.ln(curSrc.getFileName());
//            Print.ln("" + c.getId());

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

        Class c = constructor.getDeclaringClass();
        if (c.getSuperClass() != null) {
            Class superClass = c.getSuperClass();
            Constructor superC = (Constructor) superClass.get(Constructor.getConstructorSignature(superClass.getName(), new ArrayList<Class>()));
            curSrc.movRef(Register.EAX, getThisField());
            curSrc.push(Register.EAX);
            curSrc.call(superC.getUniqueName());
            curSrc.add(Register.ESP, 4);
        }
        curSrc.call(OBJECT_INSTANCE_INIT_LABEL);

        curSrc.setBpOffset(4);
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

        if (comment) {
            StringBuilder sb = new StringBuilder();
            if (method.getParameterTypes().length != 0) {
                for (Class c : method.getParameterTypes()) {
                    sb.append(c.getName());
                    sb.append(", ");
                }
                sb.setLength(sb.length() - 2);
            }

            curSrc.addComment(String.format("%s %s(%s) {",
                    method.getReturnType().getName(), method.getName(),
                    sb.toString()));
        }

        curSrc.resetBpOffset();
        curSrc.push(Register.EBP);
        curSrc.mov(Register.EBP, Register.ESP);

        linkArgumentAddresses(!Modifier.isStatic(method.getModifiers()), md.getArguments());

        if (Modifier.isNative(method.getModifiers())) {
            curSrc.call("NATIVE" + curClass.getCanonicalName() + "." + method.getName());
            restoreStackAndReturn();
        } else {
            allocateSpaceForLocalVariables(b.getStatements());

            for (Statement s : b.getStatements()) {
                generateCodeForStatement(s);
            }
        }

        if (method.getReturnType() == BaseEnvironment.TYPE_VOID) {
            restoreStackAndReturn();
        }

        if (comment) {
            curSrc.addComment("}");
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
        // sizeOfTree
        // <inheritance tree> = list of (<ptr_to_class>, <offset>)

        curSrc.setActiveSectionId(Section.DATA);

        curSrc.glabel(c.getVtableLabel());

        if (c.isInterface() || Modifier.isAbstract(c.getModifiers())) {

        } else {
            for (Method m : c.getCompleteNonStaticMethodList()) {
                if (m == null) {
                    curSrc.ddHex("0DEADBEEFh");
                    //throw new RuntimeException("Wtf. Class: " + c.getCanonicalName());
                } else {
                    if (Modifier.isAbstract(m.getModifiers())) {
                        throw new RuntimeException("Abstract method in non abstract class");
                    } else {
                        curSrc.dd(m.getUniqueName());
                    }
                }
            }
        }

        curSrc.glabel(c.getCanonicalName());

        curSrc.dd(c.getVtableLabel());
        curSrc.dd((c.getSuperClass() == null ? 0 : 1) + c.getInterfaces().length);

        Class superClass;
        if ((superClass = c.getSuperClass()) != null) {
            curSrc.dd(superClass.getCanonicalName());
            curSrc.dd(superClass.getDeclaredMethods().size() * POINTER_SIZE);
        }

        for (Class i : c.getInterfaces()) {
            curSrc.dd(i.getCanonicalName());
            curSrc.dd(i.getDeclaredMethods().size() * POINTER_SIZE);
        }

        // define the array class data

        Class ac = c.getArrayClass();
        curSrc.glabel(ac.getUniqueLabel());
        curSrc.dd(ac.getVtableLabel());
        curSrc.dd((c.getSuperClass() == null ? 0 : 1) + c.getInterfaces().length + 1);

        curSrc.dd(BaseEnvironment.TYPE_OBJECT.getUniqueLabel());
        curSrc.dd(0);
        if ((superClass = c.getSuperClass()) != null) {
            ac = superClass.getArrayClass();
            curSrc.dd(ac.getUniqueLabel());
            curSrc.dd(0);
        }

        for (Class i : c.getInterfaces()) {
            ac = i.getArrayClass();
            curSrc.dd(ac.getUniqueLabel());
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
        Register instanceR = Register.EAX;

        for (Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;

            VarDecl vd = f.getVarDecl();

            if (!(vd instanceof VarInitDecl)) {
                RegisterExpression re = getReForField(Register.EAX, f);
                curSrc.movRef(re, 0);
            }
        }

        for (Field f : c.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;

            VarDecl vd = f.getVarDecl();

            if (vd instanceof VarInitDecl) {
                curSrc.push(instanceR);
                VarInitDecl vid = (VarInitDecl) vd;

                if (comment) {
                    curSrc.addComment(vid.toPrettyString(sb).toString());
                    sb.setLength(0);
                }

                Register r2 = generateCodeForExpression(vid.getExpr());
                Register r1 = Register.getNextRegisterFrom(r2);
                curSrc.pop(r1);
                instanceR = r1;

                RegisterExpression re = getReForField(r1, f);

                curSrc.movRef(re, r2);
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

                if (forStatement.getForUpdate() != null) {
                    generateCodeForStatement(forStatement.getForUpdate());
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

                if (forStatement.getBody() != null) {
                    generateCodeForStatement(forStatement.getBody());
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
            case Expression.UNARY_EXPRESSION:
            case Expression.METHOD_EXPRESSION: {
                Register r = generateCodeForExpression(ex);
                RegisterExpression re = new RegisterExpression();
                re.set(r);
                return re;
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

                curSrc.add(Register.EAX, 3);    // for the length variable...
                curSrc.shl(Register.EAX, 2);
                curSrc.call("__malloc");
                curSrc.call("__zeroArray");

                Class o = arrayCreation.getClassType();

                RegisterExpression re = new RegisterExpression();
                re.set(Register.EAX);
                curSrc.movRef(re, o.getUniqueLabel());
                re.set(Register.EAX, Operator.PLUS, 4);
                curSrc.movRef(re, o.getVtableLabel());
                re.set(Register.EAX, Operator.PLUS, 8);
                curSrc.movRef(re, Register.EBX);

                return Register.EAX;
            }
            case Expression.ASSIGN_EXPRESSION: {
                AssignExpression assign = (AssignExpression) ex;
                RegisterExpression re = generateAddressForExpression(assign.getLhs());
                re.dumpToSingleRegister(curSrc, Register.EAX);
                curSrc.push(Register.EAX);
                Register r2 = generateCodeForExpression(assign.getRhs());
                Register r1 = Register.getNextRegisterFrom(r2);
                curSrc.pop(r1);

                curSrc.movRef(r1, r2);

                return r2;
            }
            case Expression.BINARY_EXPRESSION: {
                BinaryExpression binEx = (BinaryExpression) ex;
                Register r1 = null;
                Register r2 = null;
                Token.Type t = binEx.getOp().getType();
                if (t != Token.Type.AMP_AMP && t != Token.Type.PIPE_PIPE) {
                    r1 = generateCodeForExpression(binEx.getLeftExpr());
                    curSrc.push(r1);
                    r2 = generateCodeForExpression(binEx.getRightExpr());
                    if (r2 != Register.EBX) {
                        curSrc.mov(Register.EBX, r2);
                        r2 = Register.EBX;
                    }
                    r1 = Register.EAX;
                    curSrc.pop(r1);
                }
                switch (t) {
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

                        r1 = generateCodeForExpression(binEx.getLeftExpr());
                        curSrc.test(r1, r1);
                        curSrc.jne(l1);
                        r2 = generateCodeForExpression(binEx.getRightExpr());
                        curSrc.test(r2, r2);
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

                        r1 = generateCodeForExpression(binEx.getLeftExpr());
                        curSrc.test(r1, r1);
                        curSrc.je(l1);
                        r2 = generateCodeForExpression(binEx.getRightExpr());
                        curSrc.test(r2, r2);
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
                    case PLUS: {
                        Class lType = binEx.getLeftExpr().getClassType();
                        Class rType = binEx.getRightExpr().getClassType();
                        if (lType == BaseEnvironment.TYPE_STRING
                                || rType == BaseEnvironment.TYPE_STRING) {

                            if (rType != BaseEnvironment.TYPE_STRING) {
                                List<Class> argTypes = new ArrayList<>();
                                if (rType == BaseEnvironment.TYPE_NULL) {
                                    argTypes.add(BaseEnvironment.TYPE_OBJECT);
                                } else if (rType.isPrimitive()) {
                                    argTypes.add(rType);
                                } else {
                                    argTypes.add(BaseEnvironment.TYPE_OBJECT);
                                }
                                curSrc.push(r1);
                                Method valueOf = (Method) BaseEnvironment.TYPE_STRING.get(Method.getMethodSignature("valueOf", argTypes));
                                curSrc.push(r2);
                                curSrc.call(valueOf.getUniqueName());
                                curSrc.add(Register.ESP, valueOf.getParameterTypes().length * 4);
                                if (r2 != Register.EAX) {
                                    curSrc.mov(r2, Register.EAX);
                                }
                                curSrc.pop(r1);
                            } else if (lType != BaseEnvironment.TYPE_STRING) {
                                List<Class> argTypes = new ArrayList<>();
                                if (lType == BaseEnvironment.TYPE_NULL) {
                                    argTypes.add(BaseEnvironment.TYPE_OBJECT);
                                } else if (lType.isPrimitive()) {
                                    argTypes.add(lType);
                                } else {
                                    argTypes.add(BaseEnvironment.TYPE_OBJECT);
                                }
                                curSrc.push(r2);
                                Method valueOf = (Method) BaseEnvironment.TYPE_STRING.get(Method.getMethodSignature("valueOf", argTypes));
                                curSrc.push(r1);
                                curSrc.call(valueOf.getUniqueName());
                                curSrc.add(Register.ESP, valueOf.getParameterTypes().length * 4);
                                if (r1 != Register.EAX) {
                                    curSrc.mov(r1, Register.EAX);
                                }
                                curSrc.pop(r2);
                            }

                            // String concat...
                            List<Class> argTypes = new ArrayList<>();
                            argTypes.add(BaseEnvironment.TYPE_STRING);
                            Method concat = (Method) BaseEnvironment.TYPE_STRING.get(Method.getMethodSignature("concat", argTypes));
                            curSrc.push(r2);
                            curSrc.push(r1);
                            curSrc.call(concat.getUniqueName());
                            curSrc.add(Register.ESP, concat.getParameterTypes().length * 4 + 4);
                            return Register.EAX;
                        } else {
                            curSrc.add(r1, r2);
                            return r1;
                        }
                    }
                    case MINUS:
                        curSrc.sub(r1, r2);
                        return r1;
                    case STAR:
                        curSrc.imul(r1, r2);
                        return r1;
                    case MOD:
                        curSrc.call("__divideCheck");
                        curSrc.cdq();
                        curSrc.idiv(r2);
                        return Register.EDX;
                    case FSLASH:
                        curSrc.call("__divideCheck");
                        curSrc.cdq();
                        curSrc.idiv(r2);
                        return r1;
                    case INSTANCEOF: {
                        String fls = curSrc.getFreshLabel();
                        String exit = curSrc.getFreshLabel();

                        curSrc.push(r2);

                        curSrc.test(r1, r1);
                        curSrc.je(fls);

                        curSrc.mov(r1, String.format("[%s]", r1));
                        curSrc.push(r1);
                        curSrc.call("__instanceOf");
                        curSrc.add(Register.ESP, 8);
                        curSrc.jmp(exit);

                        curSrc.label(fls);
                        curSrc.mov(Register.EAX, 0);
                        curSrc.label(exit);
                        return Register.EAX;
                    }
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
                return createInstance(c, constructor, args);
            }
            case Expression.LITERAL_EXPRESSION: {
                LiteralExpression literalExpression = (LiteralExpression) ex;
                if (literalExpression.getLiteral().getType() == Token.Type.SEMI) return Register.EAX;
                Literal lit = literalExpression.getProper();
                Register toUse = Register.EAX;
                switch (lit.getTokenType()) {
                    case STRINGLIT:
                        String t = curSrc.getFreshContextFreeLabel();
                        curSrc.setActiveSectionId(Section.DATA);
                        curSrc.declareString(t, (String) lit.getValue());
                        curSrc.setActiveSectionId(Section.TEXT);

                        Class string = BaseEnvironment.TYPE_STRING;
                        List<Class> argTypes = new ArrayList<>();
                        argTypes.add(BaseEnvironment.TYPE_CHAR.getArrayClass());
                        Constructor c = (Constructor) string.get(Constructor.getConstructorSignature(string.getName(), argTypes));

                        curSrc.push(t);
                        createInstance(string, c, new ArrayList<Expression>());
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
            case Expression.METHOD_EXPRESSION: {
                MethodExpression me = (MethodExpression) ex;
                Expression pre = me.getPrefixExpression();
                List<Expression> expressions = me.getArgList();
                Method m = me.getProper();

                if (Modifier.isStatic(m.getModifiers())) {
                    pushArguments(expressions, m.getParameterTypes());

                    curSrc.call(m.getUniqueName());
                    curSrc.add(Register.ESP, m.getParameterTypes().length * 4);
                } else {
                    pushArguments(expressions, m.getParameterTypes());
                    if (pre != null) {
                        if (pre.getExpressionType() == Expression.TYPE_OR_VARIABLE_EXPRESSION) {
                            // Since we are sure this method isn't static, the pre-expression must
                            // be a field...
                            List<Field> fields = ((TypeOrVariableExpression) pre).getProper();
                            RegisterExpression re = getAddressForFieldList(fields);

                            int index = m.getDeclaringClass().getMethodIndex(m);
                            curSrc.movRef(Register.EAX, re);
                            curSrc.push(Register.EAX);
                            curSrc.movRef(Register.EAX, getVtableAddress(Register.EAX));
                            curSrc.movRef(Register.EAX, getMethodAddress(Register.EAX, index));
                            curSrc.call(Register.EAX);
                        } else {
                            int index = m.getDeclaringClass().getMethodIndex(m);
                            Register r = generateCodeForExpression(pre);
                            curSrc.push(r);
                            curSrc.movRef(Register.EAX, getVtableAddress(r));
                            curSrc.movRef(Register.EAX, getMethodAddress(Register.EAX, index));
                            curSrc.call(Register.EAX);
                        }
                    } else {
                        curSrc.movRef(Register.EAX, getThisField());
                        curSrc.push(Register.EAX);

                        int index = curClass.getMethodIndex(m);
                        curSrc.movRef(Register.EAX, getVtableAddress(Register.EAX));
                        curSrc.movRef(Register.EAX, getMethodAddress(Register.EAX, index));
                        curSrc.call(Register.EAX);
                    }

                    curSrc.add(Register.ESP, m.getParameterTypes().length * 4 + 4);
                }

                return Register.EAX;
            }
            case Expression.REFERENCE_TYPE: {
                ReferenceType refType = (ReferenceType) ex;
                return loadReferenceType(refType, Register.EAX);
            }
            case Expression.THIS_EXPRESSION: {
                curSrc.movRef(Register.EAX, getThisField());
                return Register.EAX;
            }
            case Expression.UNARY_EXPRESSION: {
                UnaryExpression unary = (UnaryExpression) ex;
                if (unary instanceof CastExpression) {
                    CastExpression ce = (CastExpression) unary;

                    return generateCodeForCastExpression(ce);
                }

                Register r = generateCodeForExpression(unary.getExpression());

                switch (unary.getOp().getType()) {
                    case NOT:
                        String f = curSrc.getFreshLabel();
                        String exit = curSrc.getFreshLabel();

                        curSrc.test(r, r);
                        curSrc.jne(f);
                        curSrc.mov(Register.EAX, 1);
                        curSrc.jmp(exit);
                        curSrc.label(f);
                        curSrc.mov(Register.EAX, 0);
                        curSrc.label(exit);
                        return Register.EAX;
                    case MINUS:
                        curSrc.neg(r);
                        return r;
                    default:
                        throw new RuntimeException(
                                String.format("Error. Name resolver does not support the unary '%s' operator",
                                        unary.getOp().getType()));
                }
            }
            case Expression.VARIABLE_EXPRESSION: {
                VariableExpression varExpr = (VariableExpression) ex;
                return (Register) generateCodeForVariableExpression(varExpr, false);
            }
            default:
                throw new RuntimeException(
                        String.format("Error. Code generator does not support the expression type '%s'",
                                ex.getClass().getSimpleName()));
        }
    }

    private Register generateCodeForCastExpression(CastExpression ce) {
        // One important thing to note about casts is that casting a null to a type is ok...
        // There are a few cases we need to handle...
        // Casting to SERIALIZABLE or CLONEABLE does nothing...
        // Casting a class/interface to itself does nothing...
        // Casting from one class to another class does nothing...
        // Casting from a primitive type to a primitive type may require attention
        // especially when casting from a larger type to a smaller one
        // Casting from an Interface to a Class/Interface requires attention
        // Casting from a Class to an Interface requires attention

        Class castTo = ce.getClassType();
        Class castFrom = ce.getExpression().getClassType();

        Register r1 = generateCodeForExpression(ce.getExpression());
        if (r1 != Register.EAX) {
            curSrc.mov(Register.EAX, r1);
        }

        if (castTo.isPrimitive()) {
            // special case!
            int newSize = Class.getPrimitiveSize(castTo);
            int oldSize = Class.getPrimitiveSize(castFrom);

            if (newSize > oldSize) {
                return r1;
            } else {
                if (castTo == BaseEnvironment.TYPE_CHAR) {
                    curSrc.movzx(r1, Register.AX);
                } else if (newSize == 2) {
                    curSrc.movsx(r1, Register.AX);
                } else if (newSize == 1) {
                    curSrc.movsx(r1, Register.AL);
                }
            }
            return r1;
        }

        if (castTo == BaseEnvironment.TYPE_SERIALIZABLE ||
                castTo == BaseEnvironment.TYPE_CLONEABLE ||
                castTo == ce.getExpression().getClassType() ||
                (!castTo.isInterface() && !ce.getExpression().getClassType().isInterface())) {
            return r1;
        }

        Register r2 = loadReferenceType(ce.getClassType(), Register.EBX);

        RegisterExpression re = new RegisterExpression();

        String doCast = curSrc.getFreshLabel();
        String exit = curSrc.getFreshLabel();

        curSrc.test(r1, r1);
        curSrc.je(exit);

        curSrc.push(r1);
        curSrc.push(0);
        curSrc.push(r2);

        curSrc.mov(r1, String.format("[%s]", r1));
        curSrc.push(r1);
        curSrc.call("__checkCast");
        curSrc.add(Register.ESP, 12);
        curSrc.cmp(Register.EAX, -1);
        curSrc.jne(doCast);
        curSrc.call("__exception");

        curSrc.label(doCast);
        curSrc.pop(Register.EBX);
        re.set(Register.EBX);
        curSrc.movRef(Register.ECX, re);
        re.set(Register.ECX);
        curSrc.movRef(Register.ECX, re);
        curSrc.add(Register.ECX, Register.EAX);
        re.set(Register.EBX, Operator.PLUS, 4);
        curSrc.movRef(re, Register.ECX);
        curSrc.mov(r1, Register.EBX);

        curSrc.label(exit);
        return r1;
    }

    public Register createInstance(Class c, Constructor constructor, List<Expression> args) {
        pushArguments(args, constructor.getParameterTypes());

        int fields = c.getCompleteNonStaticFields().size();

        curSrc.mov(Register.EAX, fields + 2);
        curSrc.shl(Register.EAX, 2);
        curSrc.call("__malloc");

        RegisterExpression re = new RegisterExpression();
        re.set(Register.EAX);
        curSrc.movRef(re, c.getCanonicalName());
        curSrc.movRef(getVtableAddress(Register.EAX), c.getVtableLabel());

        curSrc.push(Register.EAX);
        curSrc.call(constructor.getUniqueName());
        int argsCount = constructor.getParameterTypes().length + 1;
        curSrc.add(Register.ESP, argsCount * 4);
        return Register.EAX;
    }

    public void pushArguments(List<Expression> args, Class[] argTypes) {
        for (Expression e: ListUtils.reverse(args)) {
            Register r = generateCodeForExpression(e);
            curSrc.push(r);
        }
    }

    public Register loadReferenceType(ReferenceType refType, Register r) {
        return loadReferenceType(refType.getProper(), r);
    }

    public Register loadReferenceType(Class type, Register r) {
        String l = type.getUniqueLabel();
        curSrc.linkLabel(l);
        curSrc.mov(r, l);

        return r;
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
            // probably from expressions of the form method().field
            FieldVariable fVar = (FieldVariable) varExpr;
            Register r = generateCodeForExpression(fVar.getPrefixExpr());

            Field f = fVar.getProper().get(0);
            RegisterExpression re = getReForField(r, f);

            //

//            if (!f.getType().isPrimitive()) {
//                curSrc.mov(Register.EAX, "dword [" + re.toString() + "]");
//                re.set(Register.EAX);
//            } else {
//                curSrc.lea(r, re);
//            }
            curSrc.lea(r, re);

            if (returnAddress) {
                re.set(r);
                return re;
            } else {
                curSrc.mov(r.getAsm(), "[" + r.getAsm()+ "]");
                return r;
            }
        } else if (varExpr instanceof NameVariable) {
            // probably from expressions of the form field.field
            NameVariable nVar = (NameVariable) varExpr;

            List<Field> fields = varExpr.getProper();

            RegisterExpression re = getAddressForFieldList(fields);

            if (returnAddress) {
                return re;
            } else {
                curSrc.mov(Register.EAX, "dword [" + re.toString() + "]");
                return Register.EAX;
            }
        }
        throw new RuntimeException("Wtf... this ain't no variable expression son.");
    }

    private RegisterExpression getAddressForFieldList(List<Field> fields) {
        RegisterExpression re = new RegisterExpression();

        int level = 0;
        for (Field f : fields) {
            if (level != 0) {
                curSrc.mov(Register.EAX, "dword [" + re.toString() + "]");
                re.set(Register.EAX);
            }

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

            level++;
        }

        return re;
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
        Register r1 = generateCodeForExpression(arrayAccess.getArrayExpr());
        curSrc.push(r1);
        Register r2 = generateCodeForExpression(arrayAccess.getIndexExpr());
        if (r2 != Register.EBX) {
            curSrc.mov(Register.EBX, r2);
            r2 = Register.EBX;
        }
        r1 = Register.EAX;
        curSrc.pop(r1);

        curSrc.call("__arrayBoundCheck");

        curSrc.add(r2, 3); // account for array header
        curSrc.shl(r2, 2);

        if (returnAddress) {
            RegisterExpression re = new RegisterExpression();
            re.set(r1, Operator.PLUS, r2);
            return re;
        } else {
            curSrc.mov(Register.EAX, String.format("dword [%s+%s]", r1.getAsm(), r2.getAsm()));
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
                curSrc.gresd(f.getUniqueName());
            }
        }
    }

    public RegisterExpression getThisField() {
        // by our calling convention, "this" is stored at EBP + 8
        RegisterExpression re = new RegisterExpression();
        re.set(Register.EBP, Operator.PLUS, 8);
        return re;
    }

    public RegisterExpression getVtableAddress(Register instanceAddr) {
        RegisterExpression re = new RegisterExpression();
        re.set(instanceAddr, Operator.PLUS, 4);
        return re;
    }

    public RegisterExpression getMethodAddress(Register vtableAddress, int methodIndex) {
        RegisterExpression re = new RegisterExpression();
        re.set(vtableAddress, Operator.PLUS, methodIndex * 4);
        return re;
    }
}
