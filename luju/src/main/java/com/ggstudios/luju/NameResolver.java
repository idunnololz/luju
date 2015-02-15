package com.ggstudios.luju;

import com.ggstudios.env.BaseEnvironment;
import com.ggstudios.env.ClassEnvironment;
import com.ggstudios.env.Clazz;
import com.ggstudios.env.CompositeEnvironment;
import com.ggstudios.env.Environment;
import com.ggstudios.env.ErrorClass;
import com.ggstudios.env.Field;
import com.ggstudios.env.Interface;
import com.ggstudios.env.LocalLinkEnvironment;
import com.ggstudios.env.Literal;
import com.ggstudios.env.MapEnvironment;
import com.ggstudios.env.Method;
import com.ggstudios.env.Variable;
import com.ggstudios.error.EnvironmentException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.types.ArrayAccessExpression;
import com.ggstudios.types.ArrayCreationExpression;
import com.ggstudios.types.AssignExpression;
import com.ggstudios.types.AstNode;
import com.ggstudios.types.BinaryExpression;
import com.ggstudios.types.Block;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.Expression;
import com.ggstudios.types.ExpressionStatement;
import com.ggstudios.types.ICreationExpression;
import com.ggstudios.types.LiteralExpression;
import com.ggstudios.types.MethodDecl;
import com.ggstudios.types.MethodExpression;
import com.ggstudios.types.ReferenceType;
import com.ggstudios.types.Statement;
import com.ggstudios.types.UnaryExpression;
import com.ggstudios.types.VarDecl;
import com.ggstudios.types.VarInitDecl;
import com.ggstudios.types.VariableExpression;
import com.ggstudios.utils.ClassUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

public class NameResolver {
    private BaseEnvironment baseEnvironment;

    private Clazz curClass;

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
        // 10. We are now set to type link everything.
        // 11. Check for cyclic inheritance.
        // 12. For each class:
        // 13.   For each VarInitDecl:
        // 14.     Link all variables in expressions. (Ensure var is declared before it)
        // 15.   For each method:
        // 16.     Link all types of local variables.
        // 17.     Link all local variables within statements.

        int len = ast.size();
        for (int i = 0; i < len; i++) {
            FileNode fn = ast.get(i);
            Clazz clazz = baseEnvironment.lookupClazz((fn.getPackageName() + "." + fn.getTypeDecl().getTypeName()).split("\\."), false);
            buildBaseEnvironment(fn, clazz);
        }

        buildHierarchyEnvironment(baseEnvironment.getAllClasses());

        // check for cyclic class extends...
        List<Clazz> classes = baseEnvironment.getAllClasses();
        List<Interface> interfaces = new ArrayList<>();
        List<Clazz> realClasses = new ArrayList<>();
        Map<Clazz, Integer> interfaceToIndex = new HashMap<>();

        Clazz p1, p2;
        for (Clazz c : classes) {
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

        for (Clazz c : realClasses) {
            // 13.   For each VarInitDecl:
            // 14.     Link all variables in expressions. (Ensure var is declared before it)
            // 15.   For each method:
            // 16.     Link all types of local variables.
            // 17.     Link all local variables within statements.
            Environment env = c.getEnvironment();

            curClass = c;

            ClassDecl cDecl = (ClassDecl) c.getClassDecl();
            for (VarDecl vd : cDecl.getFieldDeclarations()) {
                try {
                    if (vd instanceof VarInitDecl) {
                        VarInitDecl vid = (VarInitDecl) vd;
                        Expression expr = vid.getExpr();
                        resolveExpression(expr, env);
                        // TODO check that the result is a variable and that the type matches...
                    }
                } catch (EnvironmentException e) {
                    throwNameResolutionException(e, c.getFileName(), vd);
                }
            }

            for (MethodDecl meth : cDecl.getMethodDeclarations()) {
                if (meth.isAbstract()) continue;
                Environment curEnv = env;
                for (VarDecl vd : meth.getArguments()) {
                    try {
                        Variable v = new Variable(meth.getMethod(), vd, curEnv);
                        curEnv = new LocalLinkEnvironment(v.getName(), v, curEnv);
                    } catch (EnvironmentException e) {
                        throwNameResolutionException(e, c.getFileName(), vd);
                    }
                }

                Block b = meth.getBlock();
                for (Statement s : b.getStatements()) {
                    curEnv = resolveStatement(s, curEnv);
                }
            }
        }
    }

