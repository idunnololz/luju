package com.ggstudios.types;

import com.ggstudios.luju.Token;
import com.ggstudios.utils.PrintUtils;

public class AstNode {
    private int row, col;

    public void setPos(Token t) {
        setPos(t.getRow(), t.getCol());
    }

    public void setPos(AstNode node) {
        this.row = node.getRow();
        this.col = node.getCol();
    }

    public void setPos(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    protected void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append(getClass().getSimpleName());
    }

    public StringBuilder toPrettyString(StringBuilder sb) {
        toPrettyString(sb, 0);
        return sb;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toPrettyString(sb, 0);
        return sb.toString();
    }
}
