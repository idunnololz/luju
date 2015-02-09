package com.ggstudios.luju;

import com.ggstudios.utils.PrintUtils;

import java.util.ArrayList;
import java.util.List;

public class Ast {
    public static final String PACKAGE_UNNAMED = "@unnamed";

    private List<FileNode> nodes = new ArrayList<>();

    public String toPrettyString() {
        return toPrettyString(new StringBuilder(), 0);
    }

    private String toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append("Ast (");
        sb.append(nodes.size());
        sb.append(")\n");

        if (!nodes.isEmpty()) {
            for (FileNode fn : nodes) {
                fn.toPrettyString(sb, level);
                sb.append('\n');
            }
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    public void addFileNode(FileNode fn) {
        nodes.add(fn);
    }
}
