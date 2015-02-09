package com.ggstudios.utils;

public class PrintUtils {
    private static final String TAB = "  ";

    public static final void level(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append(TAB);
        }
    }
}
