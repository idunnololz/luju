package com.ggstudios.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class AssemblerUtils {

    private static final String HELPER_FILE_NAME = "runtime.s";
    private static final String UTILS_FILE_NAME = "utils.s";

    public static void outputWindowsHelperFile(String directory) {
        File f = new File(directory + File.separator + HELPER_FILE_NAME);
        f.getParentFile().mkdirs();

        PrintWriter pw = null;
        try {
            f.createNewFile();

            pw = new PrintWriter(f);

            pw.write(HELPER_WINDOWS_SOURCE);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    public static void outputUtilsFile(String directory) {
        File f = new File(directory + File.separator + UTILS_FILE_NAME);
        f.getParentFile().mkdirs();

        PrintWriter pw = null;
        try {
            f.createNewFile();

            pw = new PrintWriter(f);

            pw.write(UTILS_SOURCE);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    private static final String UTILS_SOURCE =
            "extern __exception\n" +
                    "\n" +
                    "section .text\n" +
                    "; check if index ebx is valid for array at eax\n" +
                    "\tglobal __arrayBoundCheck\n" +
                    "__arrayBoundCheck:\n" +
                    "\tcmp [eax], ebx\n" +
                    "\tjle .outOfBounds\n" +
                    "\tret\n" +
                    ".outOfBounds:\n" +
                    "\tcall __exception";

    private static final String HELPER_WINDOWS_SOURCE = "extern _GetStdHandle@4\n" +
            "extern _WriteConsoleA@20\n" +
            "extern _ExitProcess@4\n" +
            "extern _malloc\n" +
            "\n" +
            "section .bss\n" +
            "        numCharsWritten:        resb 1\n" +
            "\n" +
            "section .text\n" +
            "\n" +
            "; Allocates eax bytes of memory. Pointer to allocated memory returned in eax.\n" +
            "    global __malloc\n" +
            "__malloc:\n" +
            "\tpush\teax\t\t\t; Arg1 : push desired number of bytes\n" +
            "    call \t_malloc\n" +
            "\tadd\t\tesp,\t4\t; restore stack pointer\n" +
            "    cmp \teax, 0   \t; on error, exit with code 22\n" +
            "    jne \tok\n" +
            "    mov \teax, 22\n" +
            "    call \t__debexit\n" +
            "ok:\n" +
            "    ret\n" +
            "\n" +
            "; Debugging exit: ends the process, returning the value of\n" +
            "; eax as the exit code.\n" +
            "    global __debexit\n" +
            "__debexit:\n" +
            "\tpush    eax         ; Arg1: push exit code\n" +
            "\tcall    _ExitProcess@4\n" +
            "\n" +
            "; Exceptional exit: ends the process with exit code 13.\n" +
            "; Call this in cases where the Joos code would throw an exception.\n" +
            "    global __exception\n" +
            "__exception:\n" +
            "    push \tdword 13\n" +
            "\tcall    _ExitProcess@4\n" +
            "\n" +
            "; Implementation of java.io.OutputStream.nativeWrite method.\n" +
            "; Outputs the low-order byte of eax to standard output.\n" +
            "    global NATIVEjava.io.OutputStream.nativeWrite\n" +
            "NATIVEjava.io.OutputStream.nativeWrite:\n" +
            "\t; get std handle\n" +
            "\tmov \t[char], \tal ; save the low order byte in memory\n" +
            "\tpush    dword -11\n" +
            "\tcall\t_GetStdHandle@4\n" +
            "\tpush    dword 0         ; Arg5: Unused so just use zero\n" +
            "\tpush    numCharsWritten ; Arg4: push pointer to numCharsWritten\n" +
            "\tpush    dword 1\t\t    ; Arg3: push length of output string\n" +
            "\tpush    char            ; Arg2: push pointer to output string\n" +
            "\tpush    eax             ; Arg1: push handle returned from _GetStdHandle\n" +
            "\tcall    _WriteConsoleA@20\n" +
            "\tmov eax, 0     \t\t\t; return 0\n" +
            "    ret\n" +
            "\n" +
            "section .data\n" +
            "\n" +
            "char:\n" +
            "    dd 0";
}
