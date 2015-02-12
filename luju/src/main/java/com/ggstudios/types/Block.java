package com.ggstudios.types;

import com.ggstudios.utils.PrintUtils;

import java.util.ArrayList;
import java.util.List;

public class Block extends Statement {
    private List<Statement> statements = new ArrayList<>();

    public void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);

        sb.append(getClass().getSimpleName());
        sb.append(" (");
        sb.append(")\n");

        for (Statement s : statements) {
            s.toPrettyString(sb, level + 1);
            sb.append('\n');
        }

        sb.setLength(sb.length() - 1);
    }

    public List<Statement> getLocalDeclarations() {
        return statements;
    }

    public void addStatement(Statement s) {
        this.statements.add(s);
    }
}
