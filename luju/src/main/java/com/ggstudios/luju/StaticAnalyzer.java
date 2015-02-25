package com.ggstudios.luju;

import com.ggstudios.env.*;
import com.ggstudios.env.Class;
import com.ggstudios.error.InconvertibleTypeException;
import com.ggstudios.error.StaticAnalyzerException;
import com.ggstudios.error.TypeException;
import com.ggstudios.types.BinaryExpression;
import com.ggstudios.types.Block;
import com.ggstudios.types.Expression;
import com.ggstudios.types.ForStatement;
import com.ggstudios.types.IfStatement;
import com.ggstudios.types.LiteralExpression;
import com.ggstudios.types.ReturnStatement;
import com.ggstudios.types.Statement;
import com.ggstudios.types.WhileStatement;

import java.util.List;
import java.util.Stack;

public class StaticAnalyzer {
    private static final String UNREACHABLE_STATEMENT = "Unreachable statement";

    private Stack<Boolean> stateStack = new Stack<>();

    private boolean completedNormally;

    public boolean isReturnOnAllCodePath() {
        return returnOnAllCodePath;
    }

    public void setReturnOnAllCodePath(boolean returnOnAllCodePath) {
        this.returnOnAllCodePath = returnOnAllCodePath;
    }

    private boolean returnOnAllCodePath;

    public boolean isCompletedNormally() {
        return completedNormally;
    }

    public void resetState() {
        completedNormally = true;
        returnOnAllCodePath = false;
    }

    public void pushState() {
        stateStack.push(completedNormally);
        stateStack.push(returnOnAllCodePath);
    }

    public void pushAndReset() {
        pushState();
        resetState();
    }

    public void popState() {
        returnOnAllCodePath = stateStack.pop();
        completedNormally = stateStack.pop();
    }

    public void analyzeReachability(Statement s) {
        if (!completedNormally) {
            throw new StaticAnalyzerException(s, UNREACHABLE_STATEMENT);
        }
    }

    public void analyzeReachability(Block b) {
        if (!completedNormally) {
            List<Statement> s = b.getStatements();
            if (s.size() == 0) {
                throw new StaticAnalyzerException(b, UNREACHABLE_STATEMENT);
            } else {
                throw new StaticAnalyzerException(s.get(0), UNREACHABLE_STATEMENT);
            }
        }
    }

    public void analyzeReachability(ForStatement s) {
        analyzeReachability((Statement)s);
        s.setCondition(foldConstants(s.getCondition()));
        Expression cond = s.getCondition();

        if (cond.getExpressionType() == Expression.LITERAL_EXPRESSION) {
            LiteralExpression lit = (LiteralExpression) cond;
            if (lit.getLiteral().getType() == Token.Type.TRUE) {
                completedNormally = false;
            } else if (lit.getLiteral().getType() == Token.Type.FALSE) {
                throw new StaticAnalyzerException(s.getBody(), UNREACHABLE_STATEMENT);
            }
        }
    }

    public void analyzeReachability(WhileStatement s) {
        analyzeReachability((Statement)s);
        s.setCondition(foldConstants(s.getCondition()));
        Expression cond = s.getCondition();

        if (cond.getExpressionType() == Expression.LITERAL_EXPRESSION) {
            LiteralExpression lit = (LiteralExpression) cond;
            if (lit.getLiteral().getType() == Token.Type.TRUE) {
                completedNormally = false;
            } else if (lit.getLiteral().getType() == Token.Type.FALSE) {
                throw new StaticAnalyzerException(s.getBody(), UNREACHABLE_STATEMENT);
            }
        }
    }

    public void analyzeReachability(ReturnStatement s) {
        analyzeReachability((Statement)s);

        completedNormally = false;
        returnOnAllCodePath = true;
    }

    public void setCompletedNormally(boolean completedNormally) {
        this.completedNormally = completedNormally;
    }

    public int a() {
        while (true & true) {

        }
    }

