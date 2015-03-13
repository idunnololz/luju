package com.ggstudios.utils;

import com.ggstudios.error.TestFailedException;
import com.ggstudios.luju.LuJuCompiler;
import com.ggstudios.luju.Main;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestSuite {
    private LuJuCompiler compiler;
    private int testCount, testPassed, testFailed;

    private static final String STDLIB_DIR = "stdlib/2.0/java";

    private static final Main.ArgList defaultTestArgs = new Main.ArgList();

    public TestSuite() {
        compiler = new LuJuCompiler(Main.ArgList.DEFAULT_THREADS, defaultTestArgs.useCygwin);
        defaultTestArgs.useCache = true;
        try {
            addStdlib(defaultTestArgs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runTests(Main.ArgList argList) {
        defaultTestArgs.flags = argList.flags;
        Print.setPrinter(new Print.SilentPrinter());
        runTests("tests/marmoset/a" + argList.assignmentNumber);
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
            e.printStackTrace();
        }
        System.out.println(String.format("Results: %d/%d %s", testPassed, testPassed + testFailed,
                (testFailed + testPassed) == testCount ? "" : "Warning: Inconsistent test count"));
    }

    private void runTest(File testFile) throws IOException {
        testCount++;

        Main.ArgList args = new Main.ArgList(defaultTestArgs);
        args.fileNames.add(testFile.getCanonicalPath());

        int result = runTest(args);

        if (testFile.getName().startsWith("Je") || testFile.getName().startsWith("J1e")) {
            // this test should output an error...
            if (result == 0) {
                testFailed++;
                System.out.println(String.format("[Fail] Erronous test %s", testFile.getName()));
            } else if (result == 42) {
                testPassed++;
                System.out.println(String.format("[Pass] Erronous test %s", testFile.getName()));
            } else {
                throw new RuntimeException("Compiler did not return 0 or 42");
            }
        } else {
            // this test should pass...
            if (result == 0) {
                testPassed++;
                System.out.println(String.format("[Pass] Correct test %s", testFile.getName()));
            } else if (result == 42) {
                testFailed++;
                System.out.println(String.format("[Fail] Correct test %s", testFile.getName()));
            } else {
                throw new RuntimeException("Compiler did not return 0 or 42");
            }
        }
    }

    private void runTestDirectory(File testDir) throws IOException {
        testCount++;

        Main.ArgList args = new Main.ArgList(defaultTestArgs);
        List<File> files = FileUtils.getFilesInDirRecursive(testDir);
        for (File f : files) {
            args.fileNames.add(f.getCanonicalPath());
        }

        int result = runTest(args);

        if (testDir.getName().startsWith("Je") || testDir.getName().startsWith("J1e")) {
            // this test should output an error...
            if (result == 0) {
                testFailed++;
                System.out.println(String.format("[Fail] Erronous test %s", testDir.getName()));
            } else if (result == 42) {
                testPassed++;
                System.out.println(String.format("[Pass] Erronous test %s", testDir.getName()));
            } else {
                throw new RuntimeException("Compiler did not return 0 or 42");
            }
        } else {
            // this test should pass...
            if (result == 0) {
                testPassed++;
                System.out.println(String.format("[Pass] Correct test %s", testDir.getName()));
            } else if (result == 42) {
                testFailed++;
                System.out.println(String.format("[Fail] Correct test %s", testDir.getName()));
            } else {
                throw new RuntimeException("Compiler did not return 0 or 42");
            }
        }
    }

    private void addStdlib(Main.ArgList args) throws IOException {
        List<File> files = FileUtils.getFilesInDirRecursive(new File(STDLIB_DIR));
        for (File f : files) {
            args.fileNames.add(f.getCanonicalPath());
        }
    }

    private int runTest(Main.ArgList args) {
        int res = 0;
        try {
            res = compiler.compileWith(args);
        } catch (TestFailedException e) {
            if (e.getReturnCode() == 13) {
                res = 42;
            } else {
                res = 42;
                Print.e("Invalid test return code of: " + e.getReturnCode());
            }
        }
        return res;
    }

}
