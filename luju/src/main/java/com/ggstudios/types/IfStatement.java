package com.ggstudios.types;

import com.ggstudios.utils.PrintUtils;

import java.util.ArrayList;
import java.util.List;

public class IfStatement extends Statement {
    private List<IfBlock> ifBlocks = new ArrayList<>();
    private ElseBlock elseBlock;

    public IfStatement() {
        super(Statement.TYPE_IF);
    }

    public void addIfBlock(Expression expr, Statement st) {
        ifBlocks.add(new IfBlock(expr, st));
    }

    public void addIfBlock(IfBlock block) {
        ifBlocks.add(block);
    }

    public void setIfBlocks(List<IfBlock> blocks) {
        ifBlocks = blocks;
    }

    public List<IfBlock> getIfBlocks() {
        return ifBlocks;
    }

    public void setElseBlock(ElseBlock b) {
        elseBlock = b;
    }

    public ElseBlock getElseBlock() {
        return elseBlock;
    }

    @Override
    public void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append(getClass().getSimpleName());
        sb.append(" [");

        for (IfBlock b : ifBlocks) {
            sb.append('\n');
            PrintUtils.level(sb, level + 1);
            sb.append("if ");
            sb.append(b.condition.toString());
            sb.append('\n');
            b.body.toPrettyString(sb, level + 2);
        }

        if (elseBlock != null) {
            sb.append('\n');
            PrintUtils.level(sb, level + 1);
            sb.append("else ");
            sb.append('\n');
            elseBlock.toPrettyString(sb, level + 2);
        }

        sb.append('\n');
        PrintUtils.level(sb, level);
        sb.append("]");
    }

    public static class IfBlock extends Statement {
        private Expression condition;
        private Statement body;

        public IfBlock(Expression condition, Statement body) {
            super(Statement.TYPE_IF_BLOCK);
            this.condition = condition;
            this.body = body;
        }

        public Expression getCondition() {
            return condition;
        }

        public Statement getBody() {
            return body;
        }
    }

    public static class ElseBlock extends Block {
        public ElseBlock(Statement elseStatement) {
            super();
            setPos(elseStatement);
            addStatement(elseStatement);
        }
    }
}
