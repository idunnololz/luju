package com.ggstudios.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class FileUtils {
    public static List<File> getFilesInDirRecursive(File dir) {
        List<File> files = new ArrayList<>();
        Stack<File> dirs = new Stack<>();
        dirs.push(dir);

        while (!dirs.isEmpty()) {
            File d = dirs.pop();
            for (File f : d.listFiles()) {
                if (f.isFile()) {
                    files.add(f);
                } else {
                    dirs.push(f);
                }
            }
        }

        return files;
    }
}