    private Expression foldConstants(Expression cond) {
        switch (cond.getExpressionType()) {
            case Expression.BINARY_EXPRESSION: {
                BinaryExpression binEx = (BinaryExpression) cond;
                Expression le = binEx.getLeftExpr();
                Expression re = binEx.getRightExpr();

                le = foldConstants(le);
                re = foldConstants(re);

                if (le.getExpressionType() == re.getExpressionType() && le.getExpressionType() == Expression.LITERAL_EXPRESSION) {
                    Token l = ((LiteralExpression) le).getLiteral();
                    Token r = ((LiteralExpression) re).getLiteral();

                    switch (binEx.getOp().getType()) {
                        case LT: {
                            Token t;
                            if (l.getVal() < r.getVal()) {
                                t = new Token(Token.Type.TRUE, "", 0, 0);
                            } else {
                                t = new Token(Token.Type.FALSE, "", 0, 0);
                            }
                            return new LiteralExpression(t);
                        }
                        case LT_EQ: {
                            Token t;
                            if (l.getVal() <= r.getVal()) {
                                t = new Token(Token.Type.TRUE, "", 0, 0);
                            } else {
                                t = new Token(Token.Type.FALSE, "", 0, 0);
                            }
                            return new LiteralExpression(t);
                        }
                        case GT: {
                            Token t;
                            if (l.getVal() > r.getVal()) {
                                t = new Token(Token.Type.TRUE, "", 0, 0);
                            } else {
                                t = new Token(Token.Type.FALSE, "", 0, 0);
                            }
                            return new LiteralExpression(t);
                        }
                        case GT_EQ: {
                            Token t;
                            if (l.getVal() >= r.getVal()) {
                                t = new Token(Token.Type.TRUE, "", 0, 0);
                            } else {
                                t = new Token(Token.Type.FALSE, "", 0, 0);
                            }
                            return new LiteralExpression(t);
                        }
                        case PIPE:
                        case PIPE_PIPE: {
                            Token t;
                            if (l.getType() == Token.Type.TRUE || r.getType() == Token.Type.TRUE) {
                                t = new Token(Token.Type.TRUE, "", 0, 0);
                            } else {
                                t = new Token(Token.Type.FALSE, "", 0, 0);
                            }
                            return new LiteralExpression(t);
                        }
                        case AMP:
                        case AMP_AMP: {
                            Token t;
                            if (l.getType() == Token.Type.TRUE && r.getType() == Token.Type.TRUE) {
                                t = new Token(Token.Type.TRUE, "", 0, 0);
                            } else {
                                t = new Token(Token.Type.FALSE, "", 0, 0);
                            }
                            return new LiteralExpression(t);
                        }
                        case EQ_EQ:
                            if (l.getType() == Token.Type.INTLIT) {
                                Token t;
                                if (l.getVal() == r.getVal()) {
                                    t = new Token(Token.Type.TRUE, "", 0, 0);
                                } else {
                                    t = new Token(Token.Type.FALSE, "", 0, 0);
                                }
                                return new LiteralExpression(t);
                            } else if (l.getType() == Token.Type.FALSE || l.getType() == Token.Type.TRUE) {
                                Token t;
                                if (l.getType() == r.getType()) {
                                    t = new Token(Token.Type.TRUE, "", 0, 0);
                                } else {
                                    t = new Token(Token.Type.FALSE, "", 0, 0);
                                }
                                return new LiteralExpression(t);
                            }
                            return binEx;
                        case NEQ:
                            if (l.getType() == Token.Type.INTLIT) {
                                Token t;
                                if (l.getVal() != r.getVal()) {
                                    t = new Token(Token.Type.TRUE, "", 0, 0);
                                } else {
                                    t = new Token(Token.Type.FALSE, "", 0, 0);
                                }
                                return new LiteralExpression(t);
                            } else if (l.getType() == Token.Type.FALSE || l.getType() == Token.Type.TRUE) {
                                Token t;
                                if (l.getType() != r.getType()) {
                                    t = new Token(Token.Type.TRUE, "", 0, 0);
                                } else {
                                    t = new Token(Token.Type.FALSE, "", 0, 0);
                                }
                                return new LiteralExpression(t);
                            }
                            return binEx;
                        case PLUS:
                            if (l.getType() == Token.Type.INTLIT &&
                                    r.getType() == Token.Type.INTLIT) {
                                Token t = new Token(Token.Type.INTLIT, "", 0, 0);
                                t.setVal(l.getVal() + r.getVal());
                                return new LiteralExpression(t);
                            }
                            return binEx;
                        case MINUS:
                            if (l.getType() == Token.Type.INTLIT &&
                                    r.getType() == Token.Type.INTLIT) {
                                Token t = new Token(Token.Type.INTLIT, "", 0, 0);
                                t.setVal(l.getVal() - r.getVal());
                                return new LiteralExpression(t);
                            }
                            return binEx;
                        case STAR:
                            if (l.getType() == Token.Type.INTLIT &&
                                    r.getType() == Token.Type.INTLIT) {
                                Token t = new Token(Token.Type.INTLIT, "", 0, 0);
                                t.setVal(l.getVal() * r.getVal());
                                return new LiteralExpression(t);
                            }
                            return binEx;
                        case MOD:
                            if (l.getType() == Token.Type.INTLIT &&
                                    r.getType() == Token.Type.INTLIT) {
                                Token t = new Token(Token.Type.INTLIT, "", 0, 0);
                                t.setVal(l.getVal() % r.getVal());
                                return new LiteralExpression(t);
                            }
                            return binEx;
                        case FSLASH:
                            if (l.getType() == Token.Type.INTLIT &&
                                    r.getType() == Token.Type.INTLIT) {
                                Token t = new Token(Token.Type.INTLIT, "", 0, 0);
                                t.setVal(l.getVal() / r.getVal());
                                return new LiteralExpression(t);
                            }
                            return binEx;
                        default:
                            return binEx;
                    }
                }
            }
        }

        return cond;
    }
}
