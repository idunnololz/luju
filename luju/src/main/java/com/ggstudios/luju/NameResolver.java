package com.ggstudios.luju;

import com.ggstudios.env.*;
import com.ggstudios.env.Class;
import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.IncompatibleTypeException;
import com.ggstudios.error.InconvertibleTypeException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.error.TypeException;
import com.ggstudios.types.ArrayAccessExpression;
import com.ggstudios.types.ArrayCreationExpression;
import com.ggstudios.types.AssignExpression;
import com.ggstudios.types.AstNode;
import com.ggstudios.types.BinaryExpression;
import com.ggstudios.types.Block;
import com.ggstudios.types.CastExpression;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.ConstructorDecl;
import com.ggstudios.types.Expression;
import com.ggstudios.types.ExpressionStatement;
import com.ggstudios.types.FieldVariable;
import com.ggstudios.types.ICreationExpression;
import com.ggstudios.types.LiteralExpression;
import com.ggstudios.types.MethodDecl;
import com.ggstudios.types.MethodExpression;
import com.ggstudios.types.NameVariable;
import com.ggstudios.types.ReferenceType;
import com.ggstudios.types.Statement;
import com.ggstudios.types.TypeDecl;
import com.ggstudios.types.UnaryExpression;
import com.ggstudios.types.VarDecl;
import com.ggstudios.types.VarInitDecl;
import com.ggstudios.types.VariableExpression;
import com.ggstudios.utils.ClassUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class NameResolver {
    private BaseEnvironment baseEnvironment;

    private Class curClass;

    public void resolveNames(Ast ast) {
        baseEnvironment = new BaseEnvironment(ast);

        // strategy for resolving everything:
        // 1.  We start with an environment that only contains all class members
        // 2.  We then start to look at each class
        // 3.  For each class:
        // 4.    Create an environment.
        // 5.    Add the imports to the environment.
        // 6.    For each Method (and constructor):
        // 7.      Link types (including return values).
        // 8.      Generate Method signatures.
        // 9.      Add to the BaseEnvironment.
        // 10. Check for cyclic inheritance.
        // i1. For each class: (Hierarchy check)
        // i2.   Check if the class extends another class.
        // i3.   Check if the other class is 'complete'. If not, complete that class.
        // i4.   Add all of the other class's public/protected members to this class.
        // i5.   If there is an override:
        // i6.     If it is a method, check that the return type of the method is the same.
        // i7.     If it is a field, override that field.
        // i8.   If there is an abstract method:
        // i9.     Check that the method is implemented. If not, ensure the class is abstract.
        // ia.   // Interfaze stuff...
        // 11. We are now set to type link everything.
        // 12. For each class:
        // 13.   For each VarInitDecl:
        // 14.     Link all variables in expressions. (Ensure var is declared before it)
        // 15.   For each method (and constructor):
        // 16.     Link all types of local variables.
        // 17.     Link all local variables within statements.

        int len = ast.size();
        for (int i = 0; i < len; i++) {
            FileNode fn = ast.get(i);
            Class clazz = baseEnvironment.lookupClazz((fn.getPackageName() + "." + fn.getTypeDecl().getTypeName()).split("\\."), false);
            buildBaseEnvironment(fn, clazz);
        }

        // check for cyclic class extends...
        List<Class> classes = baseEnvironment.getAllClasses();
        List<Interface> interfaces = new ArrayList<>();
        List<Class> realClasses = new ArrayList<>();
        Map<Class, Integer> interfaceToIndex = new HashMap<>();

        Class p1, p2;
        for (com.ggstudios.env.Class c : classes) {
            if (c.isInterface()) {
                interfaceToIndex.put(c, interfaces.size());
                interfaces.add((Interface) c);
                continue;
            } else {
                realClasses.add(c);
            }

            p1 = c.getSuperClass();
            if (p1 == null) continue;
            p2 = c.getSuperClass().getSuperClass();

            while (true) {
                if (p1 == p2) {
                    throw new NameResolutionException(p1.getFileName(), p1.getClassDecl(),
                            String.format("Cyclic inheritance involving '%s'", p1.getCanonicalName()));
                }

                p1 = p1.getSuperClass();
                if (p2 == null) break;
                p2 = p2.getSuperClass();
                if (p2 == null) break;
                p2 = p2.getSuperClass();
            }
        }

        // check for cyclic interface extends...
        Interface i = ClassUtils.isCyclic(interfaces, interfaceToIndex);
        if (i != null) {
            throw new NameResolutionException(i.getFileName(), i.getClassDecl(),
                    String.format("Cyclic inheritance involving '%s'", i.getCanonicalName()));
        }

        buildHierarchyEnvironment(baseEnvironment.getAllClasses());

        for (Class c : classes) {
            // 13.   For each VarInitDecl:
            // 14.     Link all variables in expressions. (Ensure var is declared before it)
            // 15.   For each method (and constructor):
            // 16.     Link all types of local variables.
            // 17.     Link all local variables within statements.
            Environment env = c.getEnvironment();

            curClass = c;
            boolean isNonAbstractClass = !Modifier.isAbstract(c.getModifiers()) && !c.isInterface();

            TypeDecl typeDecl = c.getClassDecl();
            if (!c.isInterface()) {
                ClassDecl cDecl = (ClassDecl) typeDecl;
                for (VarDecl vd : cDecl.getFieldDeclarations()) {
                    try {
                        if (vd instanceof VarInitDecl) {
                            VarInitDecl vid = (VarInitDecl) vd;
                            Expression expr = vid.getExpr();
                            resolveExpression(expr, env);
                            // TODO check that the expression type matches...
                        }
                    } catch (EnvironmentException e) {
                        throwNameResolutionException(e, c.getFileName(), vd);
                    }
                }

                for (ConstructorDecl cd : cDecl.getConstructorDeclaration()) {
                    Environment curEnv = env;
                    for (VarDecl vd : cd.getArguments()) {
                        try {
                            Variable v = new Variable(cd.getMethod(), vd, curEnv);
                            curEnv = new LocalVariableEnvironment(v.getName(), v, curEnv);
                        } catch (EnvironmentException e) {
                            throwNameResolutionException(e, c.getFileName(), vd);
                        }
                    }

                    Block b = cd.getBlock();
                    if (b == null) continue; // this can happen with native/abstract methods...
                    for (Statement s : b.getStatements()) {
                        try {
                            curEnv = resolveStatement(s, curEnv);
                        } catch (EnvironmentException e) {
                            throwNameResolutionException(e, c.getFileName(), s);
                        }
                    }
                }
            }

            for (MethodDecl meth : typeDecl.getMethodDeclarations()) {
                if (meth.isAbstract() && isNonAbstractClass) {
                    throw new NameResolutionException(c.getFileName(), meth,
                            "Abstract method in non-abstract class");
                }
                Environment curEnv = env;
                for (VarDecl vd : meth.getArguments()) {
                    try {
                        Variable v = new Variable(meth.getMethod(), vd, curEnv);
                        curEnv = new LocalVariableEnvironment(v.getName(), v, curEnv);
                    } catch (EnvironmentException e) {
                        throwNameResolutionException(e, c.getFileName(), vd);
                    }
                }

                Block b = meth.getBlock();
                if (b == null) continue; // this can happen with native/abstract methods...
                for (Statement s : b.getStatements()) {
                    try {
                        curEnv = resolveStatement(s, curEnv);
                    } catch (EnvironmentException e) {
                        throwNameResolutionException(e, c.getFileName(), s);
                    }
                }
            }
        }
    }

    private Environment resolveStatement(Statement s, Environment env) {
        switch (s.getStatementType()) {
            case Statement.TYPE_BLOCK: {
                Block b = (Block) s;
                Environment oldEnv = env;
                for (Statement st : b.getStatements()) {
                    env = resolveStatement(st, env);
                }
                env = oldEnv;
                break;
            }
            case Statement.TYPE_EXPRESSION: {
                ExpressionStatement expr = (ExpressionStatement) s;
                resolveExpression(expr.getExpr(), env);
                break;
            }
            case Statement.TYPE_FOR:
            case Statement.TYPE_IF:
            case Statement.TYPE_ELSE_BLOCK:
            case Statement.TYPE_IF_BLOCK:
            case Statement.TYPE_RETURN:
                break;
            case Statement.TYPE_VARDECL:
                VarDecl vd = (VarDecl) s;
                Variable var = new Variable(null, vd, env);
                if (vd instanceof VarInitDecl) {
                    VarInitDecl vid = (VarInitDecl) vd;
                    Class type = resolveExpression(vid.getExpr(), env);
                    if (!Class.isValidAssign(var.getType(), type)) {
                        throw new IncompatibleTypeException(curClass.getFileName(), vd,
                                        var.getType(), type);
                    }
                }

                env = new LocalVariableEnvironment(var.getName(), var, env);
                break;
            case Statement.TYPE_WHILE:
                break;
        }
        return env;
    }

    public Class resolveExpression(Expression ex, Environment env) {
        switch (ex.getExpressionType()) {
            case Expression.ARRAY_ACCESS_EXPRESSION: {
                ArrayAccessExpression arrayAccess = (ArrayAccessExpression) ex;
                Class varType = resolveExpression(arrayAccess.getArrayExpr(), env);
                Class indexType = resolveExpression(arrayAccess.getIndexExpr(), env);
                if (!varType.isArray()) {
                    throw new TypeException(curClass.getFileName(), ex,
                            String.format("Array type expected; found: '%s'", varType.getCanonicalName()));
                } else if (indexType != BaseEnvironment.TYPE_INT) {
                    throw new IncompatibleTypeException(curClass.getFileName(), ex,
                            BaseEnvironment.TYPE_INT, indexType);
                }
                return varType.getSuperClass();
            }
            case Expression.ARRAY_CREATION_EXPRESSION: {
                ArrayCreationExpression arrayCreation = (ArrayCreationExpression) ex;
                Class varType = resolveExpression(arrayCreation.getTypeExpr(), env);
                Class indexType = resolveExpression(arrayCreation.getDimExpr(), env);
                if (indexType != BaseEnvironment.TYPE_INT) {
                    throw new IncompatibleTypeException(curClass.getFileName(), ex,
                            BaseEnvironment.TYPE_INT, indexType);
                }
                return varType.getArrayClass();
            }
            case Expression.ASSIGN_EXPRESSION: {
                AssignExpression assign = (AssignExpression) ex;
                Class lhsType = resolveExpression(assign.getLhs(), env);
                Class rhsType = resolveExpression(assign.getRhs(), env);
                if (!Class.isValidAssign(lhsType, rhsType)) {
                    throw new IncompatibleTypeException(curClass.getFileName(), ex,
                            lhsType, rhsType);
                }
                return lhsType;
            }
            case Expression.BINARY_EXPRESSION: {
                BinaryExpression binEx = (BinaryExpression) ex;
                Class l = resolveExpression(binEx.getLeftExpr(),  env);
                Class r = resolveExpression(binEx.getRightExpr(),  env);
                switch (binEx.getOp().getType()) {
                    case LT:
                    case LT_EQ:
                    case GT:
                    case GT_EQ:
                        if (l != BaseEnvironment.TYPE_INT || r != BaseEnvironment.TYPE_INT) {
                            throw new TypeException(curClass.getFileName(), ex,
                                    String.format("Operator '%s' cannot be applied to '%s', '%s'",
                                            binEx.getOp().getType(),
                                            l.getCanonicalName(),
                                            r.getCanonicalName()));
                        }
                        return BaseEnvironment.TYPE_BOOLEAN;
                    case PIPE:
                    case PIPE_PIPE:
                    case AMP:
                    case AMP_AMP:
                        if (l != BaseEnvironment.TYPE_BOOLEAN || r != BaseEnvironment.TYPE_BOOLEAN) {
                            throw new TypeException(curClass.getFileName(), ex,
                                    String.format("Operator '%s' cannot be applied to '%s', '%s'",
                                            binEx.getOp().getType(),
                                            l.getCanonicalName(),
                                            r.getCanonicalName()));
                        }
                        return BaseEnvironment.TYPE_BOOLEAN;
                    case EQ_EQ:
                    case NEQ:
                        return BaseEnvironment.TYPE_BOOLEAN;
                    case PLUS:
                        if (l == BaseEnvironment.TYPE_STRING || r == BaseEnvironment.TYPE_STRING)
                            return BaseEnvironment.TYPE_STRING;
                        if (l != BaseEnvironment.TYPE_INT || r != BaseEnvironment.TYPE_INT) {
                            throw new TypeException(curClass.getFileName(), ex,
                                    String.format("Operator '%s' cannot be applied to '%s', '%s'",
                                            binEx.getOp().getType(),
                                            l.getCanonicalName(),
                                            r.getCanonicalName()));
                        }
                        return BaseEnvironment.TYPE_INT;
                    case MINUS:
                    case STAR:
                    case FSLASH:
                        if (l != BaseEnvironment.TYPE_INT || r != BaseEnvironment.TYPE_INT) {
                            throw new TypeException(curClass.getFileName(), ex,
                                    String.format("Operator '%s' cannot be applied to '%s', '%s'",
                                            binEx.getOp().getType(),
                                            l.getCanonicalName(),
                                            r.getCanonicalName()));
                        }
                        return BaseEnvironment.TYPE_INT;
                    default:
                        throw new RuntimeException(
                                String.format("Error. Name resolver does not support the '%s' operator",
                                        binEx.getOp().getType()));
                }
            }
            case Expression.ICREATION_EXPRESSION: {
                ICreationExpression instanceCreation = (ICreationExpression) ex;
                Class type = resolveExpression(instanceCreation.getType(), env);
                List<Class> argTypes = new ArrayList<>();
                for (Expression expr : instanceCreation.getArgList()) {
                    argTypes.add(resolveExpression(expr, env));
                }
                String constructorSig = Constructor.getConstructorSignature(type.getCanonicalName() + "." + type.getName(), argTypes);
                Constructor m = env.lookupConstructor(constructorSig);
                return type;
            }
            case Expression.LITERAL_EXPRESSION: {
                LiteralExpression literalExpression = (LiteralExpression) ex;
                Literal lit = new Literal(literalExpression.getLiteral(), env);
                literalExpression.setProper(lit);
                return lit.getType();
            }
            case Expression.METHOD_EXPRESSION: {
                MethodExpression meth = (MethodExpression) ex;
                VariableExpression varExpr = meth.getMethodIdExpr();
                String methodName = null;
                if (varExpr instanceof FieldVariable) {
                    FieldVariable fVar = (FieldVariable) varExpr;
                    Class c = resolveExpression(fVar.getPrefixExpr(), env);
                    methodName = c.getCanonicalName() + "." + fVar.getFieldName();
                } else if (varExpr instanceof NameVariable) {
                    NameVariable nVar = (NameVariable) varExpr;
                    methodName = nVar.getName();
                }
                //Clazz type = resolveExpression(meth.getMethodIdExpr(), env);
                List<Class> argTypes = new ArrayList<>();
                for (Expression expr : meth.getArgList()) {
                   argTypes.add(resolveExpression(expr, env));
                }
                String methSig = Method.getMethodSignature(methodName, argTypes);
                Method m = env.lookupMethod(methSig);
                return m.getReturnType();
            }
            case Expression.REFERENCE_TYPE: {
                ReferenceType refType = (ReferenceType) ex;
                refType.setProper(env.lookupClazz(refType));
                return refType.getProper();
            }
            case Expression.THIS_EXPRESSION: {
                return curClass;
            }
            case Expression.UNARY_EXPRESSION: {

                UnaryExpression unary = (UnaryExpression) ex;
                Class k = resolveExpression(unary.getExpression(), env);
                if (unary instanceof CastExpression) {
                    Class castType = env.lookupClazz(((CastExpression) unary).getCast());
                    if (!Class.isValidCast(castType, k)) {
                        throw new InconvertibleTypeException(curClass.getFileName(), unary,
                                castType, k);
                    }
                    return castType;
                }
                switch (unary.getOp().getType()) {
                    case NOT:
                        return BaseEnvironment.TYPE_BOOLEAN;
                    default:
                        throw new RuntimeException(
                                String.format("Error. Name resolver does not support the unary '%s' operator",
                                        unary.getOp().getType()));
                }
            }
            case Expression.VARIABLE_EXPRESSION: {
                VariableExpression varExpr = (VariableExpression) ex;
                String varName = null;
                if (varExpr instanceof FieldVariable) {
                    FieldVariable fVar = (FieldVariable) varExpr;
                    Class c = resolveExpression(fVar.getPrefixExpr(), env);
                    varName = c.getCanonicalName() + "." + fVar.getFieldName();
                } else if (varExpr instanceof NameVariable) {
                    NameVariable nVar = (NameVariable) varExpr;
                    varName = nVar.getName();
                }
                return env.lookupField(varName).getType();
            }
            default:
                throw new RuntimeException(
                        String.format("Error. Name resolver does not support the expression type '%s'",
                                ex.getClass().getSimpleName()));
        }
    }

    @SuppressWarnings("unchecked")
    public void buildBaseEnvironment(FileNode fn, Class c) {
        // 4.    Create an environment.
        // 5.    Add the imports to the environment.
        // 6.    For each Method (and constructor):
        // 7.      Link types (including return values).
        // 8.      Generate Method signatures.
        // 9.      Add to the BaseEnvironment.

        // priorities:
        // single type import
        // everything in the class's package
        // import on demand
        CompositeEnvironment env = new CompositeEnvironment();
        env.addEnvironment(baseEnvironment);
        MapEnvironment singleImportEnv = new MapEnvironment();
        MapEnvironment multiImportEnv = new MapEnvironment();
        MapEnvironment packageEnv = new MapEnvironment();

        for (String s : fn.getImports()) {
            String[] parts = s.split("\\.");
            if (s.charAt(s.length() - 1) == '*') {
                try {
                    List<Class> results = baseEnvironment.getAllClassesInPackage(s.substring(0, s.length() - 2));
                    for (Class clazz : results) {
                        Object o = multiImportEnv.put(clazz.getName(), clazz);
                        if (o != null && o != clazz && !(o instanceof ErrorClass)) {
                            ErrorClass errorClass = new ErrorClass();
                            errorClass.nameClash[0] = (Class) o;
                            errorClass.nameClash[1] = clazz;
                            multiImportEnv.put(clazz.getName(), errorClass);
                        }
                    }
                } catch (EnvironmentException e) {
                    throw new NameResolutionException(c.getFileName(), new AstNode(),
                            String.format("Package import refers to a class '%s'", s));
                }
            } else {
                try {
                    Class clazz = baseEnvironment.lookupClazz(parts, false);
                    Object o = singleImportEnv.put(clazz.getName(), clazz);
                    if (o != null && o != clazz) {
                        Class oldClass = (Class) o;
                        throw new NameResolutionException(c.getFileName(), new AstNode(),
                                String.format("'%s' is already defined in a single-type import", oldClass.getCanonicalName()));
                    }
                } catch (EnvironmentException e) {
                    throw new NameResolutionException(c.getFileName(), new AstNode(),
                            String.format("Could not import on class '%s'", s));
                }
            }
        }
        
        List<Class> classes = baseEnvironment.getAllClassesInPackage(c.getPackage());
        for (Class clazz : classes) {
            packageEnv.put(clazz.getName(), clazz);
        }

        env.addEnvironment(multiImportEnv);
        env.addEnvironment(packageEnv);
        env.addEnvironment(singleImportEnv);

        fn.setEnv(env);

        List<MethodDecl> methods = c.getClassDecl().getMethodDeclarations();
        for (MethodDecl m : methods) {
            Method method = new Method(c, m, env);
            m.setMethod(method);
            c.putMethod(method);
        }

        c.resolveSelf(env);

        if (!c.isInterface()) {
            ClassDecl clazz = (ClassDecl) c.getClassDecl();

            for (ConstructorDecl cd : clazz.getConstructorDeclaration()) {
                try {
                    c.putConstructor(new Constructor(c, cd, env));
                } catch (EnvironmentException e) {
                    throwNameResolutionException(e, curClass.getFileName(), cd);
                }
            }

            for (VarDecl vd : clazz.getFieldDeclarations()) {
                try {
                    c.putField(new Field(c, vd, env));
                } catch (EnvironmentException e) {
                    throwNameResolutionException(e, curClass.getFileName(), vd);
                }
            }
        }

        env.setClassMemberEnvironment(new ClassEnvironment(c));
    }

    private void buildHierarchyEnvironment(List<Class> classes) {
        Stack<Class> toResolve = new Stack<>();
        for (Class c : classes) {
            toResolve.add(c);
        }

        if (classes.size() != 0) {
            Environment env = classes.get(0).getEnvironment();
            Class objClass = env.lookupClazz("java.lang.Object", false);
            objClass.setIsComplete(true);
        }

        while (!toResolve.isEmpty()) {
            Class c = toResolve.peek();
            if (c.isComplete()) {
                toResolve.pop();
                continue;
            }

            Class superClass = c.getSuperClass();
            Class[] interfaces = c.getInterfaces();
            boolean dependenciesComplete = true;

            if (superClass != null && !superClass.isComplete()) {
                toResolve.add(superClass);
                dependenciesComplete = false;
            }

            for (Class interfaze : interfaces) {
                if (!interfaze.isComplete()) {
                    if (interfaze.getInterfaces().length == 0) {
                        interfaze.setIsComplete(true);
                    } else {
                        toResolve.add(interfaze);
                        dependenciesComplete = false;
                    }
                }
            }

            if (!dependenciesComplete) continue;

            if (superClass != null) {
                mergeMethodWithClass(c, superClass);
            }

            for (Class i : interfaces) {
                mergeMethodWithClass(c, i);
            }

            c.setIsComplete(true);
            toResolve.pop();
        }
    }

    private void mergeMethodWithClass(Class c, Class toMerge) {
        for (Map.Entry<String, Object> entry : toMerge.entrySet()) {
            Object val = entry.getValue();
            if (c.containsKey(entry.getKey())) {
                // for variables... we just allow overriding regardless...
                // however for methods we need to do some checks...
                if (val instanceof Method) {
                    Method oldMeth = (Method) val;
                    Method newMeth = (Method) c.get(entry.getKey());

                    if (oldMeth.getReturnType() != newMeth.getReturnType()) {
                        throw new NameResolutionException(c.getFileName(), newMeth.getMethodDecl(),
                                String.format("'%s' in '%s' clashes with '%s' in '%s'; attempting to use incompatible return type",
                                        newMeth.getHumanReadableSignature(), newMeth.getDeclaringClass().getCanonicalName(),
                                        oldMeth.getHumanReadableSignature(), oldMeth.getDeclaringClass().getCanonicalName()));
                    } else if (Modifier.isPublic(oldMeth.getModifiers()) && Modifier.isProtected(newMeth.getModifiers())) {
                        throw new NameResolutionException(c.getFileName(), newMeth.getMethodDecl(),
                                String.format("'%s' in '%s' clashes with '%s' in '%s'; attempting to assign weaker access privileges",
                                        newMeth.getHumanReadableSignature(), newMeth.getDeclaringClass().getCanonicalName(),
                                        oldMeth.getHumanReadableSignature(), oldMeth.getDeclaringClass().getCanonicalName()));
                    }
                }
            } else {
                if (val instanceof Method) {
                    Method m = (Method) val;
                    if (Modifier.isAbstract(m.getModifiers())) {
                        if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                            throw new NameResolutionException(c.getFileName(), c.getClassDecl(),
                                    String.format("Class '%s' must either be declared abstract or implement method '%s' in '%s'",
                                            c.getName(), m.getHumanReadableSignature(), toMerge.getName()));
                        } else {
                            c.put(entry.getKey(), entry.getValue());
                        }
                    } else {
                        c.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    c.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private static class Kind {
        private final boolean isType;
        private final boolean isField;
        public Class c;
        public Field f;
        public Method m;

        public Kind(Class c) {
            isType = true;
            isField = false;
            this.c = c;
        }

        public Kind(Field f) {
            isType = false;
            isField = true;
            this.f = f;
        }

        public Kind(Method m) {
            isType = false;
            isField = false;
            this.m = m;
        }

        public boolean isType() {
            return isType;
        }

        public boolean isField() {
            return isField;
        }

        public Class getType() {
            if (isType) {
                return c;
            } else if (isField) {
                return f.getType();
            } else {
                return m.getReturnType();
            }
        }
    }

    private void throwNameResolutionException(EnvironmentException e, String fileName, AstNode pos) {
        switch (e.getType()) {
            case EnvironmentException.ERROR_NOT_FOUND:
                throw new NameResolutionException(fileName, pos,
                        String.format("Cannot resolve symbol '%s'", e.getExtra().toString()));
            case EnvironmentException.ERROR_CLASS_NAME_CLASH:
                ErrorClass ec = (ErrorClass) e.getExtra();
                throw new NameResolutionException(fileName, pos,
                        String.format("Reference to '%s' is ambiguous, both '%s' and '%s' match",
                                ec.nameClash[0].getName(), ec.nameClash[0].getCanonicalName(),
                                ec.nameClash[1].getCanonicalName()));
            case EnvironmentException.ERROR_SAME_VARIABLE_IN_SCOPE:
                throw new NameResolutionException(fileName, pos,
                        String.format("Variable '%s' is already defined in the scope",
                                e.getExtra().toString()));
        }
    }
}
