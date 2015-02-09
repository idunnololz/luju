package com.ggstudios.utils;

import com.ggstudios.luju.LuJuCompiler;
import com.ggstudios.luju.Main;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class TestSuite {
    private LuJuCompiler compiler;
    private int testCount, testPassed, testFailed;

    public TestSuite() {
        compiler = new LuJuCompiler();
    }

    public void runTests() {
        Print.setPrinter(new Print.SilentPrinter());
        runTests("tests/marmoset/a1");
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
                }
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            System.err.println(String.format("Exception running test %s", listOfFiles[i].getName()));
            System.err.println(sw.toString());
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
}
