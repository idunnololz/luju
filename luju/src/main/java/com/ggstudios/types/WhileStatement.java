package com.ggstudios.types;

import com.ggstudios.utils.Print;
import com.ggstudios.utils.PrintUtils;

public class WhileStatement extends Statement {
    private Expression condition;
    private Statement body;

    public WhileStatement() {
        super(Statement.TYPE_WHILE);
    }

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public Statement getBody() {
        return body;
    }

    public void setBody(Statement body) {
        this.body = body;
    }

    @Override
    public void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append(getClass().getSimpleName());
        sb.append(" (");
        sb.append(condition.toString());
        sb.append(")\n");
        body.toPrettyString(sb, level + 1);
    }
}
