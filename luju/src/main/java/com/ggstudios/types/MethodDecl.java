package com.ggstudios.types;

import com.ggstudios.luju.Token;
import com.ggstudios.utils.PrintUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodDecl extends AstNode {
    private Set<Token.Type> modifiers = new HashSet<>();
    private UserType returnType;
    private Token methodName;
    private List<FieldDecl> arguments = new ArrayList<>();
    private Block block;

    public Set<Token.Type> getModifiers() {
        return modifiers;
    }

    public void addModifier(Token.Type mod) {
        this.modifiers.add(mod);
    }

    public void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append(getClass().getSimpleName());
        sb.append(" (ReturnType: ");
        sb.append(returnType);
        sb.append("; MethodName: ");
        sb.append(methodName.getRaw());
        sb.append("; Args: [");
        if (!arguments.isEmpty()) {
            for (FieldDecl arg : arguments) {
                arg.toPrettyString(sb, 0);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append("])");

        if (block != null)
            block.toPrettyString(sb, level + 1);
    }

    public UserType getReturnType() {
        return returnType;
    }

    public void setReturnType(UserType returnType) {
        this.returnType = returnType;
    }

    public Token getMethodName() {
        return methodName;
    }

    public void setName(Token methodName) {
        this.methodName = methodName;
    }

    public List<FieldDecl> getArguments() {
        return arguments;
    }

    public void addArgument(FieldDecl argument) {
        this.arguments.add(argument);
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }
}
