package com.ggstudios.luju;

import com.ggstudios.utils.PrintUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Ast {
    public static final String PACKAGE_UNNAMED = "@default";

    private List<FileNode> nodes = new ArrayList<>();

    public String toPrettyString() {
        return toPrettyString(new StringBuilder(), 0, null);
    }

    public String toPrettyString(Set<String> filter) {
        return toPrettyString(new StringBuilder(), 0, filter);
    }

    private String toPrettyString(StringBuilder sb, int level, Set<String> filter) {
        PrintUtils.level(sb, level);
        sb.append("Ast (");
        sb.append(nodes.size());
        sb.append(")\n");

        if (!nodes.isEmpty()) {
            for (FileNode fn : nodes) {
                if (filter != null && filter.contains(fn.getPackageName())) continue;
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

    public int size() {
        return nodes.size();
    }

    public FileNode get(int index) {
        return nodes.get(index);
    }
}
