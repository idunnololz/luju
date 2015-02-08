package com.ggstudios.luju;

import com.ggstudios.utils.Print;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Main {

    private static final String TEST_DIR = "luju/src/tests/";

    public static void main(String[] args) {
        String[] a;

        if (args.length == 0) {
            a = new String[]{
                    "-p",
                    "-t",
                    TEST_DIR + "tok/simple1.joos"
            };
        } else {
            a = args;
        }

        handleArgs(a);
    }

    private static void handleArgs(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        ArgList argList = new ArgList();

        int i;
        for (i = 0; i < args.length; i++) {
            String flag = args[i];

            if (flag.charAt(0) == '-') {
                if (flag.length() != 2) {
                    printUsage();
                    return;
                }
                char op = flag.charAt(1);
                switch (op) {
                    case 't':
                        argList.flags |= ArgList.FLAG_PRINT_TOKENS;
                        break;
                    case 'p':
                        argList.flags |= ArgList.FLAG_PRINT_PARSE_TREE;
                        break;
                }
            } else {
                argList.fileName = flag;
            }
        }

        if (args.length != i) {
            printUsage();
            return;
        }

        LuJuCompiler compiler = new LuJuCompiler();
        compiler.compileWith(argList);
    }

    private static void printUsage() {
        Print.ln("usage: java luju [-t] [-p] input_file");
    }

    public static class ArgList {
        public static final int FLAG_ALL = 0xFFFF;
        public static final int FLAG_TOKENIZE = 0x1;
        public static final int FLAG_PARSE = 0x2;


        public static final int FLAG_PRINT_TOKENS       = 0x00010000;
        public static final int FLAG_PRINT_PARSE_TREE   = 0x00020000;

        public String fileName;
        public int flags = FLAG_ALL;

        public boolean isTokenizeEnabled() {
            return (flags & FLAG_TOKENIZE) != 0;
        }

        public boolean isParseEnabled() {
            return (flags & FLAG_PARSE) != 0;
        }

        public boolean isPrintTokens() {
            return (flags & FLAG_PRINT_TOKENS) != 0;
        }

        public boolean isPrintParseTree() {
            return (flags & FLAG_PRINT_PARSE_TREE) != 0;
        }
    }
}
