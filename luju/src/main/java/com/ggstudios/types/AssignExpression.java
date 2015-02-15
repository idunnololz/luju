package com.ggstudios.types;

public class AssignExpression extends Expression {
    private Expression lhs;
    private Expression rhs;

    public AssignExpression() {
        setType(ASSIGN_EXPRESSION);
    }

    public Expression getLhs() {
        return lhs;
    }

    public void setLhs(Expression lhs) {
        this.lhs = lhs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lhs.toString());
        sb.append(" = ");
        sb.append(rhs.toString());
        return sb.toString();
    }

    public Expression getRhs() {
        return rhs;
    }

    public void setRhs(Expression rhs) {
        this.rhs = rhs;
    }
}
