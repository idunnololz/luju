package com.ggstudios.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class AssemblerUtils {

    private static final String RESOURCE_DIR = "luju/res/";

    private static final String WINDOWS_HELPER_FILE_NAME = "wruntime.s";

    private static final String HELPER_FILE_NAME = "runtime.s";
    private static final String UTILS_FILE_NAME = "utils.s";

    public static void outputWindowsHelperFile(String directory) {
        try {
            FileUtils.copyFile(new File(RESOURCE_DIR + WINDOWS_HELPER_FILE_NAME),
                    new File(directory + File.separator + HELPER_FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void outputUtilsFile(String directory) {
        try {
            FileUtils.copyFile(new File(RESOURCE_DIR + UTILS_FILE_NAME),
                    new File(directory + File.separator + UTILS_FILE_NAME));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
