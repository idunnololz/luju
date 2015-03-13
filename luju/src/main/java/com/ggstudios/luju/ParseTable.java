package com.ggstudios.luju;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseTable {
    private static final String LALR_TABLE_FILE = "luju/res/joos.lalr";

    private static final int SHIFT = 0x80000000;
    private static final int REDUCE = 0x40000000;
    private static final int ACTION_ID_MASK = 0x0000FFFF;

    private int startState;

    private List<LrProduction> ruleMap = new ArrayList<>();

    private static final Map<Integer, Integer> actionMap = new HashMap<>();
    private static final Map<Integer, Integer> gotoMap = new HashMap<>();

    public ParseTable() {
        loadTableFromFile(LALR_TABLE_FILE);
    }

    public LrProduction getProduction(int i) {
        return ruleMap.get(i);
    }

    public Integer getLrAction(int i, int term) {
        return actionMap.get(i + (term << 16));
    }

    public boolean isActionShift(int action) {
        return (action & SHIFT) != 0;
    }

    public boolean isActionReduce(int action) {
        return (action & REDUCE) != 0;
    }

    public int getActionId(int action) {
        return action & ACTION_ID_MASK;
    }

    public int getLrGoto(int i, int nonterm) {
        return gotoMap.get(i + (nonterm << 16));
    }

    public int getStartState() { return startState; }

    public static String getNameOfId(int id) {
        return idToSymbolName.get(id);
    }

    private void loadTableFromFile(String fileName) {
        File file = new File(fileName);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            int termCount = Integer.valueOf(reader.readLine());
            for (int i = 0; i < termCount; i++) {
                reader.readLine();
            }
            int nonTermCount = Integer.valueOf(reader.readLine());
            for (int i = 0; i < nonTermCount; i++) {
                reader.readLine();
            }
            startState = symbolNameToId.get(reader.readLine()); // the start state...

            int ruleCount = Integer.valueOf(reader.readLine());
            for (int i = 0; i < ruleCount; i++) {
                String[] toks = reader.readLine().split(" ");
                int[] rhs = new int[toks.length - 1];
                for (int j = 1; j < toks.length; j++) {
                    rhs[j - 1] = symbolNameToId.get(toks[j]);
                }
                ruleMap.add(new LrProduction(symbolNameToId.get(toks[0]), rhs));
            }

            reader.readLine();

            int actGotoCount = Integer.valueOf(reader.readLine());
            for (int i = 0; i < actGotoCount; i++) {
                String[] toks = reader.readLine().split(" ");
                if (isTerm(toks[1])) {
                    actionMap.put(Integer.valueOf(toks[0]) + (symbolNameToId.get(toks[1]) << 16),
                            symbolNameToId.get(toks[2]) | Integer.valueOf(toks[3]));
                } else {
                    gotoMap.put(Integer.valueOf(toks[0]) + (symbolNameToId.get(toks[1]) << 16),
                            Integer.valueOf(toks[3]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final int TERM_EOF = 0x0;
    public static final int TERM_BOF = 0x1;
    public static final int TERM_ID = 0x2;
    public static final int TERM_PLUS = 0x3;
    public static final int TERM_MINUS = 0x4;
    public static final int TERM_TIMES = 0x5;
    public static final int TERM_DIVIDE = 0x6;
    public static final int TERM_MOD = 0x7;
    public static final int TERM_ANDS = 0x8;
    public static final int TERM_ANDL = 0x9;
    public static final int TERM_ORS = 0xa;
    public static final int TERM_ORL = 0xb;
    public static final int TERM_NOT = 0xc;
    public static final int TERM_EQUALS = 0xd;
    public static final int TERM_NOTEQUALS = 0xe;
    public static final int TERM_LTEQ = 0xf;
    public static final int TERM_GTEQ = 0x10;
    public static final int TERM_LTTHAN = 0x11;
    public static final int TERM_GTTHAN = 0x12;
    public static final int TERM_ASSIGN = 0x13;
    public static final int TERM_LPAREN = 0x14;
    public static final int TERM_RPAREN = 0x15;
    public static final int TERM_LBRACE = 0x16;
    public static final int TERM_RBRACE = 0x17;
    public static final int TERM_LBRACKET = 0x18;
    public static final int TERM_RBRACKET = 0x19;
    public static final int TERM_INTLIT = 0x1a;
    public static final int TERM_STRINGLIT = 0x1b;
    public static final int TERM_CHARLIT = 0x1c;
    public static final int TERM_INT = 0x1d;
    public static final int TERM_CHAR = 0x1e;
    public static final int TERM_SHORT = 0x1f;
    public static final int TERM_BOOLEAN = 0x20;
    public static final int TERM_BYTE = 0x21;
    public static final int TERM_PUBLIC = 0x22;
    public static final int TERM_PROTECTED = 0x23;
    public static final int TERM_NATIVE = 0x24;
    public static final int TERM_ABSTRACT = 0x25;
    public static final int TERM_FINAL = 0x26;
    public static final int TERM_CONST = 0x27;
    public static final int TERM_STATIC = 0x28;
    public static final int TERM_RETURN = 0x29;
    public static final int TERM_VOID = 0x2a;
    public static final int TERM_NULL = 0x2b;
    public static final int TERM_NEW = 0x2c;
    public static final int TERM_IF = 0x2d;
    public static final int TERM_ELSE = 0x2e;
    public static final int TERM_FOR = 0x2f;
    public static final int TERM_WHILE = 0x30;
    public static final int TERM_TRUE = 0x31;
    public static final int TERM_FALSE = 0x32;
    public static final int TERM_CLASS = 0x33;
    public static final int TERM_EXTENDS = 0x34;
    public static final int TERM_IMPLEMENTS = 0x35;
    public static final int TERM_IMPORT = 0x36;
    public static final int TERM_INSTANCEOF = 0x37;
    public static final int TERM_INTERFACE = 0x38;
    public static final int TERM_PACKAGE = 0x39;
    public static final int TERM_DOT = 0x3a;
    public static final int TERM_COMMA = 0x3b;
    public static final int TERM_SEMI = 0x3c;
    public static final int TERM_THIS = 0x3d;
    public static final int TERM_INVALID = 0x1000;

    public static final int NONT_GOAL = 0x80000000;
    public static final int NONT_LITERAL = 0x80000001;
    public static final int NONT_TYPE = 0x80000002;
    public static final int NONT_PRIMITIVETYPE = 0x80000003;
    public static final int NONT_NUMERICTYPE = 0x80000004;
    public static final int NONT_REFERENCETYPE = 0x80000005;
    public static final int NONT_CLASSORINTERFACETYPE = 0x80000006;
    public static final int NONT_CLASSTYPE = 0x80000007;
    public static final int NONT_INTERFACETYPE = 0x80000008;
    public static final int NONT_ARRAYTYPE = 0x80000009;
    public static final int NONT_NAME = 0x8000000a;
    public static final int NONT_SIMPLENAME = 0x8000000b;
    public static final int NONT_QUALIFIEDNAME = 0x8000000c;
    public static final int NONT_COMPILATIONUNIT = 0x8000000d;
    public static final int NONT_OPTPACKAGEDECLARATION = 0x8000000e;
    public static final int NONT_OPTIMPORTDECLARATIONS = 0x8000000f;
    public static final int NONT_OPTSEMICOLONS = 0x80000010;
    public static final int NONT_IMPORTDECLARATIONS = 0x80000011;
    public static final int NONT_IMPORTDECLARATION = 0x80000012;
    public static final int NONT_SINGLETYPEIMPORTDECLARATION = 0x80000013;
    public static final int NONT_TYPEIMPORTONDEMANDDECLARATION = 0x80000014;
    public static final int NONT_TYPEDECLARATION = 0x80000015;
    public static final int NONT_MODIFIERS = 0x80000016;
    public static final int NONT_MODIFIER = 0x80000017;
    public static final int NONT_OPTMODIFIERS = 0x80000018;
    public static final int NONT_CLASSDECLARATION = 0x80000019;
    public static final int NONT_OPTSUPER = 0x8000001a;
    public static final int NONT_OPTINTERFACES = 0x8000001b;
    public static final int NONT_INTERFACETYPELIST = 0x8000001c;
    public static final int NONT_CLASSBODY = 0x8000001d;
    public static final int NONT_OPTCLASSBODYDECLARATIONS = 0x8000001e;
    public static final int NONT_CLASSBODYDECLARATIONS = 0x8000001f;
    public static final int NONT_CLASSBODYDECLARATION = 0x80000020;
    public static final int NONT_CLASSMEMBERDECLARATION = 0x80000021;
    public static final int NONT_FIELDDECLARATION = 0x80000022;
    public static final int NONT_VARIABLEDECLARATOR = 0x80000023;
    public static final int NONT_VARIABLEDECLARATORID = 0x80000024;
    public static final int NONT_METHODDECLARATION = 0x80000025;
    public static final int NONT_METHODHEADER = 0x80000026;
    public static final int NONT_METHODDECLARATOR = 0x80000027;
    public static final int NONT_OPTFORMALPARAMETERLIST = 0x80000028;
    public static final int NONT_FORMALPARAMETERLIST = 0x80000029;
    public static final int NONT_FORMALPARAMETER = 0x8000002a;
    public static final int NONT_METHODBODY = 0x8000002b;
    public static final int NONT_CONSTRUCTORDECLARATION = 0x8000002c;
    public static final int NONT_CONSTRUCTORDECLARATOR = 0x8000002d;
    public static final int NONT_INTERFACEDECLARATION = 0x8000002e;
    public static final int NONT_OPTEXTENDSINTERFACES = 0x8000002f;
    public static final int NONT_EXTENDSINTERFACES = 0x80000030;
    public static final int NONT_INTERFACEBODY = 0x80000031;
    public static final int NONT_OPTINTERFACEMEMBERDECLARATIONS = 0x80000032;
    public static final int NONT_INTERFACEMEMBERDECLARATIONS = 0x80000033;
    public static final int NONT_ABSTRACTMETHODDECLARATION = 0x80000034;
    public static final int NONT_BLOCK = 0x80000035;
    public static final int NONT_OPTBLOCKSTATEMENTS = 0x80000036;
    public static final int NONT_BLOCKSTATEMENTS = 0x80000037;
    public static final int NONT_BLOCKSTATEMENT = 0x80000038;
    public static final int NONT_LOCALVARIABLEDECLARATIONSTATEMENT = 0x80000039;
    public static final int NONT_LOCALVARIABLEDECLARATION = 0x8000003a;
    public static final int NONT_STATEMENT = 0x8000003b;
    public static final int NONT_STATEMENTNOSHORTIF = 0x8000003c;
    public static final int NONT_STATEMENTWITHOUTTRAILINGSUBSTATEMENT = 0x8000003d;
    public static final int NONT_EMPTYSTATEMENT = 0x8000003e;
    public static final int NONT_EXPRESSIONSTATEMENT = 0x8000003f;
    public static final int NONT_STATEMENTEXPRESSION = 0x80000040;
    public static final int NONT_IFTHENSTATEMENT = 0x80000041;
    public static final int NONT_IFTHENELSESTATEMENT = 0x80000042;
    public static final int NONT_IFTHENELSESTATEMENTNOSHORTIF = 0x80000043;
    public static final int NONT_WHILESTATEMENT = 0x80000044;
    public static final int NONT_WHILESTATEMENTNOSHORTIF = 0x80000045;
    public static final int NONT_FORSTATEMENT = 0x80000046;
    public static final int NONT_OPTFORINIT = 0x80000047;
    public static final int NONT_OPTEXPRESSION = 0x80000048;
    public static final int NONT_OPTFORUPDATE = 0x80000049;
    public static final int NONT_FORSTATEMENTNOSHORTIF = 0x8000004a;
    public static final int NONT_FORINIT = 0x8000004b;
    public static final int NONT_FORUPDATE = 0x8000004c;
    public static final int NONT_RETURNSTATEMENT = 0x8000004d;
    public static final int NONT_PRIMARY = 0x8000004e;
    public static final int NONT_PRIMARYNONEWARRAY = 0x8000004f;
    public static final int NONT_CLASSINSTANCECREATIONEXPRESSION = 0x80000050;
    public static final int NONT_OPTARGUMENTLIST = 0x80000051;
    public static final int NONT_ARGUMENTLIST = 0x80000052;
    public static final int NONT_ARRAYCREATIONEXPRESSION = 0x80000053;
    public static final int NONT_DIMEXPR = 0x80000054;
    public static final int NONT_OPTDIMS = 0x80000055;
    public static final int NONT_DIMS = 0x80000056;
    public static final int NONT_FIELDACCESS = 0x80000057;
    public static final int NONT_METHODINVOCATION = 0x80000058;
    public static final int NONT_ARRAYACCESS = 0x80000059;
    public static final int NONT_POSTFIXEXPRESSION = 0x8000005a;
    public static final int NONT_UNARYEXPRESSION = 0x8000005b;
    public static final int NONT_UNARYEXPRESSIONNOTPLUSMINUS = 0x8000005c;
    public static final int NONT_CASTEXPRESSION = 0x8000005d;
    public static final int NONT_MULTIPLICATIVEEXPRESSION = 0x8000005e;
    public static final int NONT_ADDITIVEEXPRESSION = 0x8000005f;
    public static final int NONT_RELATIONALEXPRESSION = 0x80000060;
    public static final int NONT_EQUALITYEXPRESSION = 0x80000061;
    public static final int NONT_ANDEXPRESSION = 0x80000062;
    public static final int NONT_INCLUSIVEOREXPRESSION = 0x80000063;
    public static final int NONT_CONDITIONALANDEXPRESSION = 0x80000064;
    public static final int NONT_CONDITIONALOREXPRESSION = 0x80000065;
    public static final int NONT_ASSIGNMENTEXPRESSION = 0x80000066;
    public static final int NONT_ASSIGNMENT = 0x80000067;
    public static final int NONT_LEFTHANDSIDE = 0x80000068;
    public static final int NONT_EXPRESSION = 0x80000069;
    public static final int NONT_CONSTANTEXPRESSION = 0x8000006a;

    private static final Map<String, Integer> symbolNameToId = new HashMap<>();
    private static final Map<Integer, String> idToSymbolName = new HashMap<>();

    static {
        Map<String, Integer> a = symbolNameToId;
        a.put("EOF", TERM_EOF);
        a.put("BOF", TERM_BOF);
        a.put("ID", TERM_ID);
        a.put("PLUS", TERM_PLUS);
        a.put("MINUS", TERM_MINUS);
        a.put("TIMES", TERM_TIMES);
        a.put("DIVIDE", TERM_DIVIDE);
        a.put("MOD", TERM_MOD);
        a.put("ANDS", TERM_ANDS);
        a.put("ANDL", TERM_ANDL);
        a.put("ORS", TERM_ORS);
        a.put("ORL", TERM_ORL);
        a.put("NOT", TERM_NOT);
        a.put("EQUALS", TERM_EQUALS);
        a.put("NOTEQUALS", TERM_NOTEQUALS);
        a.put("LTEQ", TERM_LTEQ);
        a.put("GTEQ", TERM_GTEQ);
        a.put("LTTHAN", TERM_LTTHAN);
        a.put("GTTHAN", TERM_GTTHAN);
        a.put("ASSIGN", TERM_ASSIGN);
        a.put("LPAREN", TERM_LPAREN);
        a.put("RPAREN", TERM_RPAREN);
        a.put("LBRACE", TERM_LBRACE);
        a.put("RBRACE", TERM_RBRACE);
        a.put("LBRACKET", TERM_LBRACKET);
        a.put("RBRACKET", TERM_RBRACKET);
        a.put("INTLIT", TERM_INTLIT);
        a.put("STRINGLIT", TERM_STRINGLIT);
        a.put("CHARLIT", TERM_CHARLIT);
        a.put("INT", TERM_INT);
        a.put("CHAR", TERM_CHAR);
        a.put("SHORT", TERM_SHORT);
        a.put("BOOLEAN", TERM_BOOLEAN);
        a.put("BYTE", TERM_BYTE);
        a.put("PUBLIC", TERM_PUBLIC);
        a.put("PROTECTED", TERM_PROTECTED);
        a.put("NATIVE", TERM_NATIVE);
        a.put("ABSTRACT", TERM_ABSTRACT);
        a.put("FINAL", TERM_FINAL);
        a.put("CONST", TERM_CONST);
        a.put("STATIC", TERM_STATIC);
        a.put("RETURN", TERM_RETURN);
        a.put("VOID", TERM_VOID);
        a.put("NULL", TERM_NULL);
        a.put("NEW", TERM_NEW);
        a.put("IF", TERM_IF);
        a.put("ELSE", TERM_ELSE);
        a.put("FOR", TERM_FOR);
        a.put("WHILE", TERM_WHILE);
        a.put("TRUE", TERM_TRUE);
        a.put("FALSE", TERM_FALSE);
        a.put("CLASS", TERM_CLASS);
        a.put("EXTENDS", TERM_EXTENDS);
        a.put("IMPLEMENTS", TERM_IMPLEMENTS);
        a.put("IMPORT", TERM_IMPORT);
        a.put("INSTANCEOF", TERM_INSTANCEOF);
        a.put("INTERFACE", TERM_INTERFACE);
        a.put("PACKAGE", TERM_PACKAGE);
        a.put("DOT", TERM_DOT);
        a.put("COMMA", TERM_COMMA);
        a.put("SEMI", TERM_SEMI);
        a.put("THIS", TERM_THIS);

        a.put("goal", NONT_GOAL);
        a.put("literal", NONT_LITERAL);
        a.put("type", NONT_TYPE);
        a.put("primitiveType", NONT_PRIMITIVETYPE);
        a.put("numericType", NONT_NUMERICTYPE);
        a.put("referenceType", NONT_REFERENCETYPE);
        a.put("classOrInterfaceType", NONT_CLASSORINTERFACETYPE);
        a.put("classType", NONT_CLASSTYPE);
        a.put("interfaceType", NONT_INTERFACETYPE);
        a.put("arrayType", NONT_ARRAYTYPE);
        a.put("name", NONT_NAME);
        a.put("simpleName", NONT_SIMPLENAME);
        a.put("qualifiedName", NONT_QUALIFIEDNAME);
        a.put("compilationUnit", NONT_COMPILATIONUNIT);
        a.put("optPackageDeclaration", NONT_OPTPACKAGEDECLARATION);
        a.put("optImportDeclarations", NONT_OPTIMPORTDECLARATIONS);
        a.put("optSemicolons", NONT_OPTSEMICOLONS);
        a.put("importDeclarations", NONT_IMPORTDECLARATIONS);
        a.put("importDeclaration", NONT_IMPORTDECLARATION);
        a.put("singleTypeImportDeclaration", NONT_SINGLETYPEIMPORTDECLARATION);
        a.put("typeImportOnDemandDeclaration", NONT_TYPEIMPORTONDEMANDDECLARATION);
        a.put("typeDeclaration", NONT_TYPEDECLARATION);
        a.put("modifiers", NONT_MODIFIERS);
        a.put("modifier", NONT_MODIFIER);
        a.put("optModifiers", NONT_OPTMODIFIERS);
        a.put("classDeclaration", NONT_CLASSDECLARATION);
        a.put("optSuper", NONT_OPTSUPER);
        a.put("optInterfaces", NONT_OPTINTERFACES);
        a.put("interfaceTypeList", NONT_INTERFACETYPELIST);
        a.put("classBody", NONT_CLASSBODY);
        a.put("optClassBodyDeclarations", NONT_OPTCLASSBODYDECLARATIONS);
        a.put("classBodyDeclarations", NONT_CLASSBODYDECLARATIONS);
        a.put("classBodyDeclaration", NONT_CLASSBODYDECLARATION);
        a.put("classMemberDeclaration", NONT_CLASSMEMBERDECLARATION);
        a.put("fieldDeclaration", NONT_FIELDDECLARATION);
        a.put("variableDeclarator", NONT_VARIABLEDECLARATOR);
        a.put("variableDeclaratorId", NONT_VARIABLEDECLARATORID);
        a.put("methodDeclaration", NONT_METHODDECLARATION);
        a.put("methodHeader", NONT_METHODHEADER);
        a.put("methodDeclarator", NONT_METHODDECLARATOR);
        a.put("optFormalParameterList", NONT_OPTFORMALPARAMETERLIST);
        a.put("formalParameterList", NONT_FORMALPARAMETERLIST);
        a.put("formalParameter", NONT_FORMALPARAMETER);
        a.put("methodBody", NONT_METHODBODY);
        a.put("constructorDeclaration", NONT_CONSTRUCTORDECLARATION);
        a.put("constructorDeclarator", NONT_CONSTRUCTORDECLARATOR);
        a.put("interfaceDeclaration", NONT_INTERFACEDECLARATION);
        a.put("optExtendsInterfaces", NONT_OPTEXTENDSINTERFACES);
        a.put("extendsInterfaces", NONT_EXTENDSINTERFACES);
        a.put("interfaceBody", NONT_INTERFACEBODY);
        a.put("optInterfaceMemberDeclarations", NONT_OPTINTERFACEMEMBERDECLARATIONS);
        a.put("interfaceMemberDeclarations", NONT_INTERFACEMEMBERDECLARATIONS);
        a.put("abstractMethodDeclaration", NONT_ABSTRACTMETHODDECLARATION);
        a.put("block", NONT_BLOCK);
        a.put("optBlockStatements", NONT_OPTBLOCKSTATEMENTS);
        a.put("blockStatements", NONT_BLOCKSTATEMENTS);
        a.put("blockStatement", NONT_BLOCKSTATEMENT);
        a.put("localVariableDeclarationStatement", NONT_LOCALVARIABLEDECLARATIONSTATEMENT);
        a.put("localVariableDeclaration", NONT_LOCALVARIABLEDECLARATION);
        a.put("statement", NONT_STATEMENT);
        a.put("statementNoShortIf", NONT_STATEMENTNOSHORTIF);
        a.put("statementWithoutTrailingSubstatement", NONT_STATEMENTWITHOUTTRAILINGSUBSTATEMENT);
        a.put("emptyStatement", NONT_EMPTYSTATEMENT);
        a.put("expressionStatement", NONT_EXPRESSIONSTATEMENT);
        a.put("statementExpression", NONT_STATEMENTEXPRESSION);
        a.put("ifThenStatement", NONT_IFTHENSTATEMENT);
        a.put("ifThenElseStatement", NONT_IFTHENELSESTATEMENT);
        a.put("ifThenElseStatementNoShortIf", NONT_IFTHENELSESTATEMENTNOSHORTIF);
        a.put("whileStatement", NONT_WHILESTATEMENT);
        a.put("whileStatementNoShortIf", NONT_WHILESTATEMENTNOSHORTIF);
        a.put("forStatement", NONT_FORSTATEMENT);
        a.put("optForInit", NONT_OPTFORINIT);
        a.put("optExpression", NONT_OPTEXPRESSION);
        a.put("optForUpdate", NONT_OPTFORUPDATE);
        a.put("forStatementNoShortIf", NONT_FORSTATEMENTNOSHORTIF);
        a.put("forInit", NONT_FORINIT);
        a.put("forUpdate", NONT_FORUPDATE);
        a.put("returnStatement", NONT_RETURNSTATEMENT);
        a.put("primary", NONT_PRIMARY);
        a.put("primaryNoNewArray", NONT_PRIMARYNONEWARRAY);
        a.put("classInstanceCreationExpression", NONT_CLASSINSTANCECREATIONEXPRESSION);
        a.put("optArgumentList", NONT_OPTARGUMENTLIST);
        a.put("argumentList", NONT_ARGUMENTLIST);
        a.put("arrayCreationExpression", NONT_ARRAYCREATIONEXPRESSION);
        a.put("dimExpr", NONT_DIMEXPR);
        a.put("optDims", NONT_OPTDIMS);
        a.put("dims", NONT_DIMS);
        a.put("fieldAccess", NONT_FIELDACCESS);
        a.put("methodInvocation", NONT_METHODINVOCATION);
        a.put("arrayAccess", NONT_ARRAYACCESS);
        a.put("postfixExpression", NONT_POSTFIXEXPRESSION);
        a.put("unaryExpression", NONT_UNARYEXPRESSION);
        a.put("unaryExpressionNotPlusMinus", NONT_UNARYEXPRESSIONNOTPLUSMINUS);
        a.put("castExpression", NONT_CASTEXPRESSION);
        a.put("multiplicativeExpression", NONT_MULTIPLICATIVEEXPRESSION);
        a.put("additiveExpression", NONT_ADDITIVEEXPRESSION);
        a.put("relationalExpression", NONT_RELATIONALEXPRESSION);
        a.put("equalityExpression", NONT_EQUALITYEXPRESSION);
        a.put("andExpression", NONT_ANDEXPRESSION);
        a.put("inclusiveOrExpression", NONT_INCLUSIVEOREXPRESSION);
        a.put("conditionalAndExpression", NONT_CONDITIONALANDEXPRESSION);
        a.put("conditionalOrExpression", NONT_CONDITIONALOREXPRESSION);
        a.put("assignmentExpression", NONT_ASSIGNMENTEXPRESSION);
        a.put("assignment", NONT_ASSIGNMENT);
        a.put("leftHandSide", NONT_LEFTHANDSIDE);
        a.put("expression", NONT_EXPRESSION);
        a.put("constantExpression", NONT_CONSTANTEXPRESSION);

        a.put("shift", SHIFT);
        a.put("reduce", REDUCE);

        Map<Integer, String> b = idToSymbolName;
        b.put(TERM_EOF, "EOF");
        b.put(TERM_BOF, "BOF");
        b.put(TERM_ID, "ID");
        b.put(TERM_PLUS, "PLUS");
        b.put(TERM_MINUS, "MINUS");
        b.put(TERM_TIMES, "TIMES");
        b.put(TERM_DIVIDE, "DIVIDE");
        b.put(TERM_MOD, "MOD");
        b.put(TERM_ANDS, "ANDS");
        b.put(TERM_ANDL, "ANDL");
        b.put(TERM_ORS, "ORS");
        b.put(TERM_ORL, "ORL");
        b.put(TERM_NOT, "NOT");
        b.put(TERM_EQUALS, "EQUALS");
        b.put(TERM_NOTEQUALS, "NOTEQUALS");
        b.put(TERM_LTEQ, "LTEQ");
        b.put(TERM_GTEQ, "GTEQ");
        b.put(TERM_LTTHAN, "LTTHAN");
        b.put(TERM_GTTHAN, "GTTHAN");
        b.put(TERM_ASSIGN, "ASSIGN");
        b.put(TERM_LPAREN, "LPAREN");
        b.put(TERM_RPAREN, "RPAREN");
        b.put(TERM_LBRACE, "LBRACE");
        b.put(TERM_RBRACE, "RBRACE");
        b.put(TERM_LBRACKET, "LBRACKET");
        b.put(TERM_RBRACKET, "RBRACKET");
        b.put(TERM_INTLIT, "INTLIT");
        b.put(TERM_STRINGLIT, "STRINGLIT");
        b.put(TERM_CHARLIT, "CHARLIT");
        b.put(TERM_INT, "INT");
        b.put(TERM_CHAR, "CHAR");
        b.put(TERM_SHORT, "SHORT");
        b.put(TERM_BOOLEAN, "BOOLEAN");
        b.put(TERM_BYTE, "BYTE");
        b.put(TERM_PUBLIC, "PUBLIC");
        b.put(TERM_PROTECTED, "PROTECTED");
        b.put(TERM_NATIVE, "NATIVE");
        b.put(TERM_ABSTRACT, "ABSTRACT");
        b.put(TERM_FINAL, "FINAL");
        b.put(TERM_CONST, "CONST");
        b.put(TERM_STATIC, "STATIC");
        b.put(TERM_RETURN, "RETURN");
        b.put(TERM_VOID, "VOID");
        b.put(TERM_NULL, "NULL");
        b.put(TERM_NEW, "NEW");
        b.put(TERM_IF, "IF");
        b.put(TERM_ELSE, "ELSE");
        b.put(TERM_FOR, "FOR");
        b.put(TERM_WHILE, "WHILE");
        b.put(TERM_TRUE, "TRUE");
        b.put(TERM_FALSE, "FALSE");
        b.put(TERM_CLASS, "CLASS");
        b.put(TERM_EXTENDS, "EXTENDS");
        b.put(TERM_IMPLEMENTS, "IMPLEMENTS");
        b.put(TERM_IMPORT, "IMPORT");
        b.put(TERM_INSTANCEOF, "INSTANCEOF");
        b.put(TERM_INTERFACE, "INTERFACE");
        b.put(TERM_PACKAGE, "PACKAGE");
        b.put(TERM_DOT, "DOT");
        b.put(TERM_COMMA, "COMMA");
        b.put(TERM_SEMI, "SEMI");
        b.put(TERM_THIS, "THIS");

        b.put(NONT_GOAL, "goal");
        b.put(NONT_LITERAL, "literal");
        b.put(NONT_TYPE, "type");
        b.put(NONT_PRIMITIVETYPE, "primitiveType");
        b.put(NONT_NUMERICTYPE, "numericType");
        b.put(NONT_REFERENCETYPE, "referenceType");
        b.put(NONT_CLASSORINTERFACETYPE, "classOrInterfaceType");
        b.put(NONT_CLASSTYPE, "classType");
        b.put(NONT_INTERFACETYPE, "interfaceType");
        b.put(NONT_ARRAYTYPE, "arrayType");
        b.put(NONT_NAME, "name");
        b.put(NONT_SIMPLENAME, "simpleName");
        b.put(NONT_QUALIFIEDNAME, "qualifiedName");
        b.put(NONT_COMPILATIONUNIT, "compilationUnit");
        b.put(NONT_OPTPACKAGEDECLARATION, "optPackageDeclaration");
        b.put(NONT_OPTIMPORTDECLARATIONS, "optImportDeclarations");
        b.put(NONT_OPTSEMICOLONS, "optSemicolons");
        b.put(NONT_IMPORTDECLARATIONS, "importDeclarations");
        b.put(NONT_IMPORTDECLARATION, "importDeclaration");
        b.put(NONT_SINGLETYPEIMPORTDECLARATION, "singleTypeImportDeclaration");
        b.put(NONT_TYPEIMPORTONDEMANDDECLARATION, "typeImportOnDemandDeclaration");
        b.put(NONT_TYPEDECLARATION, "typeDeclaration");
        b.put(NONT_MODIFIERS, "modifiers");
        b.put(NONT_MODIFIER, "modifier");
        b.put(NONT_OPTMODIFIERS, "optModifiers");
        b.put(NONT_CLASSDECLARATION, "classDeclaration");
        b.put(NONT_OPTSUPER, "optSuper");
        b.put(NONT_OPTINTERFACES, "optInterfaces");
        b.put(NONT_INTERFACETYPELIST, "interfaceTypeList");
        b.put(NONT_CLASSBODY, "classBody");
        b.put(NONT_OPTCLASSBODYDECLARATIONS, "optClassBodyDeclarations");
        b.put(NONT_CLASSBODYDECLARATIONS, "classBodyDeclarations");
        b.put(NONT_CLASSBODYDECLARATION, "classBodyDeclaration");
        b.put(NONT_CLASSMEMBERDECLARATION, "classMemberDeclaration");
        b.put(NONT_FIELDDECLARATION, "fieldDeclaration");
        b.put(NONT_VARIABLEDECLARATOR, "variableDeclarator");
        b.put(NONT_VARIABLEDECLARATORID, "variableDeclaratorId");
        b.put(NONT_METHODDECLARATION, "methodDeclaration");
        b.put(NONT_METHODHEADER, "methodHeader");
        b.put(NONT_METHODDECLARATOR, "methodDeclarator");
        b.put(NONT_OPTFORMALPARAMETERLIST, "optFormalParameterList");
        b.put(NONT_FORMALPARAMETERLIST, "formalParameterList");
        b.put(NONT_FORMALPARAMETER, "formalParameter");
        b.put(NONT_METHODBODY, "methodBody");
        b.put(NONT_CONSTRUCTORDECLARATION, "constructorDeclaration");
        b.put(NONT_CONSTRUCTORDECLARATOR, "constructorDeclarator");
        b.put(NONT_INTERFACEDECLARATION, "interfaceDeclaration");
        b.put(NONT_OPTEXTENDSINTERFACES, "optExtendsInterfaces");
        b.put(NONT_EXTENDSINTERFACES, "extendsInterfaces");
        b.put(NONT_INTERFACEBODY, "interfaceBody");
        b.put(NONT_OPTINTERFACEMEMBERDECLARATIONS, "optInterfaceMemberDeclarations");
        b.put(NONT_INTERFACEMEMBERDECLARATIONS, "interfaceMemberDeclarations");
        b.put(NONT_ABSTRACTMETHODDECLARATION, "abstractMethodDeclaration");
        b.put(NONT_BLOCK, "block");
        b.put(NONT_OPTBLOCKSTATEMENTS, "optBlockStatements");
        b.put(NONT_BLOCKSTATEMENTS, "blockStatements");
        b.put(NONT_BLOCKSTATEMENT, "blockStatement");
        b.put(NONT_LOCALVARIABLEDECLARATIONSTATEMENT, "localVariableDeclarationStatement");
        b.put(NONT_LOCALVARIABLEDECLARATION, "localVariableDeclaration");
        b.put(NONT_STATEMENT, "statement");
        b.put(NONT_STATEMENTNOSHORTIF, "statementNoShortIf");
        b.put(NONT_STATEMENTWITHOUTTRAILINGSUBSTATEMENT, "statementWithoutTrailingSubstatement");
        b.put(NONT_EMPTYSTATEMENT, "emptyStatement");
        b.put(NONT_EXPRESSIONSTATEMENT, "expressionStatement");
        b.put(NONT_STATEMENTEXPRESSION, "statementExpression");
        b.put(NONT_IFTHENSTATEMENT, "ifThenStatement");
        b.put(NONT_IFTHENELSESTATEMENT, "ifThenElseStatement");
        b.put(NONT_IFTHENELSESTATEMENTNOSHORTIF, "ifThenElseStatementNoShortIf");
        b.put(NONT_WHILESTATEMENT, "whileStatement");
        b.put(NONT_WHILESTATEMENTNOSHORTIF, "whileStatementNoShortIf");
        b.put(NONT_FORSTATEMENT, "forStatement");
        b.put(NONT_OPTFORINIT, "optForInit");
        b.put(NONT_OPTEXPRESSION, "optExpression");
        b.put(NONT_OPTFORUPDATE, "optForUpdate");
        b.put(NONT_FORSTATEMENTNOSHORTIF, "forStatementNoShortIf");
        b.put(NONT_FORINIT, "forInit");
        b.put(NONT_FORUPDATE, "forUpdate");
        b.put(NONT_RETURNSTATEMENT, "returnStatement");
        b.put(NONT_PRIMARY, "primary");
        b.put(NONT_PRIMARYNONEWARRAY, "primaryNoNewArray");
        b.put(NONT_CLASSINSTANCECREATIONEXPRESSION, "classInstanceCreationExpression");
        b.put(NONT_OPTARGUMENTLIST, "optArgumentList");
        b.put(NONT_ARGUMENTLIST, "argumentList");
        b.put(NONT_ARRAYCREATIONEXPRESSION, "arrayCreationExpression");
        b.put(NONT_DIMEXPR, "dimExpr");
        b.put(NONT_OPTDIMS, "optDims");
        b.put(NONT_DIMS, "dims");
        b.put(NONT_FIELDACCESS, "fieldAccess");
        b.put(NONT_METHODINVOCATION, "methodInvocation");
        b.put(NONT_ARRAYACCESS, "arrayAccess");
        b.put(NONT_POSTFIXEXPRESSION, "postfixExpression");
        b.put(NONT_UNARYEXPRESSION, "unaryExpression");
        b.put(NONT_UNARYEXPRESSIONNOTPLUSMINUS, "unaryExpressionNotPlusMinus");
        b.put(NONT_CASTEXPRESSION, "castExpression");
        b.put(NONT_MULTIPLICATIVEEXPRESSION, "multiplicativeExpression");
        b.put(NONT_ADDITIVEEXPRESSION, "additiveExpression");
        b.put(NONT_RELATIONALEXPRESSION, "relationalExpression");
        b.put(NONT_EQUALITYEXPRESSION, "equalityExpression");
        b.put(NONT_ANDEXPRESSION, "andExpression");
        b.put(NONT_INCLUSIVEOREXPRESSION, "inclusiveOrExpression");
        b.put(NONT_CONDITIONALANDEXPRESSION, "conditionalAndExpression");
        b.put(NONT_CONDITIONALOREXPRESSION, "conditionalOrExpression");
        b.put(NONT_ASSIGNMENTEXPRESSION, "assignmentExpression");
        b.put(NONT_ASSIGNMENT, "assignment");
        b.put(NONT_LEFTHANDSIDE, "leftHandSide");
        b.put(NONT_EXPRESSION, "expression");
        b.put(NONT_CONSTANTEXPRESSION, "constantExpression");
    }

    private boolean isTerm(String tok) {
        char c = tok.charAt(0);
        return c >= 'A' && c <= 'Z';
    }

    public static class LrProduction {
        int lhs;
        int[] rhs;

        public LrProduction(int lhs, int[] rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }
}
