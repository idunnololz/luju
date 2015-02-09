package com.ggstudios.luju;

import com.ggstudios.error.ParseException;
import com.ggstudios.error.WeedException;
import com.ggstudios.utils.ListUtils;
import com.ggstudios.utils.ParserUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class Parser {
    private ParseTable table;

    public Parser() {
        table = new ParseTable();
    }

    public Node parse(FileNode fn) {
        List<Token> tokens = fn.getTokens();
        Node tree = generateSyntaxTree(tokens);
        blazeIt(fn, tree);
        return tree;
    }

    private Node generateSyntaxTree(List<Token> tokens) {
        Stack<Integer> stack = new Stack<>();
        Stack<Node> nodeStack = new Stack<>();
        stack.push(0);

        if (tokens.size() == 0) {
            throw new ParseException("Unexpected end of input.");
        }

        int index = 0;
        for (;;) {
            Token t = tokens.get(index);
            Integer action = table.getLrAction(stack.peek(), t.getType().getValue());
            if (action == null) {
                throw new ParseException(t, String.format("Received invalid token '%s' with type %s.", t.getRaw(), t.getType()));
            } else if (table.isActionShift(action)) {
                if (t.getType() == Token.Type.EOF) {
                    return nodeStack.peek();
                } else {
                    stack.push(table.getActionId(action));
                    nodeStack.push(new Node(t));
                    index++;
                }
            } else if (table.isActionReduce(action)) {
                ParseTable.LrProduction prod = table.getProduction(table.getActionId(action));
                Node n = new Node();
                for (int i = 0; i < prod.rhs.length; i++) {
                    stack.pop();
                    n.children.add(nodeStack.pop());
                }
                Collections.reverse(n.children);

                int gotoIndex = table.getLrGoto(stack.peek(), prod.lhs);
                n.prod = prod;
                stack.push(gotoIndex);
                nodeStack.push(n);
            } else {
                throw new ParseException("Wtf.");
            }
        }
    }

    /**
     * BLAZE IT WEEDER!
     */
    private void blazeIt(FileNode fn, Node tree) {
        Stack<Node> toVisit = new Stack<Node>();
        toVisit.push(tree);

        boolean isClass = false;
        boolean isInterface = false;
        boolean hasConstructor = false;
        Token classTok = null;

        while (!toVisit.isEmpty()) {
            Node cur = toVisit.pop();

            if (cur.t == null) {
                switch (cur.prod.lhs) {
                    case ParseTable.NONT_INTERFACEDECLARATION: {
                        // interfaceDeclaration -> optModifiers INTERFACE ID optExtendsInterfaces interfaceBody
                        isInterface = true;
                        doInterfaceOrClassDeclCheck(fn, cur);
                        break;
                    }
                    case ParseTable.NONT_CLASSDECLARATION: {
                        // classDeclaration -> optModifiers CLASS ID optSuper optInterfaces classBody
                        doInterfaceOrClassDeclCheck(fn, cur);

                        isClass = true;
                        classTok = cur.children.get(2).t;

                        // need to check mods...
                        Node mods = cur.children.get(0);
                        List<Token> toks = ParserUtils.getTokensInTree(mods);

                        // ensure no duplicate mods...
                        Set<Token.Type> set = ensureNoDuplicate(toks);

                        if (set.contains(Token.Type.ABSTRACT) && set.contains(Token.Type.FINAL)) {
                            Token t1 = getTokenWithType(toks, Token.Type.ABSTRACT);
                            Token t2 = getTokenWithType(toks, Token.Type.FINAL);
                            throw new WeedException(t1, String.format("Illegal combination of modifiers: '%s' and '%s'.",
                                    t1.getRaw(), t2.getRaw()));
                        }

                        if (!set.contains(Token.Type.PUBLIC) && !set.contains(Token.Type.PROTECTED)) {
                            Token errTok = cur.children.get(2).t;
                            throw new WeedException(errTok, String.format("Class '%s' must be either public or protected.", errTok.getRaw()));
                        }

                        break;
                    }
                    case ParseTable.NONT_METHODDECLARATION: {
                        // methodDeclaration -> methodHeader methodBody

                        // need to check mods...
                        Node mods = cur.children.get(0).children.get(0);
                        List<Token> toks = ParserUtils.getTokensInTree(mods);

                        // ensure no duplicate mods...
                        Set<Token.Type> set = ensureNoDuplicate(toks);
                        boolean isAbstract = set.contains(Token.Type.ABSTRACT);
                        boolean isNative = set.contains(Token.Type.NATIVE);
                        boolean isStatic = set.contains(Token.Type.STATIC);
                        boolean isFinal = set.contains(Token.Type.FINAL);

                        if (isAbstract || isNative) {
                            // ensure has no body...
                            // methodBody -> SEMI
                            Node methBody = cur.children.get(1);
                            int rhs = methBody.prod.rhs[0];
                            if (rhs != Token.Type.SEMI.getValue()) {
                                List<Token> errToks = ParserUtils.getTokensInTree(methBody);
                                Token t = errToks.get(0);
                                throw new WeedException(t, "Abstract or native methods cannot have bodies.");
                            }
                        }

                        if (isAbstract) {
                            Token errTok = null;
                            if (isStatic) {
                                errTok = getTokenWithType(toks, Token.Type.STATIC);
                            } else if (isFinal) {
                                errTok = getTokenWithType(toks, Token.Type.FINAL);
                            }

                            if (errTok != null) {
                                throw new WeedException(errTok, String.format("Illegal combination of modifiers: 'abstract' and '%s'.",
                                        errTok.getRaw()));
                            }
                        }

                        if (isStatic && isFinal) {
                            Token errTok = getTokenWithType(toks, Token.Type.STATIC);
                            throw new WeedException(errTok, "Illegal combination of modifiers: 'static' and 'final'.");
                        }

                        if (isNative && !isStatic) {
                            Token errTok = getTokenWithType(toks, Token.Type.NATIVE);
                            throw new WeedException(errTok, "A native method must be static.");
                        }

                        if (isInterface) {
                            if (isStatic) {
                                Token errTok = getTokenWithType(toks, Token.Type.STATIC);
                                throw new WeedException(errTok, "Interface method cannot be static.");
                            } else if (isFinal) {
                                Token errTok = getTokenWithType(toks, Token.Type.FINAL);
                                throw new WeedException(errTok, "Interface method cannot be final.");
                            } else if (isNative) {
                                Token errTok = getTokenWithType(toks, Token.Type.NATIVE);
                                throw new WeedException(errTok, "Interface method cannot be native.");
                            }
                        }
                        break;
                    }
                    case ParseTable.NONT_ABSTRACTMETHODDECLARATION: {
                        // abstractMethodDeclaration -> methodHeader SEMI

                        // need to check mods...
                        Node mods = cur.children.get(0).children.get(0);
                        List<Token> toks = ParserUtils.getTokensInTree(mods);

                        // ensure no duplicate mods...
                        Set<Token.Type> set = ensureNoDuplicate(toks);
                        boolean isAbstract = set.contains(Token.Type.ABSTRACT);
                        boolean isNative = set.contains(Token.Type.NATIVE);
                        boolean isStatic = set.contains(Token.Type.STATIC);
                        boolean isFinal = set.contains(Token.Type.FINAL);

                        if (isInterface) {
                            if (isStatic) {
                                Token errTok = getTokenWithType(toks, Token.Type.STATIC);
                                throw new WeedException(errTok, "Interface method cannot be static.");
                            } else if (isFinal) {
                                Token errTok = getTokenWithType(toks, Token.Type.FINAL);
                                throw new WeedException(errTok, "Interface method cannot be final.");
                            } else if (isNative) {
                                Token errTok = getTokenWithType(toks, Token.Type.NATIVE);
                                throw new WeedException(errTok, "Interface method cannot be native.");
                            }
                        }
                        break;
                    }
                    case ParseTable.NONT_CONSTRUCTORDECLARATION: {
                        // constructorDeclaration -> optModifiers constructorDeclarator block
                        hasConstructor = true;
                        break;
                    }
                    case ParseTable.NONT_CONSTRUCTORDECLARATOR: {
                        // constructorDeclarator -> simpleName LPAREN optFormalParameterList RPAREN
                        Node mods = cur.children.get(0);
                        List<Token> toks = ParserUtils.getTokensInTree(mods);
                        if (!toks.get(0).getRaw().equals(fn.getClassName())) {
                            throw new WeedException(toks.get(0), String.format("Invalid constructor name '%s'.", toks.get(0).getRaw()));
                        }
                        break;
                    }
                    case ParseTable.NONT_FIELDDECLARATION: {
                        // fieldDeclaration -> optModifiers type variableDeclarator SEMI

                        // need to check mods...
                        Node mods = cur.children.get(0);
                        List<Token> toks = ParserUtils.getTokensInTree(mods);

                        // ensure no duplicate mods...
                        Set<Token.Type> set = ensureNoDuplicate(toks);

                        if (set.contains(Token.Type.FINAL)) {
                            Token errTok = getTokenWithType(toks, Token.Type.FINAL);
                            throw new WeedException(errTok, String.format("Illegal modifier: '%s'", errTok.getRaw()));
                        } else if (!set.contains(Token.Type.PUBLIC) && !set.contains(Token.Type.PROTECTED)) {
                            Token errTok = ParserUtils.getTokensInTree(cur.children.get(2)).get(0);
                            throw new WeedException(errTok, String.format("Field '%s' must be either public or protected.", errTok.getRaw()));
                        }
                        break;
                    }
                    case ParseTable.NONT_CASTEXPRESSION: {
                        // castExpression -> LPAREN expression RPAREN unaryExpressionNotPlusMinus

                        // ensure expression in cast is a type...
                        if (cur.prod.rhs[1] == ParseTable.NONT_EXPRESSION && !isTypeNode(cur.children.get(1))) {
                            Token errTok = cur.children.get(0).t;
                            throw new WeedException(errTok, "Invalid cast expression.");
                        }
                        break;
                    }
                    default:
                        break;
                }

                for (Node child : ListUtils.reverse(cur.children)) {
                    toVisit.push(child);
                }
            }
        }

        if (isClass && !hasConstructor) {
            Token errTok = classTok;
            throw new WeedException(errTok, String.format("Class '%s' must have at least on constructor.", errTok.getRaw()));
        }
    }

    private boolean isTypeNode(Node node) {
        if (node.t == null) {
            if (node.prod.lhs == ParseTable.NONT_NAME) {
                return true;
            } else if (node.children.size() == 1) {
                return isTypeNode(node.children.get(0));
            }
        }
        return false;
    }

    private void doInterfaceOrClassDeclCheck(FileNode fn, Node tree) {
        // interfaceDeclaration -> optModifiers INTERFACE   ID optExtendsInterfaces interfaceBody
        // classDeclaration     -> optModifiers CLASS       ID optSuper optInterfaces classBody

        String type = tree.prod.lhs == ParseTable.NONT_CLASSDECLARATION ? "Class" : "Interface";

        Token id = tree.children.get(2).t;
        if (!fn.getFileClassName().equals(id.getRaw())) {
            throw new WeedException(id,
                    String.format("%s '%s' should be declared in a file named '%s.java'", type, id.getRaw(), id.getRaw()));
        } else {
            fn.setClassName(id.getRaw());
        }
    }

    private Set<Token.Type> ensureNoDuplicate(List<Token> toks) {
        Set<Token.Type> set = new HashSet<>();
        for (Token each : toks) {
            if (!set.add(each.getType())) {
                Token t = each;
                throw new WeedException(t, String.format("Illegal combination of modifiers: '%s' and '%s'",
                        t.getRaw(), t.getRaw()));
            }
        }
        return set;
    }

    private Token getTokenWithType(List<Token> toks, Token.Type type) {
        for (Token t : toks) {
            if (t.getType() == type) {
                return t;
            }
        }
        return null;
    }

    private static boolean hasDuplicate(Iterable<Token> all) {
        Set<Token.Type> set = new HashSet<Token.Type>();
        for (Token each: all) if (!set.add(each.getType())) return true;
        return false;
    }

    private static Token getDuplicate(Iterable<Token> all) {
        Set<Token.Type> set = new HashSet<Token.Type>();
        for (Token each: all) if (!set.add(each.getType())) return each;
        return null;
    }

    public static class Node {
        public List<Node> children;
        ParseTable.LrProduction prod;
        public Token t;

        Node() {
            children = new ArrayList<>();
        }

        Node(Token t) {
            this.t = t;
        }

        public String toPrettyString() {
            return toPrettyString(0);
        }

        private String toPrettyString(int depth) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                sb.append("  ");
            }

            if (t == null) {
                sb.append("(Node (LrProduction ");
                sb.append(ParseTable.getNameOfId(prod.lhs));
                if (prod.rhs.length != 0) {
                    sb.append(" -> ");
                    for (int i : prod.rhs) {
                        sb.append(ParseTable.getNameOfId(i));
                        sb.append(" ");
                    }
                    sb.setLength(sb.length() - 1);
                }
                sb.append(") [");

                if (children.size() != 0) {
                    sb.append('\n');
                    for (Node n : children) {
                        sb.append(n.toPrettyString(depth + 1));
                        sb.append("\n");
                    }
                    sb.setLength(sb.length() - 1);
                }
                sb.append("])");

            } else {
                sb.append("(Leaf ");
                sb.append(t.toString());
                sb.append(')');
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            if (t == null) {
                StringBuilder sb = new StringBuilder();
                sb.append("(Node (LrProduction ");
                sb.append(ParseTable.getNameOfId(prod.lhs));
                if (prod.rhs.length != 0) {
                    sb.append(" -> ");
                    for (int i : prod.rhs) {
                        sb.append(ParseTable.getNameOfId(i));
                        sb.append(" ");
                    }
                    sb.setLength(sb.length() - 1);
                }
                sb.append(") [");

                if (children.size() != 0) {
                    for (Node n : children) {
                        sb.append(n.toString());
                        sb.append(" ");
                    }
                    sb.setLength(sb.length() - 1);
                }
                sb.append("])");

                return sb.toString();
            } else {
                return "(Leaf " + t.toString() + ")";
            }
        }
    }
}