    private Environment resolveStatement(Statement s, Environment env) {
        switch (s.getStatementType()) {
            case Statement.TYPE_BLOCK: {
                Block b = (Block) s;
                for (Statement st : b.getStatements()) {
                    env = resolveStatement(st, env);
                }
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
            case Statement.TYPE_VARDECL:
            case Statement.TYPE_WHILE:
                break;
        }
        return env;
    }

    public Kind resolveExpression(Expression ex, Environment env) {
        switch (ex.getExpressionType()) {
            case Expression.ARRAY_ACCESS_EXPRESSION: {
                ArrayAccessExpression arrayAccess = (ArrayAccessExpression) ex;
                resolveExpression(arrayAccess.getArrayExpr(), env);
                // TODO check result...
                break;
            }
            case Expression.ARRAY_CREATION_EXPRESSION: {
                ArrayCreationExpression arrayCreation = (ArrayCreationExpression) ex;
                resolveExpression(arrayCreation.getTypeExpr(), env);
                resolveExpression(arrayCreation.getDimExpr(), env);
                // TODO check result...
                break;
            }
            case Expression.ASSIGN_EXPRESSION: {
                AssignExpression assign = (AssignExpression) ex;
                resolveExpression(assign.getLhs(), env);
                resolveExpression(assign.getRhs(), env);
                // TODO check result...
                break;
            }
            case Expression.BINARY_EXPRESSION: {
                BinaryExpression binEx = (BinaryExpression) ex;
                resolveExpression(binEx.getLeftExpr(),  env);
                resolveExpression(binEx.getRightExpr(),  env);
                // TODO check result...
                break;
            }
            case Expression.ICREATION_EXPRESSION: {
                ICreationExpression instanceCreation = (ICreationExpression) ex;
                resolveExpression(instanceCreation.getType(), env);
                for (Expression expr : instanceCreation.getArgList()) {
                    resolveExpression(expr, env);
                }
                // TODO check result...
                break;
            }
            case Expression.LITERAL_EXPRESSION: {
                LiteralExpression literalExpression = (LiteralExpression) ex;
                Literal lit = new Literal(literalExpression.getLiteral(), env);
                return new Kind(lit);
            }
            case Expression.METHOD_EXPRESSION: {
                MethodExpression meth = (MethodExpression) ex;
                resolveExpression(meth.getMethodIdExpr(), env);
                for (Expression expr : meth.getArgList())
                    resolveExpression(expr, env);
                // TODO check result...
                break;
            }
            case Expression.REFERENCE_TYPE: {
                ReferenceType refType = (ReferenceType) ex;
                refType.setType(env.lookupClazz(refType));
                return new Kind(refType.getType());
            }
            case Expression.THIS_EXPRESSION: {
                return new Kind(env.lookupField("this"));
            }
            case Expression.UNARY_EXPRESSION: {
                UnaryExpression unary = (UnaryExpression) ex;
                resolveExpression(unary.getExpression(), env);
                // TODO check result...
                break;
            }
            case Expression.VARIABLE_EXPRESSION: {
                VariableExpression var = (VariableExpression) ex;
                return new Kind(env.lookupField(var.getName()));
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public void buildBaseEnvironment(FileNode fn, Clazz c) {
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
                    List<Clazz> results = baseEnvironment.getAllClassesInPackage(s.substring(0, s.length() - 2));
                    for (Clazz clazz : results) {
                        Object o = multiImportEnv.put(clazz.getName(), clazz);
                        if (o != null && o != clazz && !(o instanceof ErrorClass)) {
                            ErrorClass errorClass = new ErrorClass();
                            errorClass.nameClash[0] = (Clazz) o;
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
                    Clazz clazz = baseEnvironment.lookupClazz(parts, false);
                    Object o = singleImportEnv.put(clazz.getName(), clazz);
                    if (o != null && o != clazz) {
                        Clazz oldClazz = (Clazz) o;
                        throw new NameResolutionException(c.getFileName(), new AstNode(),
                                String.format("'%s' is already defined in a single-type import", oldClazz.getCanonicalName()));
                    }
                } catch (EnvironmentException e) {
                    throw new NameResolutionException(c.getFileName(), new AstNode(),
                            String.format("Could not import on class '%s'", s));
                }
            }
        }
        
        List<Clazz> classes = baseEnvironment.getAllClassesInPackage(c.getPackage());
        for (Clazz clazz : classes) {
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

            for (VarDecl vd : clazz.getFieldDeclarations()) {
                try {
                    c.putField(new Field(c, vd, env));
                } catch (EnvironmentException e) {
                    switch (e.getType()) {
                        case EnvironmentException.ERROR_NOT_FOUND:
                            throw new NameResolutionException(c.getFileName(), vd,
                                    String.format("Cannot resolve symbol '%s'", e.getExtra().toString()));
                    }
                    throw e;
                }
            }
        }

        env.addEnvironment(new ClassEnvironment(c));
    }

    private void buildHierarchyEnvironment(List<Clazz> classes) {
        Queue<Clazz> toResolve = new LinkedList<>();
        for (Clazz c : classes) {
            toResolve.add(c);
        }

        if (classes.size() != 0) {
            Environment env = classes.get(0).getEnvironment();
            Clazz objClazz = env.lookupClazz("java.lang.Object", false);
            objClazz.setIsComplete(true);
        }

        while (!toResolve.isEmpty()) {
            Clazz c = toResolve.peek();
            if (c.isComplete()) {
                toResolve.poll();
                continue;
            }


            Clazz superClass = c.getSuperClass();
            Clazz[] interfaces = c.getInterfaces();
            boolean dependenciesComplete = true;

            if (superClass != null && !superClass.isComplete()) {
                toResolve.add(superClass);
                dependenciesComplete = false;
            }

            for (Clazz interfaze : interfaces) {
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


            if (c instanceof Interface) {


            } else {
                for (Map.Entry<String, Object> entry : superClass.entrySet()) {
                    if (c.containsKey(entry.getKey())) {
                        // for variables... we just allow overriding regardless...
                        // however for methods we need to do some checks...
                        Object val = entry.getValue();
                        if (val instanceof Method) {
                            
                        }
                    }
                }
            }

            c.setIsComplete(true);
            toResolve.poll();
        }
    }

    private static class Kind {
        private final boolean isType;
        private final boolean isField;
        public Clazz c;
        public Field f;
        public Method m;

        public Kind(Clazz c) {
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
