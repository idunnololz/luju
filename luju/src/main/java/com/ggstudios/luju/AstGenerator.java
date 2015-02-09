package com.ggstudios.luju;

import com.ggstudios.luju.Parser.Node;
import com.ggstudios.types.AbstractMethodDecl;
import com.ggstudios.types.Block;
import com.ggstudios.types.ClassDecl;
import com.ggstudios.types.ConstructorDecl;
import com.ggstudios.types.Expression;
import com.ggstudios.types.FieldDecl;
import com.ggstudios.types.FieldInitDecl;
import com.ggstudios.types.InterfaceDecl;
import com.ggstudios.types.MethodDecl;
import com.ggstudios.types.TypeDecl;
import com.ggstudios.types.UserType;
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
        InterfaceDecl decl = new InterfaceDecl();
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
                MethodDecl methodDecl = new AbstractMethodDecl();
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
        ClassDecl decl = new ClassDecl();
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

        decl.addConstructorDeclaration(constructorDecl);
    }

    private void doMethodDecl(ClassDecl decl, Node node) {
        // methodDeclaration -> methodHeader methodBody
        MethodDecl methodDecl;

        boolean isBodiless = node.children.get(1).prod.rhs[0] == ParseTable.TERM_SEMI;
        if (isBodiless) {
            methodDecl = new AbstractMethodDecl();
        } else {
            methodDecl = new MethodDecl();
        }

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
            methodDecl.setReturnType(new UserType(nameFromNode(returnType)));
        } else {
            methodDecl.setReturnType(new UserType("void"));
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
            for (Node n : formalParameters) {
                FieldDecl arg = new FieldDecl();
                Token argLandmark = getFirstTokensInTree(n.children.get(1));
                arg.setPos(argLandmark.getRow(), argLandmark.getCol());
                arg.setType(new UserType(nameFromNode(n.children.get(0))));
                arg.setId(getFirstTokensInTree(n.children.get(1)));
                methodDecl.addArgument(arg);
            }
        }
    }

    private void doFieldDecl(ClassDecl decl, Node node) {
        // fieldDeclaration -> optModifiers type variableDeclarator SEMI
        // fieldDeclaration -> optModifiers type variableDeclarator ASSIGN expression SEMI

        FieldDecl fieldDecl = null;

        if (node.prod.rhs.length == 4) {
            fieldDecl = new FieldDecl();
        } else {
            fieldDecl = new FieldInitDecl();
        }

        List<Token> toks = ParserUtils.getTokensInTree(node.children.get(0));

        Token landmark = getFirstTokensInTree(node.children.get(0));
        fieldDecl.setPos(landmark.getRow(), landmark.getCol());

        for (Token t : toks) {
            fieldDecl.addModifier(t.getType());
        }

        fieldDecl.setType(new UserType(nameFromNode(node.children.get(1))));

        // variableDeclarator -> variableDeclaratorId
        // variableDeclaratorId -> ID
        Node variableDeclarator = node.children.get(2);
        Token t = getFirstTokensInTree(variableDeclarator);
        fieldDecl.setId(t);

        if (node.prod.rhs.length != 4) {
            FieldInitDecl fiDecl = (FieldInitDecl) fieldDecl;

            fiDecl.setExpr(getExpression(decl, node.children.get(4)));
        }

        decl.addFieldDeclaration(fieldDecl);
    }

    private Block getBlock(Node node) {
        //block -> LBRACE optBlockStatements RBRACE
        Block block = new Block();

        // TODO

        return block;
    }

    private Expression getExpression(ClassDecl decl, Node node) {
        // expression -> assignmentExpression

        Expression expr = new Expression();

        // TODO


        return expr;
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
            Node imports = node.children.get(0);
            while (imports.children.size() == 2) {
                doImportDecl(imports.children.get(1));
                imports = imports.children.get(0);
            }
            doImportDecl(imports.children.get(0));
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

}
