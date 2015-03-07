package com.ggstudios.types;

import com.ggstudios.env.*;
import com.ggstudios.env.Class;
import com.ggstudios.error.TypeException;
import com.ggstudios.luju.Token;

public class BinaryExpression extends Expression {
    private Token op;
    private Expression leftExpr;
    private Expression rightExpr;

    public BinaryExpression() {
        setType(BINARY_EXPRESSION);
    }

    public Expression getLeftExpr() {
        return leftExpr;
    }

    public void setLeftExpr(Expression leftExpr) {
        this.leftExpr = leftExpr;
    }

    public Expression getRightExpr() {
        return rightExpr;
    }

    public void setRightExpr(Expression rightExpr) {
        this.rightExpr = rightExpr;
    }

    public Token getOp() {
        return op;
    }

    public void setOp(Token op) {
        this.op = op;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(leftExpr.toString());
        sb.append(' ');
        switch (op.getType()) {
            case LT:
                sb.append("<");
                break;
            case LT_EQ:
                sb.append("<=");
                break;
            case GT:
                sb.append(">");
                break;
            case GT_EQ:
                sb.append(">=");
                break;
            case PIPE:
                sb.append("|");
                break;
            case PIPE_PIPE:
                sb.append("||");
                break;
            case AMP:
                sb.append("&");
                break;
            case AMP_AMP:
                sb.append("&&");
                break;
            case EQ_EQ:
                sb.append("==");
                break;
            case NEQ:
                sb.append("!=");
                break;
            case PLUS:
                sb.append("+");
                break;
            case MINUS:
                sb.append("-");
                break;
            case STAR:
                sb.append("*");
                break;
            case MOD:
                sb.append("%");
                break;
            case FSLASH:
                sb.append("/");
                break;
            case INSTANCEOF:
                sb.append("instanceof");
                break;
            default:
                sb.append(op.getRaw());
        }
        sb.append(' ');
        sb.append(rightExpr.toString());
        sb.append(')');
        return sb.toString();
    }
}
