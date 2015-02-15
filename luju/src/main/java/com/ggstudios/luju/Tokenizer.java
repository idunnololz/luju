package com.ggstudios.luju;

import com.ggstudios.error.TokenException;
import com.ggstudios.utils.Print;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Tokenizer {
    private String fileName;
    private BufferedReader in;
    private int row, col;

    private StringBuilder raw = new StringBuilder();
    private List<Token> toks;

    private boolean inMlComment = false;

    private static final Map<Character, Character> escapeChar = new HashMap<>();
    private static final Set<Character> termChar = new HashSet<>();
    private static final Map<Character, Token.Type> charToState = new HashMap<>();
    static {
        termChar.add('|');
        termChar.add('&');
        termChar.add('<');
        termChar.add('>');
        termChar.add('=');
        termChar.add('!');
        termChar.add('+');
        termChar.add('-');
        termChar.add('*');
        termChar.add('/');
        termChar.add('%');
        termChar.add('{');
        termChar.add('}');
        termChar.add('[');
        termChar.add(']');
        termChar.add('(');
        termChar.add(')');
        termChar.add(';');
        termChar.add(',');
        termChar.add('.');
        termChar.add('\n');

        Map<Character, Token.Type> m = charToState;
        m.put('.', Token.Type.DOT);
        m.put('(', Token.Type.LPAREN);
        m.put(')', Token.Type.RPAREN);
        m.put('[', Token.Type.LBRACK);
        m.put(']', Token.Type.RBRACK);
        m.put('{', Token.Type.LBRACE);
        m.put('}', Token.Type.RBRACE);
        m.put(',', Token.Type.COMMA);
        m.put(';', Token.Type.SEMI);
        m.put('+', Token.Type.PLUS);
        m.put('-', Token.Type.MINUS);
        m.put('*', Token.Type.STAR);
        m.put('%', Token.Type.MOD);

        escapeChar.put('b', '\b');
        escapeChar.put('t', '\t');
        escapeChar.put('n', '\n');
        escapeChar.put('f', '\f');
        escapeChar.put('r', '\r');
        escapeChar.put('"', '"');
        escapeChar.put('\'', '\'');
        escapeChar.put('\\', '\\');
    }


    private static final Keyword[] keywords = new Keyword[] {
            new Keyword("abstract",     Token.Type.ABSTRACT),
            new Keyword("boolean",      Token.Type.BOOLEAN),
            new Keyword("break",        Token.Type.INVALID),
            new Keyword("byte",         Token.Type.BYTE),
            new Keyword("char",         Token.Type.CHAR),
            new Keyword("case",         Token.Type.INVALID),
            new Keyword("catch",        Token.Type.INVALID),
            new Keyword("char",         Token.Type.INVALID),
            new Keyword("class",        Token.Type.CLASS),
            new Keyword("const",        Token.Type.INVALID),
            new Keyword("continue",     Token.Type.INVALID),
            new Keyword("default",      Token.Type.INVALID),
            new Keyword("do",           Token.Type.INVALID),
            new Keyword("double",       Token.Type.INVALID),
            new Keyword("else",         Token.Type.ELSE),
            new Keyword("extends",      Token.Type.EXTENDS),
            new Keyword("false",        Token.Type.FALSE),
            new Keyword("final",        Token.Type.FINAL),
            new Keyword("finally",      Token.Type.INVALID),
            new Keyword("float",        Token.Type.INVALID),
            new Keyword("for",          Token.Type.FOR),
            new Keyword("goto",         Token.Type.INVALID),
            new Keyword("if",           Token.Type.IF),
            new Keyword("implements",   Token.Type.IMPLEMENTS),
            new Keyword("import",       Token.Type.IMPORT),
            new Keyword("instanceof",   Token.Type.INSTANCEOF),
            new Keyword("int",          Token.Type.INT),
            new Keyword("interface",    Token.Type.INTERFACE),
            new Keyword("long",         Token.Type.INVALID),
            new Keyword("native",       Token.Type.NATIVE),
            new Keyword("new",          Token.Type.NEW),
            new Keyword("null",         Token.Type.NULL),
            new Keyword("package",      Token.Type.PACKAGE),
            new Keyword("private",      Token.Type.INVALID),
            new Keyword("protected",    Token.Type.PROTECTED),
            new Keyword("public",       Token.Type.PUBLIC),
            new Keyword("return",       Token.Type.RETURN),
            new Keyword("short",        Token.Type.SHORT),
            new Keyword("static",       Token.Type.STATIC),
            new Keyword("strictfp",     Token.Type.INVALID),
            new Keyword("super",        Token.Type.INVALID),
            new Keyword("switch",       Token.Type.INVALID),
            new Keyword("synchronized", Token.Type.INVALID),
            new Keyword("this",         Token.Type.THIS),
            new Keyword("throw",        Token.Type.INVALID),
            new Keyword("throws",       Token.Type.INVALID),
            new Keyword("transient",    Token.Type.INVALID),
            new Keyword("true",         Token.Type.TRUE),
            new Keyword("try",          Token.Type.INVALID),
            new Keyword("void",         Token.Type.VOID),
            new Keyword("volatile",     Token.Type.INVALID),
            new Keyword("while",        Token.Type.WHILE)
    };

    private void tokenizeLine(String line) {
        col = 0;

        if (inMlComment) {
            processMlComment(line);
            col++;
        }

        final int len = line.length();
        for (; col < len; col++) {
            raw.setLength(0);

            char c = line.charAt(col);
            if (c >= 'a' && c <= 'z') {
                toks.add(tryMatchKeyword(line));
            } else if (c >= 'A' && c <= 'Z') {
                raw.append(c);
                col++;
                toks.add(getId(line));
            } else if (c == '0') {
                if (isTerm(line.charAt(col + 1))) {
                    raw.append(c);
                    toks.add(new Token(Token.Type.INTLIT, raw.toString(), col, row));
                } else {
                    throw new TokenException(fileName, new Token(Token.Type.INVALID, Character.toString(line.charAt(col + 1)), col + 1, row),
                            "Zero proceeded with '" + line.charAt(col + 1) + "'");
                }
            } else if (Character.isDigit(c)) {
                toks.add(getIntLit(line));
            } else if (c == '/') {
                raw.append(c);

                c = line.charAt(col + 1);
                if (c == '*') {
                    // add a comment token just to label where the comment starts in case of error
                    toks.add(new Token(Token.Type.COMMENT, "", col - 1, row));
                    col++;
                    inMlComment = true;
                    processMlComment(line);
                } else if (c == '/') {
                    // single line comment... just skip line...
                    break;
                } else {
                    toks.add(new Token(Token.Type.FSLASH, raw.toString(), col, row));
                }
            } else if (c == '"') {
                // string!
                col++;
                toks.add(getStringLiteral(line));
            } else if (c == '\'') {
                col++;
                toks.add(getCharLiteral(line));
            } else if (charToState.containsKey(c)) {
                raw.append(c);
                Token.Type type = charToState.get(c);
                toks.add(new Token(type, raw.toString(), col, row));
            } else if (c == '&') {
                char next = line.charAt(col + 1);
                if (next == '&') {
                    toks.add(new Token(Token.Type.AMP_AMP, "", col, row));
                    col++;
                } else {
                    toks.add(new Token(Token.Type.AMP, "", col, row));
                }
            } else if (c == '|') {
                char next = line.charAt(col + 1);
                if (next == '|') {
                    toks.add(new Token(Token.Type.PIPE_PIPE, "", col, row));
                    col++;
                } else {
                    toks.add(new Token(Token.Type.PIPE, "", col, row));
                }
            } else if (c == '<') {
                char next = line.charAt(col + 1);
                if (next == '=') {
                    toks.add(new Token(Token.Type.LT_EQ, "", col, row));
                    col++;
                } else {
                    toks.add(new Token(Token.Type.LT, "", col, row));
                }
            } else if (c == '>') {
                char next = line.charAt(col + 1);
                if (next == '=') {
                    toks.add(new Token(Token.Type.GT_EQ, "", col, row));
                    col++;
                } else {
                    toks.add(new Token(Token.Type.GT, "", col, row));
                }
            } else if (c == '=') {
                char next = line.charAt(col + 1);
                if (next == '=') {
                    toks.add(new Token(Token.Type.EQ_EQ, "==", col, row));
                    col++;
                } else {
                    toks.add(new Token(Token.Type.EQ, "=", col, row));
                }
            } else if (c == '!') {
                char next = line.charAt(col + 1);
                if (next == '=') {
                    toks.add(new Token(Token.Type.NEQ, "!=", col, row));
                    col++;
                } else {
                    toks.add(new Token(Token.Type.NOT, "!", col, row));
                }
            } else if (Character.isWhitespace(c)) {
            } else {
                throw new TokenException(fileName, new Token(Token.Type.INVALID, Character.toString(c), col, row),
                        "Invalid token '" + c + "'");
            }

            if (toks.size() != 0 && toks.get(toks.size() - 1).getType() == Token.Type.INVALID) {
                Token t = toks.get(toks.size() - 1);
                throw new TokenException(fileName, t, "Invalid token '" + t.getRaw() + "'");
            }
        }
    }

    private void processMlComment(String line) {
        char c = line.charAt(col);

        while (c != '\n') {
            if (c == '*') {
                c = line.charAt(col + 1);
                if (c == '/') {
                    inMlComment = false;
                    col++;
                    toks.remove(toks.size() - 1);
                    return;
                }
            }
            c = line.charAt(++col);
        }
    }

    private Token tryMatchKeyword(String line) {
        return tryMatchKeyword(line, 0, keywords.length);
    }

    private Token tryMatchKeyword(String line, int s, int e) {
        int index = 0;

        char c, next = line.charAt(col);
        while (true) {
            c = next;
            next = line.charAt(col + 1);

            boolean startFound = false;

            for (int i = s; i < e; i++) {
                Keyword k = keywords[i];

                if (!startFound && c == k.lit.charAt(index)) {
                    if (k.lit.length() == index + 1) {
                        if (isTerm(next)) {
                            return getKeyword(k);
                        }
                    } else {
                        startFound = true;
                        s = i;
                    }
                } else if (startFound && c != k.lit.charAt(index)) {
                    e = i;

                    raw.append(c);
                    index++;
                    col++;
                    break;
                }

                if (startFound && i + 1 == e) {
                    e = i + 1;

                    raw.append(c);
                    index++;
                    col++;
                }
            }

            if (!startFound || next == '\n') {
                break;
            }
        }

        return getId(line);
    }

    private Token getKeyword(Keyword k) {
        return new Token(k.t, k.lit, col - k.lit.length() + 1, row);
    }

    private Token getId(String line) {
        char c;
        boolean fail = false;
        while (!isTerm(c = line.charAt(col++))) {
            raw.append(c);
            if (!isAlphaNum(c) && c != '_') {
                fail = true;
            }
        }
        col -= 2;
        Token t = new Token(Token.Type.ID, raw.toString(), col - raw.length(), row);
        if (fail)
            throw new TokenException(fileName, t, "Invalid identifier: " + t.getRaw());
        return t;
    }

    private Token getIntLit(String line) {
        long val = 0;
        char c;
        char next = line.charAt(col);
        while (!isTerm(next)) {
            c = next;
            next = line.charAt(col + 1);

            if (Character.isDigit(c)) {
                raw.append(c);
                val = val * 10 + (c - '0');

                if (val > 2147483648L) {
                    throw new TokenException(fileName, new Token(Token.Type.INTLIT, raw.toString(), col, row),
                            "Invalid integer literal: " + raw.toString() + "...");
                }
            } else {
                throw new TokenException(fileName, new Token(Token.Type.INTLIT, raw.toString(), col, row),
                        "Integer literal cannot be followed by: " + c);
            }

            col++;
        }

        Token t = new Token(Token.Type.INTLIT, raw.toString(), col - raw.length(), row);
        t.setVal(val);
        col--;
        return t;
    }

    private Token getStringLiteral(String line) {
        char c;
        char next = line.charAt(col);
        while (next != '\n') {
            c = next;
            next = line.charAt(col + 1);
            raw.append(c);

            if (c == '\\') {
                col++;
                tokenizeEscape(line);
                next = line.charAt(col + 1);
            } else if (c == '"') {
                raw.setLength(raw.length() - 1);
                return new Token(Token.Type.STRINGLIT, raw.toString(), col - raw.length() - 1, row);
            }

            col++;
        }

        throw new TokenException(fileName, new Token(Token.Type.STRINGLIT, raw.toString(), col - raw.length() - 1, row),
                "Unclosed string literal");
    }

    public Token getCharLiteral(String line) {
        char c = line.charAt(col);
        if (c == '\n' || c == '\'') throw new TokenException(fileName, new Token(Token.Type.CHARLIT, "", col - 1, row), "Invalid character literal");
        char next = line.charAt(col + 1);
        if (next == '\n') throw new TokenException(fileName, new Token(Token.Type.CHARLIT, "", col - 1, row), "Invalid character literal");

        if (c == '\\') {
            raw.append(c);
            col++;
            tokenizeEscape(line);

            next = line.charAt(col + 1);
        } else {
            raw.append(c);
        }

        if (next != '\'') {
            if (next == '\n') {
                throw new TokenException(fileName, new Token(Token.Type.CHARLIT, "", col - 1, row), "Unclosed character literal");
            } else {
                throw new TokenException(fileName, new Token(Token.Type.CHARLIT, "", col - 1, row), "Too many characters in character literal");
            }
        }

        col++;

        return new Token(Token.Type.CHARLIT, raw.toString(), col - raw.length() - 1, row);
    }

    private void tokenizeEscape(String line) {
        char c = line.charAt(col);
        switch (c) {
            case 'b':
            case 't':
            case 'n':
            case 'f':
            case 'r':
            case '"':
            case '\'':
            case '\\':
                raw.setLength(raw.length() - 1);
                raw.append(escapeChar.get(c));
                break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
                int val = c - '0';

                int len = raw.length() - 1;
                c = line.charAt(++col);
                while (c >= '0' && c <= '7') {
                    val = val * 8 + (c - '0');
                    if (val > 255) {
                        break;
                    }
                    c = line.charAt(++col);
                }
                raw.setLength(len);
                raw.append((char) val);

                col--;
                break;
            default:
                throw new TokenException(fileName, new Token(Token.Type.CHARLIT, "", col + 3, row), "Illegal escape character.");
        }
    }

    public List<Token> tokenizeWith(String fileName) {
        this.fileName = fileName;
        toks = new ArrayList<>();

        row = 1;
        col = 0;

        try {
            toks.add(new Token(Token.Type.BOF, "", row, col));
            in = new BufferedReader(new FileReader(fileName));

            String line;
            while ((line = in.readLine()) != null) {
                ensureAscii(line);
                tokenizeLine(line + "\n");
                row++;
            }

            toks.add(new Token(Token.Type.EOF, "", row, col));

            if (inMlComment) {
                Token t = toks.get(toks.size() - 1);
                throw new TokenException(fileName, t, "Unclosed comment");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return toks;
    }

    private void ensureAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (c > 0x7F) {
                throw new TokenException(fileName, new Token(Token.Type.INVALID, "", i, row), "Non ASCII characters are not supported: " + ((char)c));
            }
        }
    }

    private static boolean isTerm(char c) {
        return termChar.contains(c) || Character.isWhitespace(c);
    }

    private static boolean isAlphaNum(char c) {
        return Character.isDigit(c) || Character.isAlphabetic(c);
    }

    private static class Keyword {
        String lit;
        Token.Type t;

        public Keyword(String lit, Token.Type t) {
            this.lit = lit;
            this.t = t;
        }
    }
}