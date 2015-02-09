package com.ggstudios.luju;

import com.ggstudios.utils.Print;
import com.ggstudios.utils.TestSuite;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String TEST_DIR = "luju/src/tests/";

    public static void main(String[] args) {
        String[] a;

        if (args.length == 0) {
            a = new String[]{
                    "-t",
                    "-A",
                    TEST_DIR + "tok/Interface1.java"
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
                    case 'T':
                        argList.flags |= ArgList.FLAG_RUN_TESTS;
                        break;
                    case 'A':
                        argList.flags |= ArgList.FLAG_PRINT_AST;
                        break;
                }
            } else {
                argList.fileNames.add(flag);
            }
        }

        if (args.length != i) {
            printUsage();
            return;
        }

        if (argList.isRunTests()) {
            TestSuite ts = new TestSuite();
            ts.runTests();
        } else {
            LuJuCompiler compiler = new LuJuCompiler();
            compiler.compileWith(argList);
        }
    }

    private static void printUsage() {
        Print.ln("usage: java luju [-t] [-p] [-T] [-A] input_file_1 ...");
    }

    public static class ArgList {
        public static final int FLAG_ALL = 0xFFFF;
        public static final int FLAG_TOKENIZE = 0x1;
        public static final int FLAG_PARSE = 0x2;


        public static final int FLAG_PRINT_TOKENS       = 0x00010000;
        public static final int FLAG_PRINT_PARSE_TREE   = 0x00020000;
        public static final int FLAG_RUN_TESTS          = 0x00040000;
        public static final int FLAG_PRINT_AST          = 0x00080000;

        public List<String> fileNames = new ArrayList<>();
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

        public boolean isRunTests() {
            return (flags & FLAG_RUN_TESTS) != 0;
        }

        public boolean isPrintAst() {
            return (flags & FLAG_PRINT_AST) != 0;
        }
    }
}
