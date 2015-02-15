package com.ggstudios.types;

import com.ggstudios.env.Method;
import com.ggstudios.luju.Token;
import com.ggstudios.utils.PrintUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MethodDecl extends AstNode {
    private Set<Token.Type> modifiers = new HashSet<>();
    private ReferenceType returnType;
    private Token methodName;
    private List<VarDecl> arguments = new ArrayList<>();
    private Block block;
    private Method method;

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
            for (VarDecl arg : arguments) {
                arg.toPrettyString(sb, 0);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append("])");

        if (block != null) {
            sb.append("\n");
            block.toPrettyString(sb, level + 1);
        }
    }

    public ReferenceType getReturnType() {
        return returnType;
    }

    public void setReturnType(ReferenceType returnType) {
        this.returnType = returnType;
    }

    public Token getMethodName() {
        return methodName;
    }

    public void setName(Token methodName) {
        this.methodName = methodName;
    }

    public List<VarDecl> getArguments() {
        return arguments;
    }

    public void addArgument(VarDecl argument) {
        this.arguments.add(argument);
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public boolean isAbstract() {
        return false;
    }
}
