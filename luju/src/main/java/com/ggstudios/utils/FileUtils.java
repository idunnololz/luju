package com.ggstudios.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public static void writeStringsToFiles(String directory, Map<String, String> fileNameToText) {
        for (Map.Entry<String, String> entry : fileNameToText.entrySet()) {
            String fileName = entry.getKey();
            String text = entry.getValue();

            File f = new File(directory + File.separator + fileName);
            PrintWriter pw = null;
            try {
                pw = new PrintWriter(f);
                pw.write(text);
                pw.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
    }

    public static void emptyDirectory(String dirPath) {
        File dir = new File(dirPath);
        for (File f : dir.listFiles()) f.delete();
    }
}
