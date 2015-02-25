package com.ggstudios.luju;

import com.ggstudios.utils.AssemblerUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

public class CodeGenerator {
    private static final String OUTPUT_DIRECTORY = "output";

    public void generateCode(Ast ast, Assembler assembler) {
        AssemblerUtils.outputWindowsHelperFile(OUTPUT_DIRECTORY);

        File f = new File(OUTPUT_DIRECTORY + File.separator + "test.s");
        try {
            PrintWriter pw = new PrintWriter(f);
            pw.write("extern _printf\n" +
                    "extern __debexit\n" +
                    "global _start\n" +
                    "_start:\n" +
                    "push msg\n" +
                    "call _printf\n" +
                    "mov eax, 1\n" +
                    "call __debexit\n" +
                    "\n" +
                    "msg db \"Hello world\",0");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            assembler.assemble(OUTPUT_DIRECTORY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
