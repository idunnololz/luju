package com.ggstudios.types;

import com.ggstudios.utils.PrintUtils;

public class ForStatement extends Statement {
    private Statement forInit;
    private Expression condition;
    private Statement forUpdate;
    private Statement body;

    public ForStatement() {
        super(Statement.TYPE_FOR);
    }

    public Statement getForInit() {
        return forInit;
    }

    public void setForInit(Statement forInit) {
        this.forInit = forInit;
    }

    public Expression getCondition() {
        return condition;
    }

    public void setCondition(Expression condition) {
        this.condition = condition;
    }

    public Statement getForUpdate() {
        return forUpdate;
    }

    public void setForUpdate(Statement forUpdate) {
        this.forUpdate = forUpdate;
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
        if (forInit != null) {
            sb.append('\n');
            PrintUtils.level(sb, level + 1);
            sb.append("ForInit");
            forInit.toPrettyString(sb, 0);
        }
        if (condition != null) {
            sb.append('\n');
            PrintUtils.level(sb, level + 1);
            sb.append("Condition");
            condition.toPrettyString(sb, 0);
        }
        if (forUpdate != null) {
            sb.append('\n');
            PrintUtils.level(sb, level + 1);
            sb.append("ForUpdate");
            forUpdate.toPrettyString(sb, 0);
        }
        body.toPrettyString(sb, level + 1);
    }
}
