package com.ggstudios.types;

public class CastExpression extends UnaryExpression {
    private ReferenceType cast;

    public ReferenceType getCast() {
        return cast;
    }

    public void setCast(ReferenceType cast) {
        this.cast = cast;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(cast.toString());
        sb.append(") ");
        sb.append(getExpression().toString());
        return sb.toString();
    }
}
