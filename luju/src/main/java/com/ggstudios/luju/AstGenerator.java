package com.ggstudios.luju;

import com.ggstudios.error.AstException;
import com.ggstudios.error.WeedException;
import com.ggstudios.luju.Parser.Node;
import com.ggstudios.types.ArrayAccessExpression;
import com.ggstudios.types.ArrayCreationExpression;
import com.ggstudios.types.AssignExpression;
import com.ggstudios.types.BinaryExpression;
import com.ggstudios.types.Block;
import com.ggstudios.types.CastExpression;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.ConstructorDecl;
import com.ggstudios.types.Expression;
import com.ggstudios.types.ExpressionStatement;
import com.ggstudios.types.FieldVariable;
import com.ggstudios.types.ForStatement;
import com.ggstudios.types.ICreationExpression;
import com.ggstudios.types.LiteralExpression;
import com.ggstudios.types.MethodExpression;
import com.ggstudios.types.NameVariable;
import com.ggstudios.types.ThisExpression;
import com.ggstudios.types.UnaryExpression;
import com.ggstudios.types.VarDecl;
import com.ggstudios.types.VarInitDecl;
import com.ggstudios.types.IfStatement;
import com.ggstudios.types.InterfaceDecl;
import com.ggstudios.types.MethodDecl;
import com.ggstudios.types.ReturnStatement;
import com.ggstudios.types.Statement;
import com.ggstudios.types.TypeDecl;
import com.ggstudios.types.ReferenceType;
import com.ggstudios.types.WhileStatement;
import com.ggstudios.utils.ListUtils;
import com.ggstudios.utils.ParserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class AstGenerator {
    private FileNode fn;

    public AstGenerator() {}

    public void generateAst(FileNode fileNode, Node n) {
        fn = fileNode;
        completeAst(n);
    }

    private void completeAst(Node n) {
        // compilationUnit -> optPackageDeclaration optImportDeclarations typeDeclaration optSemicolons
        doPackageDecl(n.children.get(0));
        doImportsDecl(n.children.get(1));
        doTypeDecl(n.children.get(2));
    }

    private void doTypeDecl(Node node) {
        // typeDeclaration -> classDeclaration
        // typeDeclaration -> interfaceDeclaration
        TypeDecl decl;
        if (node.prod.rhs[0] == ParseTable.NONT_CLASSDECLARATION) {
            decl = doClassDecl(node.children.get(0));
        } else {
            decl = doInterfaceDecl(node.children.get(0));
        }
        fn.setTypeDecl(decl);
    }

    private InterfaceDecl doInterfaceDecl(Node node) {
        // interfaceDeclaration -> optModifiers INTERFACE ID optExtendsInterfaces interfaceBody
        InterfaceDecl decl = new InterfaceDecl(fn.getPackageName());
        Token landmark = getFirstTokensInTree(node);
        decl.setPos(landmark.getRow(), landmark.getCol());
        decl.setTypeName(node.children.get(2).t.getRaw());

        List<Token> toks = ParserUtils.getTokensInTree(node.children.get(0));
        for (Token t : toks) {
            decl.addModifier(t.getType());
        }

        // optExtendsInterfaces -> extendsInterfaces
        // optExtendsInterfaces
        Node optExtendsInterfaces = node.children.get(3);
        if (optExtendsInterfaces.prod.rhs.length != 0) {
            // extendsInterfaces -> EXTENDS interfaceType
            // extendsInterfaces -> extendsInterfaces COMMA interfaceType
            Node extendsInterfaces = optExtendsInterfaces.children.get(0);
            List<Node> interfaceTypes = listFromTree(extendsInterfaces, 0, 2, 1, 2);
            for (Node n : interfaceTypes) {
                decl.addExtend(nameFromNode(n));
            }
        }

        // interfaceBody -> LBRACE optInterfaceMemberDeclarations RBRACE
        Node interfaceBody = node.children.get(4);
        // optInterfaceMemberDeclarations -> interfaceMemberDeclarations
        // optInterfaceMemberDeclarations
        Node optInterfaceMemberDeclarations = interfaceBody.children.get(1);
        if (!optInterfaceMemberDeclarations.children.isEmpty()) {
            // interfaceMemberDeclarations -> abstractMethodDeclaration
            // interfaceMemberDeclarations -> interfaceMemberDeclarations abstractMethodDeclaration
            Node interfaceMemberDeclarations = optInterfaceMemberDeclarations.children.get(0);

            // abstractMethodDeclaration -> methodHeader SEMI
            List<Node> abstractMethodDeclarations = listFromTree(interfaceMemberDeclarations);
            for (Node n : ListUtils.reverse(abstractMethodDeclarations)) {
                MethodDecl methodDecl = new MethodDecl();
                methodDecl.addModifier(Token.Type.ABSTRACT);
                Token methodLandmark = getFirstTokensInTree(n);
                methodDecl.setPos(methodLandmark.getRow(), methodLandmark.getCol());
                doMethodHeader(methodDecl, n.children.get(0));

                decl.addMethodDeclaration(methodDecl);
            }
        }

        return decl;
    }

    private ClassDecl doClassDecl(Node node) {
        // classDeclaration -> optModifiers CLASS ID optSuper optInterfaces classBody
        ClassDecl decl = new ClassDecl(fn.getPackageName());
        Token landmark = getFirstTokensInTree(node);
        decl.setPos(landmark.getRow(), landmark.getCol());
        decl.setTypeName(node.children.get(2).t.getRaw());

        List<Token> toks = ParserUtils.getTokensInTree(node.children.get(0));
        for (Token t : toks) {
            decl.addModifier(t.getType());
        }

        // optSuper -> EXTENDS classType
        // optSuper
        Node optSuper = node.children.get(3);
        if (optSuper.prod.rhs.length != 0) {
            decl.setSuperTypeName(nameFromNode(optSuper.children.get(1)));
        }

        // optInterfaces -> IMPLEMENTS interfaceTypeList
        // optInterfaces

        Node optInterfaces = node.children.get(4);
        if (optInterfaces.prod.rhs.length != 0) {
            // interfaceTypeList -> interfaceType
            // interfaceTypeList -> interfaceTypeList COMMA interfaceType
            Node interfaceTypeList = optInterfaces.children.get(1);
            while (interfaceTypeList.children.size() == 3) {
                decl.addImplement(nameFromNode(interfaceTypeList.children.get(2)));
                interfaceTypeList = interfaceTypeList.children.get(0);
            }
            decl.addImplement(nameFromNode(interfaceTypeList.children.get(0)));
        }

        // classBody -> LBRACE optClassBodyDeclarations RBRACE
        // optClassBodyDeclarations -> classBodyDeclarations
        // optClassBodyDeclarations
        Node classBody = node.children.get(5);
        Node optClassBodyDecls = classBody.children.get(1);

        if (optClassBodyDecls.children.size() == 1) {
            // classBodyDeclarations -> classBodyDeclaration
            // classBodyDeclarations -> classBodyDeclarations classBodyDeclaration
            Node classBodyDecls = optClassBodyDecls.children.get(0);

            for (Node classBodyDecl : ListUtils.reverse(listFromTree(classBodyDecls))) {
                doClassBodyDecl(decl, classBodyDecl);
            }
        }

        return decl;
    }

    private void doClassBodyDecl(ClassDecl decl, Node node) {
        // classBodyDeclaration -> classMemberDeclaration
        // classBodyDeclaration -> constructorDeclaration
        // classBodyDeclaration -> SEMI

        int construct = node.prod.rhs[0];

        if (construct == ParseTable.NONT_CLASSMEMBERDECLARATION) {
            // classMemberDeclaration -> fieldDeclaration
            // classMemberDeclaration -> methodDeclaration
            Node classMemberDecl = node.children.get(0);

            construct = classMemberDecl.prod.rhs[0];

            if (construct == ParseTable.NONT_FIELDDECLARATION) {
                doFieldDecl(decl, classMemberDecl.children.get(0));
            } else {
                doMethodDecl(decl, classMemberDecl.children.get(0));
            }
        } else if (construct == ParseTable.NONT_CONSTRUCTORDECLARATION) {
            doConstructorDecl(decl, node.children.get(0));
        }
    }

    private void doConstructorDecl(ClassDecl decl, Node node) {
        // constructorDeclaration -> optModifiers constructorDeclarator block
        ConstructorDecl constructorDecl = new ConstructorDecl(decl);

        Token landmark = getFirstTokensInTree(node);
        constructorDecl.setPos(landmark.getRow(), landmark.getCol());

        List<Token> toks = ParserUtils.getTokensInTree(node.children.get(0));
        for (Token t : toks) {
            constructorDecl.addModifier(t.getType());
        }

        // constructorDeclarator -> simpleName LPAREN optFormalParameterList RPAREN
        Node constructorDeclarator = node.children.get(1);

        constructorDecl.setName(getFirstTokensInTree(constructorDeclarator.children.get(0)));
        doOptFormalParameterList(constructorDecl, constructorDeclarator.children.get(2));
        constructorDecl.setBlock(getBlock(node.children.get(2)));

        decl.addConstructorDeclaration(constructorDecl);
    }

    private void doMethodDecl(ClassDecl decl, Node node) {
        // methodDeclaration -> methodHeader methodBody
        MethodDecl methodDecl;

        boolean isBodiless = node.children.get(1).prod.rhs[0] == ParseTable.TERM_SEMI;
        methodDecl = new MethodDecl();

        Token landmark = getFirstTokensInTree(node);
        methodDecl.setPos(landmark.getRow(), landmark.getCol());

        Node methodHeader = node.children.get(0);
        doMethodHeader(methodDecl, methodHeader);

        // methodBody -> block
        // methodBody -> SEMI
        Node methodBody = node.children.get(1);
        if (!isBodiless) {
            methodDecl.setBlock(getBlock(methodBody.children.get(0)));
        }

        decl.addMethodDeclaration(methodDecl);
    }

    private void doMethodHeader(MethodDecl methodDecl, Node methodHeader) {
        // methodHeader -> optModifiers type methodDeclarator
        // methodHeader -> optModifiers VOID methodDeclarator
        List<Token> toks = ParserUtils.getTokensInTree(methodHeader.children.get(0));
        for (Token t : toks) {
            methodDecl.addModifier(t.getType());
        }

        Node returnType = methodHeader.children.get(1);
        if (returnType.t == null) {
            methodDecl.setReturnType(new ReferenceType(nameFromNode(returnType)));
        } else {
            methodDecl.setReturnType(new ReferenceType("void"));
        }

        // methodDeclarator -> ID LPAREN optFormalParameterList RPAREN
        Node methodDeclarator = methodHeader.children.get(2);
        methodDecl.setName(methodDeclarator.children.get(0).t);

        // optFormalParameterList -> formalParameterList
        // optFormalParameterList
        doOptFormalParameterList(methodDecl, methodDeclarator.children.get(2));
    }

    private void doOptFormalParameterList(MethodDecl methodDecl, Node optFormalParameterList) {
        // optFormalParameterList -> formalParameterList
        // optFormalParameterList
        if (optFormalParameterList.children.size() != 0) {
            // formalParameterList -> formalParameter
            // formalParameterList -> formalParameterList COMMA formalParameter
            Node formalParameterList = optFormalParameterList.children.get(0);
            List<Node> formalParameters = listFromTree(formalParameterList, 0, 2);

            // formalParameter -> type variableDeclaratorId
            for (Node n : ListUtils.reverse(formalParameters)) {
                VarDecl arg = new VarDecl();
                Token argLandmark = getFirstTokensInTree(n.children.get(1));
                arg.setPos(argLandmark.getRow(), argLandmark.getCol());
                arg.setType(new ReferenceType(nameFromNode(n.children.get(0))));
                arg.setId(getFirstTokensInTree(n.children.get(1)));
                methodDecl.addArgument(arg);
            }
        }
    }

    private void doFieldDecl(ClassDecl decl, Node node) {
        // fieldDeclaration -> optModifiers type variableDeclarator SEMI
        // fieldDeclaration -> optModifiers type variableDeclarator ASSIGN expression SEMI

        VarDecl varDecl = null;

        if (node.prod.rhs.length == 4) {
            varDecl = new VarDecl();
        } else {
            varDecl = new VarInitDecl();
        }

        List<Token> toks = ParserUtils.getTokensInTree(node.children.get(0));

        Token landmark = getFirstTokensInTree(node.children.get(0));
        varDecl.setPos(landmark.getRow(), landmark.getCol());

        for (Token t : toks) {
            varDecl.addModifier(t.getType());
        }

        varDecl.setType(new ReferenceType(nameFromNode(node.children.get(1))));

        // variableDeclarator -> variableDeclaratorId
        // variableDeclaratorId -> ID
        Node variableDeclarator = node.children.get(2);
        Token t = getFirstTokensInTree(variableDeclarator);
        varDecl.setId(t);

        if (node.prod.rhs.length != 4) {
            VarInitDecl fiDecl = (VarInitDecl) varDecl;

            fiDecl.setExpr(getExpression(node.children.get(4)));
        }

        decl.addFieldDeclaration(varDecl);
    }

    private Block getBlock(Node node) {
        //block -> LBRACE optBlockStatements RBRACE
        Block block = new Block();

        // optBlockStatements -> blockStatements
        // optBlockStatements
        if (!node.children.get(1).children.isEmpty()) {
            // blockStatements -> blockStatement
            // blockStatements -> blockStatements blockStatement
            Node blockStatementsNode = node.children.get(1).children.get(0);

            // blockStatement -> localVariableDeclarationStatement
            // blockStatement -> statement
            List<Node> blockStatements = listFromTree(blockStatementsNode);
            for (Node blockStatement : ListUtils.reverse(blockStatements)) {
                int rhs = blockStatement.prod.rhs[0];
                if (rhs == ParseTable.NONT_LOCALVARIABLEDECLARATIONSTATEMENT) {
                    // localVariableDeclarationStatement -> localVariableDeclaration SEMI
                    Node localVariableDeclaration = blockStatement.children.get(0).children.get(0);
                    VarInitDecl fieldDecl = getLocalVariableDeclaration(localVariableDeclaration);
                    block.addStatement(fieldDecl);
                } else if (rhs == ParseTable.NONT_STATEMENT) {
                    // statement statementWithoutTrailingSubstatement
                    // statement ifThenStatement
                    // statement ifThenElseStatement
                    // statement whileStatement
                    // statement forStatement
                    Statement statement = getStatement(blockStatement.children.get(0));
                    if (statement != null) {
                        block.addStatement(statement);
                    }
                } else {
                    throw new IllegalStateException("BlockStatement rule not found.");
                }
            }
        }

        return block;
    }

    private VarInitDecl getLocalVariableDeclaration(Node localVariableDeclaration) {
        // localVariableDeclaration -> type variableDeclarator ASSIGN expression
        VarInitDecl varDecl = new VarInitDecl();
        Token landmark = getFirstTokensInTree(localVariableDeclaration);
        varDecl.setPos(landmark.getRow(), landmark.getCol());
        varDecl.setType(new ReferenceType(nameFromNode(localVariableDeclaration.children.get(0))));
        varDecl.setId(getFirstTokensInTree(localVariableDeclaration.children.get(1)));
        varDecl.setExpr(getExpression(localVariableDeclaration.children.get(3)));
        return varDecl;
    }

    /**
     * May return null
     * @param statement
     * @return
     */
    private Statement getStatement(Node statement) {
        // statement statementWithoutTrailingSubstatement
        // statement ifThenStatement
        // statement ifThenElseStatement
        // statement whileStatement
        // statement forStatement
        int statementType = statement.prod.rhs[0];
        Node s = statement.children.get(0);

        switch (statementType) {
            case ParseTable.NONT_STATEMENTWITHOUTTRAILINGSUBSTATEMENT: {
                Statement maybeS = getStatementWithoutTrailingSubstatement(s);
                return maybeS;
            }
            case ParseTable.NONT_IFTHENSTATEMENT: {
                // ifThenStatement -> IF LPAREN expression RPAREN statement
                Node ifThenStatement = statement.children.get(0);
                IfStatement ifs = new IfStatement();
                Token landmark = ifThenStatement.children.get(0).t;
                ifs.setPos(landmark.getRow(), landmark.getCol());

                Expression expr = getExpression(ifThenStatement.children.get(2));
                Statement body = getStatement(ifThenStatement.children.get(4));
                ifs.addIfBlock(expr, body);
                return ifs;
            }
            case ParseTable.NONT_IFTHENELSESTATEMENT: {
                Node ifThenElseStatement = statement.children.get(0);
                return getIfThenElseStatement(ifThenElseStatement);
            }
            case ParseTable.NONT_WHILESTATEMENT: {
                Node whileStatement = statement.children.get(0);
                return getWhileStatement(whileStatement);
            }
            case ParseTable.NONT_FORSTATEMENT: {
                Node forStatement = statement.children.get(0);
                return getForStatement(forStatement);
            }
        }
        throw new IllegalStateException("Statement rule not found.");
    }

    private Statement getStatementNoShortIf(Node statementNoShortIf) {
        // statementNoShortIf -> statementWithoutTrailingSubstatement
        // statementNoShortIf -> ifThenElseStatementNoShortIf
        // statementNoShortIf -> whileStatementNoShortIf
        // statementNoShortIf -> forStatementNoShortIf
        int rhs = statementNoShortIf.prod.rhs[0];

        switch (rhs) {
            case ParseTable.NONT_STATEMENTWITHOUTTRAILINGSUBSTATEMENT:
                return getStatementWithoutTrailingSubstatement(statementNoShortIf.children.get(0));
            case ParseTable.NONT_IFTHENELSESTATEMENTNOSHORTIF:
                return getIfThenElseStatement(statementNoShortIf.children.get(0));
            case ParseTable.NONT_WHILESTATEMENTNOSHORTIF:
                return getWhileStatement(statementNoShortIf.children.get(0));
            case ParseTable.NONT_FORSTATEMENTNOSHORTIF:
                return getForStatement(statementNoShortIf.children.get(0));
        }

        throw new IllegalStateException("Statement rule not found.");
    }

    private Statement getIfThenElseStatement(Node ifThenElseStatement) {
        // ifThenElseStatement -> IF LPAREN expression RPAREN statementNoShortIf ELSE statement
        // ifThenElseStatementNoShortIf -> IF LPAREN expression RPAREN statementNoShortIf ELSE statementNoShortIf
        IfStatement ifs = new IfStatement();
        Token landmark = ifThenElseStatement.children.get(0).t;
        ifs.setPos(landmark.getRow(), landmark.getCol());

        Expression expr = getExpression(ifThenElseStatement.children.get(2));

        Node statementNoShortIf = ifThenElseStatement.children.get(4);
        Statement body = getStatementNoShortIf(statementNoShortIf);

        Statement elseS;
        if (ifThenElseStatement.prod.lhs == ParseTable.NONT_IFTHENELSESTATEMENT) {
            elseS = getStatement(ifThenElseStatement.children.get(6));
        } else {
            elseS = getStatementNoShortIf(ifThenElseStatement.children.get(6));
        }

        if (elseS instanceof IfStatement) {
            IfStatement otherIf = (IfStatement) elseS;
            List<IfStatement.IfBlock> ifBlocks = otherIf.getIfBlocks();
            ifBlocks.add(0, new IfStatement.IfBlock(expr, body));
            ifs.setIfBlocks(ifBlocks);
        } else {
            ifs.addIfBlock(expr, body);
            ifs.setElseBlock(new IfStatement.ElseBlock(elseS));
        }

        return ifs;
    }

    private Statement getWhileStatement(Node whileStatement) {
        // whileStatement -> WHILE LPAREN expression RPAREN statement
        // whileStatementNoShortIf -> WHILE LPAREN expression RPAREN statementNoShortIf
        WhileStatement whileS = new WhileStatement();
        Token landmark = whileStatement.children.get(0).t;
        whileS.setPos(landmark.getRow(), landmark.getCol());

        whileS.setCondition(getExpression(whileStatement.children.get(2)));
        if (whileStatement.prod.lhs == ParseTable.NONT_WHILESTATEMENT) {
            whileS.setBody(getStatement(whileStatement.children.get(4)));
        } else {
            whileS.setBody(getStatementNoShortIf(whileStatement.children.get(4)));
        }

        return whileS;
    }

    private Statement getForStatement(Node forStatement) {
        // forStatement -> FOR LPAREN optForInit SEMI optExpression SEMI optForUpdate RPAREN statement
        // forStatementNoShortIf -> FOR LPAREN optForInit SEMI optExpression SEMI optForUpdate RPAREN statementNoShortIf
        ForStatement forS = new ForStatement();
        Token landmark = forStatement.children.get(0).t;
        forS.setPos(landmark.getRow(), landmark.getCol());

        // optForInit forInit
        // optForInit
        if (forStatement.children.get(2).prod.rhs.length != 0) {
            // forInit statementExpression
            // forInit localVariableDeclaration
            Statement forInitS;
            Node forInit = forStatement.children.get(2).children.get(0);
            if (forInit.prod.rhs[0] == ParseTable.NONT_STATEMENTEXPRESSION) {
                forInitS = getStatementExpression(forInit.children.get(0));
            } else {
                forInitS = getLocalVariableDeclaration(forInit.children.get(0));
            }
            forS.setForInit(forInitS);
        }

        Expression expr = getOptExpression(forStatement.children.get(4));
        if (expr != null) {
            forS.setCondition(expr);
        }

        // optForUpdate forUpdate
        // optForUpdate
        if (forStatement.children.get(6).prod.rhs.length != 0) {
            // forUpdate statementExpression
            Node forUpdate = forStatement.children.get(6).children.get(0);
            forS.setForUpdate(getStatementExpression(forUpdate.children.get(0)));
        }

        Node forBody = forStatement.children.get(8);
        if (forBody.prod.lhs == ParseTable.NONT_STATEMENT) {
            forS.setBody(getStatement(forBody));
        } else {
            forS.setBody(getStatementNoShortIf(forBody));
        }
        return forS;
    }

    private Statement getStatementWithoutTrailingSubstatement(Node node) {
        // statementWithoutTrailingSubstatement block
        // statementWithoutTrailingSubstatement emptyStatement
        // statementWithoutTrailingSubstatement expressionStatement
        // statementWithoutTrailingSubstatement returnStatement
        int statementType = node.prod.rhs[0];
        Node s = node.children.get(0);

        switch (statementType) {
            case ParseTable.NONT_BLOCK:
                return getBlock(s);
            case ParseTable.NONT_EMPTYSTATEMENT:
                // emptyStatement -> SEMI
                return null;
            case ParseTable.NONT_EXPRESSIONSTATEMENT:
                // expressionStatement -> statementExpression SEMI
                return getStatementExpression(s.children.get(0));
            case ParseTable.NONT_RETURNSTATEMENT:
                // returnStatement -> RETURN optExpression SEMI
                ReturnStatement rs = new ReturnStatement();
                Token landmark = s.children.get(0).t;
                rs.setPos(landmark.getRow(), landmark.getCol());
                rs.setExpression(getOptExpression(s.children.get(1)));
                return rs;
        }

        return null;
    }

    private Expression getOptExpression(Node node) {
        // optExpression -> expression
        // optExpression
        if (node.prod.rhs.length == 0) {
            return null;
        }

        return getExpression(node.children.get(0));
    }

    private Statement getStatementExpression(Node statementExpression) {
        // statementExpression -> assignment
        // statementExpression -> methodInvocation
        // statementExpression -> classInstanceCreationExpression
        Node n = statementExpression.children.get(0);
        int rhs = statementExpression.prod.rhs[0];
        switch (rhs) {
            case ParseTable.NONT_ASSIGNMENT:
                return new ExpressionStatement(getAssignment(n));
            case ParseTable.NONT_METHODINVOCATION:
                return new ExpressionStatement(getMethodInvocation(n));
            case ParseTable.NONT_CLASSINSTANCECREATIONEXPRESSION:
                return new ExpressionStatement(getClassInstanceCreationExpression(n));
            default:
                throw new IllegalStateException("StatementExpression rule not found.");
        }
    }

    private Expression getAssignment(Node assignment) {
        // assignment -> leftHandSide ASSIGN assignmentExpression
        AssignExpression assignE = new AssignExpression();
        Token landmark = getFirstTokensInTree(assignment);
        assignE.setPos(landmark.getRow(), landmark.getCol());

        // leftHandSide name
        // leftHandSide fieldAccess
        // leftHandSide arrayAccess
        Node leftHandSide = assignment.children.get(0);
        Node unk = leftHandSide.children.get(0);
        Expression v;
        switch (leftHandSide.prod.rhs[0]) {
            case ParseTable.NONT_NAME:
                Node name = unk;
                NameVariable nVar = new NameVariable(ParserUtils.getIdSeqInTree(name));
                Token varToken = getFirstTokensInTree(name);
                nVar.setPos(varToken.getRow(), varToken.getCol());
                v = nVar;
                break;
            case ParseTable.NONT_FIELDACCESS:
                v = getFieldAccess(unk);
                break;
            case ParseTable.NONT_ARRAYACCESS:
                v = getArrayAccess(unk);
                break;
            default:
                throw new IllegalStateException("LeftHandSide rule not found.");
        }
        assignE.setLhs(v);

        assignE.setRhs(getAssignmentExpression(assignment.children.get(2)));

        return assignE;
    }

    private Expression getAssignmentExpression(Node assignmentExpression) {
        // assignmentExpression -> conditionalOrExpression
        // assignmentExpression -> assignment
        if (assignmentExpression.prod.rhs[0] == ParseTable.NONT_ASSIGNMENT) {
            return getAssignment(assignmentExpression.children.get(0));
        }

        return getConditionalOrExpression(assignmentExpression.children.get(0));
    }

    private Expression getConditionalOrExpression(Node conditionalOrExpression) {
        // conditionalOrExpression -> conditionalAndExpression
        // conditionalOrExpression -> conditionalOrExpression ORS conditionalAndExpression
        BinaryExpression first = new BinaryExpression();
        BinaryExpression last = first;
        while (conditionalOrExpression.prod.rhs[0] == ParseTable.NONT_CONDITIONALOREXPRESSION) {
            BinaryExpression expr = new BinaryExpression();
            expr.setOp(conditionalOrExpression.children.get(1).t);
            expr.setRightExpr(getConditionalAndExpression(conditionalOrExpression.children.get(2)));
            last.setLeftExpr(expr);
            last = expr;

            conditionalOrExpression = conditionalOrExpression.children.get(0);
        }
        last.setLeftExpr(getConditionalAndExpression(conditionalOrExpression.children.get(0)));

        return first.getLeftExpr();
    }

    private Expression getConditionalAndExpression(Node conditionalAndExpression) {
        // conditionalAndExpression -> inclusiveOrExpression
        // conditionalAndExpression -> conditionalAndExpression ANDS inclusiveOrExpression
        BinaryExpression first = new BinaryExpression();
        BinaryExpression last = first;
        while (conditionalAndExpression.prod.rhs[0] == ParseTable.NONT_CONDITIONALANDEXPRESSION) {
            BinaryExpression expr = new BinaryExpression();
            expr.setOp(conditionalAndExpression.children.get(1).t);
            expr.setRightExpr(getInclusiveOrExpression(conditionalAndExpression.children.get(2)));
            last.setLeftExpr(expr);
            last = expr;

            conditionalAndExpression = conditionalAndExpression.children.get(0);
        }
        last.setLeftExpr(getInclusiveOrExpression(conditionalAndExpression.children.get(0)));

        return first.getLeftExpr();
    }

    private Expression getInclusiveOrExpression(Node inclusiveOrExpression) {
        // inclusiveOrExpression -> andExpression
        // inclusiveOrExpression -> inclusiveOrExpression ORL andExpression
        BinaryExpression first = new BinaryExpression();
        BinaryExpression last = first;
        while (inclusiveOrExpression.prod.rhs[0] == ParseTable.NONT_INCLUSIVEOREXPRESSION) {
            BinaryExpression expr = new BinaryExpression();
            expr.setOp(inclusiveOrExpression.children.get(1).t);
            expr.setRightExpr(getAndExpression(inclusiveOrExpression.children.get(2)));
            last.setLeftExpr(expr);
            last = expr;

            inclusiveOrExpression = inclusiveOrExpression.children.get(0);
        }
        last.setLeftExpr(getAndExpression(inclusiveOrExpression.children.get(0)));

        return first.getLeftExpr();
    }

    private Expression getAndExpression(Node andExpression) {
        // andExpression -> equalityExpression
        // andExpression -> andExpression ANDL equalityExpression
        BinaryExpression first = new BinaryExpression();
        BinaryExpression last = first;
        while (andExpression.prod.rhs[0] == ParseTable.NONT_ANDEXPRESSION) {
            BinaryExpression expr = new BinaryExpression();
            expr.setOp(andExpression.children.get(1).t);
            expr.setRightExpr(getEqualityExpression(andExpression.children.get(2)));
            last.setLeftExpr(expr);
            last = expr;

            andExpression = andExpression.children.get(0);
        }
        last.setLeftExpr(getEqualityExpression(andExpression.children.get(0)));

        return first.getLeftExpr();
    }

    private Expression getEqualityExpression(Node equalityExpression) {
        // equalityExpression -> relationalExpression
        // equalityExpression -> equalityExpression EQUALS relationalExpression
        // equalityExpression -> equalityExpression NOTEQUALS relationalExpression
        BinaryExpression first = new BinaryExpression();
        BinaryExpression last = first;
        while (equalityExpression.prod.rhs[0] == ParseTable.NONT_EQUALITYEXPRESSION) {
            BinaryExpression expr = new BinaryExpression();
            expr.setOp(equalityExpression.children.get(1).t);
            expr.setRightExpr(getRelationalExpression(equalityExpression.children.get(2)));
            last.setLeftExpr(expr);
            last = expr;

            equalityExpression = equalityExpression.children.get(0);
        }
        last.setLeftExpr(getRelationalExpression(equalityExpression.children.get(0)));

        return first.getLeftExpr();
    }

    private Expression getRelationalExpression(Node relationalExpression) {
        // relationalExpression -> additiveExpression
        // relationalExpression -> relationalExpression LTTHAN additiveExpression
        // relationalExpression -> relationalExpression GTTHAN additiveExpression
        // relationalExpression -> relationalExpression LTEQ additiveExpression
        // relationalExpression -> relationalExpression GTEQ additiveExpression
        // relationalExpression -> relationalExpression INSTANCEOF referenceType

        BinaryExpression first = new BinaryExpression();
        BinaryExpression last = first;
        while (relationalExpression.prod.rhs[0] == ParseTable.NONT_RELATIONALEXPRESSION) {
            BinaryExpression expr = new BinaryExpression();
            expr.setOp(relationalExpression.children.get(1).t);
            if (relationalExpression.prod.rhs[2] == ParseTable.NONT_ADDITIVEEXPRESSION) {
                expr.setRightExpr(getAdditiveExpression(relationalExpression.children.get(2)));
            } else {
                expr.setRightExpr(new ReferenceType(nameFromNode(relationalExpression.children.get(2))));
            }
            last.setLeftExpr(expr);
            last = expr;

            relationalExpression = relationalExpression.children.get(0);
        }
        last.setLeftExpr(getAdditiveExpression(relationalExpression.children.get(0)));

        return first.getLeftExpr();
    }

    private Expression getAdditiveExpression(Node additiveExpression) {
        // additiveExpression -> multiplicativeExpression
        // additiveExpression -> additiveExpression PLUS multiplicativeExpression
        // additiveExpression -> additiveExpression MINUS multiplicativeExpression
        BinaryExpression first = new BinaryExpression();
        BinaryExpression last = first;
        while (additiveExpression.prod.rhs[0] == ParseTable.NONT_ADDITIVEEXPRESSION) {
            BinaryExpression expr = new BinaryExpression();
            expr.setOp(additiveExpression.children.get(1).t);
            expr.setRightExpr(getMultiplicativeExpression(additiveExpression.children.get(2)));
            last.setLeftExpr(expr);
            last = expr;

            additiveExpression = additiveExpression.children.get(0);
        }
        last.setLeftExpr(getMultiplicativeExpression(additiveExpression.children.get(0)));

        return first.getLeftExpr();
    }

    private Expression getMultiplicativeExpression(Node multiplicativeExpression) {
        // multiplicativeExpression -> unaryExpression
        // multiplicativeExpression -> multiplicativeExpression TIMES unaryExpression
        // multiplicativeExpression -> multiplicativeExpression DIVIDE unaryExpression
        // multiplicativeExpression -> multiplicativeExpression MOD unaryExpression

        BinaryExpression first = new BinaryExpression();
        BinaryExpression last = first;
        while (multiplicativeExpression.prod.rhs[0] == ParseTable.NONT_MULTIPLICATIVEEXPRESSION) {
            BinaryExpression expr = new BinaryExpression();
            expr.setOp(multiplicativeExpression.children.get(1).t);
            expr.setRightExpr(getUnaryExpression(multiplicativeExpression.children.get(2)));
            last.setLeftExpr(expr);
            last = expr;

            multiplicativeExpression = multiplicativeExpression.children.get(0);
        }
        last.setLeftExpr(getUnaryExpression(multiplicativeExpression.children.get(0)));

        return first.getLeftExpr();
    }

    private static final int[] UNARY_INT_LIT_DERIVATION = new int[] {
            ParseTable.NONT_UNARYEXPRESSION,
            ParseTable.NONT_UNARYEXPRESSIONNOTPLUSMINUS,
            ParseTable.NONT_POSTFIXEXPRESSION,
            ParseTable.NONT_PRIMARY,
            ParseTable.NONT_PRIMARYNONEWARRAY,
            ParseTable.NONT_LITERAL
    };
    private Expression getUnaryExpression(Node unaryExpression) {
        // unaryExpression -> MINUS unaryExpression
        // unaryExpression -> unaryExpressionNotPlusMinus

        UnaryExpression first = new UnaryExpression();
        UnaryExpression last = first;
        while (true) {
            int rhs = unaryExpression.prod.rhs[0];
            if (rhs == ParseTable.TERM_MINUS) {
                if (isDerivation(UNARY_INT_LIT_DERIVATION, unaryExpression.children.get(1))) {
                    Token t = getFirstTokensInTree(unaryExpression.children.get(1));
                    t.setVal(t.getVal() * -1);
                    LiteralExpression lit = new LiteralExpression(t);
                    last.setExpression(lit);
                    return first.getExpression();
                } else {
                    UnaryExpression expr = new UnaryExpression();
                    expr.setOp(unaryExpression.children.get(0).t);
                    last.setExpression(expr);
                    last = expr;
                    unaryExpression = unaryExpression.children.get(1);
                }
            } else if (rhs == ParseTable.NONT_UNARYEXPRESSIONNOTPLUSMINUS) {
                // unaryExpressionNotPlusMinus postfixExpression
                // unaryExpressionNotPlusMinus NOT unaryExpression
                // unaryExpressionNotPlusMinus castExpression
                Node unaryExpressionNotPlusMinus = unaryExpression.children.get(0);

                loop:
                while (true) {
                    int next = unaryExpressionNotPlusMinus.prod.rhs[0];
                    switch (next) {
                        case ParseTable.NONT_POSTFIXEXPRESSION: {
                            // postfixExpression primary
                            // postfixExpression name
                            Node postFixExpression = unaryExpressionNotPlusMinus.children.get(0);
                            if (postFixExpression.prod.rhs[0] == ParseTable.NONT_PRIMARY) {
                                last.setExpression(getPrimary(postFixExpression.children.get(0)));
                            } else {
                                last.setExpression(getName(postFixExpression.children.get(0)));
                            }
                            return first.getExpression();
                        }
                        case ParseTable.TERM_NOT: {
                            UnaryExpression expr = new UnaryExpression();
                            expr.setOp(unaryExpressionNotPlusMinus.children.get(0).t);
                            last.setExpression(expr);
                            last = expr;
                            unaryExpression = unaryExpressionNotPlusMinus.children.get(1);
                            break loop;
                        }
                        case ParseTable.NONT_CASTEXPRESSION:
                            // castExpression -> LPAREN primitiveType optDims RPAREN unaryExpression
                            // castExpression -> LPAREN name dims RPAREN unaryExpressionNotPlusMinus
                            // castExpression -> LPAREN expression RPAREN unaryExpressionNotPlusMinus
                            Node castExpression = unaryExpressionNotPlusMinus.children.get(0);
                            CastExpression expr = new CastExpression();
                            expr.setPos(castExpression.children.get(0).t);

                            if (castExpression.prod.rhs[1] == ParseTable.NONT_PRIMITIVETYPE) {
                                String typeName = nameFromNode(castExpression.children.get(1));
                                if (castExpression.children.get(2).prod.rhs.length == 1) {
                                    typeName += "[]";
                                }
                                expr.setCast(new ReferenceType(typeName));
                                last.setExpression(expr);
                                last = expr;
                                unaryExpression = castExpression.children.get(4);
                                break loop;
                            } else if (castExpression.prod.rhs[1] == ParseTable.NONT_NAME) {
                                expr.setCast(new ReferenceType(getName(castExpression.children.get(1)), true));
                                last.setExpression(expr);
                                last = expr;
                                unaryExpressionNotPlusMinus = castExpression.children.get(4);
                            } else {
                                Expression e = getExpression(castExpression.children.get(1));
                                if (e instanceof NameVariable) {
                                    expr.setCast(new ReferenceType((NameVariable) e, false));
                                    last.setExpression(expr);
                                    last = expr;
                                    unaryExpressionNotPlusMinus = castExpression.children.get(3);
                                } else {
                                    throw new AstException(fn.getFilePath(), expr,
                                            "Invalid cast expression. Expression: " +
                                                    e.getClass().getSimpleName() + ": " + e.toString());
                                }
                            }
                            break;
                    }
                }
            } else {
                throw new IllegalStateException("UnaryExpression rule not found.");
            }
        }
    }

    private FieldVariable getFieldAccess(Node fieldAccess) {
        // fieldAccess -> primary DOT ID
        return new FieldVariable(fieldAccess.children.get(2).t,
                getPrimary(fieldAccess.children.get(0)));
    }

    private Expression getPrimary(Node primary) {
        // primary primaryNoNewArray
        // primary arrayCreationExpression
        int rhs = primary.prod.rhs[0];

        if (rhs == ParseTable.NONT_PRIMARYNONEWARRAY) {
            return getPrimaryNoNewArray(primary.children.get(0));
        } else if (rhs == ParseTable.NONT_ARRAYCREATIONEXPRESSION) {
            // arrayCreationExpression -> NEW primitiveType dimExpr
            // arrayCreationExpression -> NEW classOrInterfaceType dimExpr
            Node arrayCreationExpression = primary.children.get(0);
            ArrayCreationExpression expr = new ArrayCreationExpression();
            expr.setPos(arrayCreationExpression.children.get(0).t);

            List<Token> toks = ParserUtils.getTokensInTree(arrayCreationExpression.children.get(1));
            ReferenceType refType = new ReferenceType(toks, false);
            refType.setPos(toks.get(0));
            expr.setTypeExpr(refType);
            // dimExpr -> LBRACKET expression RBRACKET
            expr.setDimExpr(getExpression(arrayCreationExpression.children.get(2).children.get(1)));
            return expr;
        }

        throw new IllegalStateException("Primary rule not found.");
    }

    private Expression getPrimaryNoNewArray(Node primaryNoNewArray) {
        // primaryNoNewArray literal
        // primaryNoNewArray THIS
        // primaryNoNewArray LPAREN expression RPAREN
        // primaryNoNewArray classInstanceCreationExpression
        // primaryNoNewArray fieldAccess
        // primaryNoNewArray methodInvocation
        // primaryNoNewArray arrayAccess
        switch (primaryNoNewArray.prod.rhs[0]) {
            case ParseTable.NONT_LITERAL:
                return getLiteral(primaryNoNewArray.children.get(0));
            case ParseTable.TERM_THIS:
                return new ThisExpression(primaryNoNewArray.children.get(0).t);
            case ParseTable.TERM_LPAREN: {
                Expression expr = getExpression(primaryNoNewArray.children.get(1));
                expr.setEnclosedInParen(true);
                return expr;
            }
            case ParseTable.NONT_CLASSINSTANCECREATIONEXPRESSION:
                return getClassInstanceCreationExpression(primaryNoNewArray.children.get(0));
            case ParseTable.NONT_FIELDACCESS:
                return getFieldAccess(primaryNoNewArray.children.get(0));
            case ParseTable.NONT_METHODINVOCATION:
                return getMethodInvocation(primaryNoNewArray.children.get(0));
            case ParseTable.NONT_ARRAYACCESS:
                return getArrayAccess(primaryNoNewArray.children.get(0));
            default:
                throw new IllegalStateException("PrimaryNoNewArray rule not found.");
        }
    }

    private Expression getLiteral(Node literal) {
        // literal INTLIT
        // literal TRUE
        // literal FALSE
        // literal CHARLIT
        // literal STRINGLIT
        // literal NULL

        Token t = literal.children.get(0).t;
        switch (t.getType()) {
            case INTLIT:
                if (t.getVal() > Integer.MAX_VALUE || t.getVal() < Integer.MIN_VALUE) {
                    throw new WeedException(fn.getFilePath(), t, String.format("Integer '%d' out of bounds", t.getVal()));
                }
                break;
        }

        return new LiteralExpression(t);
    }

    private Expression getArrayAccess(Node arrayAccess) {
        // arrayAccess -> name LBRACKET expression RBRACKET
        // arrayAccess -> primaryNoNewArray LBRACKET expression RBRACKET
        ArrayAccessExpression expr = new ArrayAccessExpression();

        if (arrayAccess.prod.rhs[0] == ParseTable.NONT_NAME) {
            expr.setArrayExpr(getName(arrayAccess.children.get(0)));
        } else {
            expr.setArrayExpr(getPrimaryNoNewArray(arrayAccess.children.get(0)));
        }

        expr.setIndexExpr(getExpression(arrayAccess.children.get(2)));
        return expr;
    }

    private Expression getMethodInvocation(Node methodInvocation) {
        // methodInvocation -> name LPAREN optArgumentList RPAREN
        // methodInvocation -> primary DOT ID LPAREN optArgumentList RPAREN
        MethodExpression methodExpr = new MethodExpression();
        methodExpr.setPos(getFirstTokensInTree(methodInvocation));

        if (methodInvocation.prod.rhs[0] == ParseTable.NONT_NAME) {
            NameVariable nVar = getName(methodInvocation.children.get(0));
            methodExpr.setMethodIdExpr(nVar);
            methodExpr.setArgList(getOptArgumentList(methodInvocation.children.get(2)));
        } else if (methodInvocation.prod.rhs[0] == ParseTable.NONT_PRIMARY) {
            FieldVariable fVar = new FieldVariable(methodInvocation.children.get(2).t,
                    getPrimary(methodInvocation.children.get(0)));
            methodExpr.setMethodIdExpr(fVar);
            methodExpr.setArgList(getOptArgumentList(methodInvocation.children.get(4)));
        } else {
            throw new IllegalStateException("MethodInvocation rule not found.");
        }

        return methodExpr;
    }

    private NameVariable getName(Node name) {
        List<Token> toks = ParserUtils.getIdSeqInTree(name);
        NameVariable nVar = new NameVariable(toks);
        return nVar;
    }

    private Expression getClassInstanceCreationExpression(Node classInstanceCreationExpression) {
        // classInstanceCreationExpression -> NEW classType LPAREN optArgumentList RPAREN
        ICreationExpression iExpr = new ICreationExpression();
        iExpr.setPos(classInstanceCreationExpression.children.get(0).t);
        iExpr.setType(new ReferenceType(ParserUtils.getIdSeqInTree(classInstanceCreationExpression.children.get(1)), false));
        iExpr.setArgList(getOptArgumentList(classInstanceCreationExpression.children.get(3)));
        return iExpr;
    }

    private List<Expression> getOptArgumentList(Node optArgumentList) {
        // optArgumentList -> argumentList
        // optArgumentList
        List<Expression> argList = new ArrayList<>();

        if (optArgumentList.prod.rhs.length != 0) {
            // argumentList expression
            // argumentList argumentList COMMA expression
            Node argumentList = optArgumentList.children.get(0);
            List<Node> expressions = listFromTree(argumentList, 0, 2);
            for (Node expression : ListUtils.reverse(expressions)) {
                argList.add(getExpression(expression));
            }
        }

        return argList;
    }

    private Expression getExpression(Node node) {
        // expression -> assignmentExpression
        return getAssignmentExpression(node.children.get(0));
    }

    private void doPackageDecl(Node node) {
        // optPackageDeclaration -> PACKAGE name SEMI
        // optPackageDeclaration

        fn.setPackageName(node.children.size() == 0 ? Ast.PACKAGE_UNNAMED :
                nameFromNode(node.children.get(1)));
    }

    private void doImportsDecl(Node node) {
        // optImportDeclarations -> importDeclarations
        // optImportDeclarations
        // importDeclarations -> importDeclaration
        // importDeclarations -> importDeclarations importDeclaration

        if (node.children.size() != 0) {
            Node importDeclarations = node.children.get(0);
            List<Node> importDeclarationss = listFromTree(importDeclarations);
            for (Node n : ListUtils.reverse(importDeclarationss)) {
                doImportDecl(n);
            }
        }
    }

    private void doImportDecl(Node importDecl) {
        // importDeclaration -> singleTypeImportDeclaration
        // importDeclaration -> typeImportOnDemandDeclaration
        // importDeclaration -> SEMI
        // singleTypeImportDeclaration -> IMPORT name SEMI
        // typeImportOnDemandDeclaration -> IMPORT name DOT TIMES SEMI

        if (importDecl.prod.rhs[0] == ParseTable.NONT_SINGLETYPEIMPORTDECLARATION) {
            fn.addImport(nameFromNode(importDecl.children.get(0).children.get(1)));
        } else if (importDecl.prod.rhs[0] == ParseTable.NONT_TYPEIMPORTONDEMANDDECLARATION) {
            fn.addImport(nameFromNode(importDecl.children.get(0).children.get(1)) + ".*");
        }
    }

    private List<Node> listFromTree(Node node) {
        return listFromTree(node, 0, 1);
    }

    private List<Node> listFromTree(Node node, int aIndex, int bIndex) {
        return listFromTree(node, aIndex, bIndex, 0, 1);
    }

    private List<Node> listFromTree(Node node, int aIndex, int bIndex, int cIndex, int shorterRuleRhsLen) {
        // A -> B
        //      ^- cIndex
        // A -> A B <- bIndex
        //      ^- aIndex
        List<Node> nodes = new ArrayList<>();

        while (node.children.size() != shorterRuleRhsLen) {
            nodes.add(node.children.get(bIndex));
            node = node.children.get(aIndex);
        }
        nodes.add(node.children.get(cIndex));

        return nodes;
    }

    private String nameFromNode(Node n) {
        // name -> simpleName
        // name -> qualifiedName
        // simpleName -> ID
        // qualifiedName -> name DOT ID
        StringBuilder sb = new StringBuilder();
        List<Token> toks = ParserUtils.getTokensInTree(n);
        for (Token t : toks) {
            sb.append(t.getRaw());
        }
        return sb.toString();
    }

    private Token getFirstTokensInTree(Node tree) {
        Stack<Node> toVisit = new Stack<>();
        toVisit.push(tree);

        while (!toVisit.isEmpty()) {
            Node n = toVisit.pop();
            if (n.t == null) {
                for (Node child : ListUtils.reverse(n.children)) {
                    toVisit.push(child);
                }
            } else {
                return n.t;
            }
        }

        return null;
    }

    /**
     * Checks if a node follows a certain derivation
     * @param derivations
     * @param n
     * @return
     */
    private boolean isDerivation(int[] derivations, Node n) {
        for (int i = 0; i < derivations.length; i++) {
            if (n.prod == null || n.prod.lhs != derivations[i]) {
                return false;
            }

            n = n.children.get(0);
        }
        return true;
    }

}
