package com.ggstudios.utils;

import com.ggstudios.luju.LuJuCompiler;
import com.ggstudios.luju.Main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TestSuite {
    private LuJuCompiler compiler;
    private int testCount, testPassed, testFailed;

    public TestSuite() {
        compiler = new LuJuCompiler(Main.ArgList.DEFAULT_THREADS);
    }

    public void runTests(int assignmentNumber) {
        Print.setPrinter(new Print.SilentPrinter());
        runTests("tests/marmoset/a" + assignmentNumber);
        compiler.shutdown();
    }

    public void runTests(String testFolder) {
        testCount = 0;
        testPassed = 0;

        File folder = new File(testFolder);
        File[] listOfFiles = folder.listFiles();

        int i = 0;
        try {
            for (; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    runTest(listOfFiles[i]);
                } else if (listOfFiles[i].isDirectory()) {
                    runTestDirectory(listOfFiles[i]);
                }
            }
        } catch (Exception e) {
            System.err.println(String.format("Exception running test %s", listOfFiles[i].getName()));
            System.err.println(ExceptionUtils.exceptionToString(e));
        }
        System.out.println(String.format("Results: %d/%d %s", testPassed, testPassed + testFailed,
                (testFailed + testPassed) == testCount ? "" : "Warning: Inconsistent test count"));
    }

    private void runTest(File testFile) throws IOException {
        testCount++;

        Main.ArgList args = new Main.ArgList();
        args.fileNames.add(testFile.getCanonicalPath());

        if (testFile.getName().startsWith("Je")) {
            // this test should output an error...
            if (compiler.compileWith(args) == 0) {
                testFailed++;
                System.out.println(String.format("[Fail] Erronous test %s", testFile.getName()));
            } else {
                testPassed++;
                System.out.println(String.format("[Pass] Erronous test %s", testFile.getName()));
            }
        } else {
            // this test should pass...
            if (compiler.compileWith(args) == 0) {
                testPassed++;
                System.out.println(String.format("[Pass] Correct test %s", testFile.getName()));
            } else {
                testFailed++;
                System.out.println(String.format("[Fail] Correct test %s", testFile.getName()));
            }
        }
    }

    private void runTestDirectory(File testDir) throws IOException {
        testCount++;

        Main.ArgList args = new Main.ArgList();
        List<File> files = FileUtils.getFilesInDirRecursive(testDir);
        for (File f : files) {
            args.fileNames.add(f.getCanonicalPath());
        }

        if (testDir.getName().startsWith("Je")) {
            // this test should output an error...
            if (compiler.compileWith(args) == 0) {
                testFailed++;
                System.out.println(String.format("[Fail] Erronous test %s", testDir.getName()));
            } else {
                testPassed++;
                System.out.println(String.format("[Pass] Erronous test %s", testDir.getName()));
            }
        } else {
            // this test should pass...
            if (compiler.compileWith(args) == 0) {
                testPassed++;
                System.out.println(String.format("[Pass] Correct test %s", testDir.getName()));
            } else {
                testFailed++;
                System.out.println(String.format("[Fail] Correct test %s", testDir.getName()));
            }
        }
    }

}
