package com.ggstudios.luju;

import com.ggstudios.utils.Print;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

public class Assembler {

    enum Os {
        WINDOWS,
        LINUX
    }

    private Os os;
    private boolean useCygwin;
    private String cygwinBashPath;

    private Thread outputThread;
    private Thread errorThread;
    private PrintStream ps;

    private volatile boolean quit = false;
    private Process proc;

    public static class Builder {
        private Os os = Os.LINUX;
        private boolean useCygwin = false;
        private String cygwinBashPath = "";

        public Builder setTargetOs(Os os) {
            this.os = os;
            return this;
        }

        public Builder setUseCygwin(boolean yes, String cygwinBashPath) {
            useCygwin = yes;
            this.cygwinBashPath = cygwinBashPath;
            return this;
        }

        public Assembler build() throws IOException {
            return new Assembler(os, useCygwin, cygwinBashPath);
        }
    }

    public Assembler(Os os, boolean useCygwin, String cygwinBashPath) throws IOException {
        this.os = os;
        this.useCygwin = useCygwin;
        this.cygwinBashPath = cygwinBashPath;

        if (useCygwin) {
            if (!new File(cygwinBashPath).exists()) {
                throw new RuntimeException("Cygwin bash executable does not exist. Given: " + cygwinBashPath);
            }
            proc = Runtime.getRuntime().exec(new String[]{cygwinBashPath, "-s"});
        } else {
            proc = Runtime.getRuntime().exec("bash -s");
        }

        ps = new PrintStream(proc.getOutputStream());

        startThreads(proc.getInputStream(), proc.getErrorStream());
    }

    private void startThreads(final InputStream out, final InputStream err) {
        outputThread = new Thread() {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(out));
                while (!quit) {
                    try {
                        String s = in.readLine();
                        Print.ln(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        errorThread = new Thread() {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(err));
                while (!quit) {
                    try {
                        String s = in.readLine();
                        Print.e(s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        outputThread.start();
        errorThread.start();
    }

    public void assemble(String directoryPath) throws IOException {
        File dir = new File(directoryPath);
        File [] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".s");
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("nasm -O1 -f elf -F dwarf ");

        for (File asm : files) {
            String fileName = asm.getCanonicalPath();
            fileName = fileName.replace("\\","/");
            sb.append(fileName);
            ps.println(sb.toString());
            sb.setLength(sb.length() - fileName.length());
        }
        ps.println("ld output/*.o -lmsvcrt -lkernel32 -subsystem=console -entry=_start -o test.exe");
        ps.println("./test");
        ps.flush();
    }

    public void shutdown() {
        quit = true;

        ps.println("exit");
        ps.flush();
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (outputThread.isAlive()) {
            proc.destroy();
        }
    }
}
