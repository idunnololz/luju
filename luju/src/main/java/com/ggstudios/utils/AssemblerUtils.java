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
                    "extern java.lang.Object$vt\n" +
                    "extern java.lang.Object\n" +
                    "\n" +
                    "section .text\n" +
                    "\n" +
                    "; check if index ebx is valid for array at eax\n" +
                    "\tglobal __arrayBoundCheck\n" +
                    "__arrayBoundCheck:\n" +
                    "\tcmp \t[eax+8], ebx\n" +
                    "\tjle \t.outOfBounds\n" +
                    "\tret\n" +
                    ".outOfBounds:\n" +
                    "\tcall\t__exception\n" +
                    "\t\n" +
                    "\tglobal __divideCheck\n" +
                    "__divideCheck:\n" +
                    "\ttest \tebx, ebx\n" +
                    "\tje  \t.divisionByZero\n" +
                    "\tret\n" +
                    ".divisionByZero:\n" +
                    "\tcall\t__exception\n" +
                    "\t\n" +
                    "; eax = address to zero, ebx = length (in DWORDs)\n" +
                    "; note that the actual length in bytes is ebx * 4 + 4 (for the length field)\n" +
                    "\tglobal __zeroArray\n" +
                    "__zeroArray:\n" +
                    "\tpush\teax\n" +
                    "\tpush\tecx\n" +
                    "\tpush\tedx\n" +
                    "\tmov\t\tecx, ebx\n" +
                    "\tadd \tecx, 3\n" +
                    "\txor \tedx, edx\n" +
                    ".loop:\n" +
                    "\tmov \t[eax], edx\n" +
                    "\tadd     eax, 4\n" +
                    "\tsub\t\tecx, 1\n" +
                    "\tjnz \t.loop\n" +
                    "\tpop \tedx\n" +
                    "\tpop \tecx\n" +
                    "\tpop \teax\n" +
                    "\tret\n" +
                    "\t\n" +
                    "; check if type eax is of type ebx\n" +
                    "\tglobal __instanceOf\n" +
                    "__instanceOf:\n" +
                    "\tpush\tebp\n" +
                    "\tmov \tebp, esp\n" +
                    "\t\n" +
                    "\tsub \tesp, 12\t; create 3 local variables...\n" +
                    "\t\t\t\t\t; -4 = a, -8 = t, -12 = i\n" +
                    "\tmov \teax, [ebp+8]\n" +
                    "\tmov \tebx, [ebp+12]\n" +
                    "\tmov\t\t[ebp-4], eax\n" +
                    "\tcmp \teax, ebx\n" +
                    "\tjne\t\t.start\n" +
                    "\tmov \teax, 1\n" +
                    "\tjmp \t.exit\n" +
                    ".start:\n" +
                    "\tlea \tecx, [eax+8]\n" +
                    "\tmov \t[ebp-8], ecx\n" +
                    "\tmov \tdword [ebp-12], 0\n" +
                    "\tjmp \t.forCond\n" +
                    ".forUpdate:\n" +
                    "\tmov \teax, [ebp-12]\n" +
                    "\tadd \teax, 1\n" +
                    "\tmov \t[ebp-12], eax\n" +
                    ".forCond: \n" +
                    "\tmov \teax, [ebp-4]\n" +
                    "\tmov \tecx, [ebp-12]\n" +
                    "\tcmp \tecx, dword [eax+4]\n" +
                    "\tjge\t\t.false\n" +
                    "\t\n" +
                    "\tpush  \tebx\n" +
                    "\tmov \teax, [ebp-8]\n" +
                    "\tmov \teax, [eax]\n" +
                    "\tpush\teax\n" +
                    "\tcall\t__instanceOf\n" +
                    "\tadd \tesp, 8\n" +
                    "\ttest\teax, eax\n" +
                    "\tje\t\t.continue\n" +
                    "\tmov \teax, 1\n" +
                    "\tjmp \t.exit\n" +
                    ".continue:\n" +
                    "\tmov \teax, [ebp-8]\n" +
                    "\tadd \teax, 8\n" +
                    "\tmov \t[ebp-8], eax\n" +
                    "\tjmp \t.forUpdate\n" +
                    ".false:\n" +
                    "\tmov \teax, 0\n" +
                    ".exit:\n" +
                    "\tmov \tesp, ebp\n" +
                    "\tpop \tebp\n" +
                    "\tret\n" +
                    "\t\n" +
                    "\t\n" +
                    "\t\n" +
                    "; define primitive array class structure\n" +
                    "\tglobal int#Array\n" +
                    "int#Array:\n" +
                    "\tdd\tjava.lang.Object$vt\n" +
                    "\tdd\t1\n" +
                    "\tdd\tjava.lang.Object\n" +
                    "\tdd\t0\n" +
                    "\tglobal short#Array\n" +
                    "short#Array:\n" +
                    "\tdd\tjava.lang.Object$vt\n" +
                    "\tdd\t1\n" +
                    "\tdd\tjava.lang.Object\n" +
                    "\tdd\t0\n" +
                    "\tglobal char#Array\n" +
                    "char#Array:\n" +
                    "\tdd\tjava.lang.Object$vt\n" +
                    "\tdd\t1\n" +
                    "\tdd\tjava.lang.Object\n" +
                    "\tdd\t0\n" +
                    "\tglobal boolean#Array\n" +
                    "boolean#Array:\n" +
                    "\tdd\tjava.lang.Object$vt\n" +
                    "\tdd\t1\n" +
                    "\tdd\tjava.lang.Object\n" +
                    "\tdd\t0\n" +
                    "\tglobal byte#Array\n" +
                    "byte#Array:\n" +
                    "\tdd\tjava.lang.Object$vt\n" +
                    "\tdd\t1\n" +
                    "\tdd\tjava.lang.Object\n" +
                    "\tdd\t0\n" +
                    "\t\n" +
                    "\t\n" +
                    "\t\n" +
                    "\t";

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
