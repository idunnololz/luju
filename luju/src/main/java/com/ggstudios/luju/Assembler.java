package com.ggstudios.luju;

import com.ggstudios.utils.AssemblerUtils;
import com.ggstudios.utils.Print;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

    private boolean initialized = false;

    private Queue<String> unread = new LinkedList<>();

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

        initialized = false;
    }

    private void startThreads(final InputStream out, final InputStream err) {
        outputThread = new Thread() {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(out));
                String s;
                try {
                    while ((s = in.readLine()) != null) {
                        synchronized (unread) {
                            unread.add(s);
                            unread.notify();
                        }
                        Print.ln(s);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        errorThread = new Thread() {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(err));
                String s;
                try {
                    while ((s = in.readLine()) != null) {
                        Print.e(s);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        outputThread.start();
        errorThread.start();
    }

    public void initialize() {
        try {
            if (useCygwin) {
                if (!new File(cygwinBashPath).exists()) {
                    throw new RuntimeException("Cygwin bash executable does not exist. Given: " + cygwinBashPath);
                }
                proc = Runtime.getRuntime().exec(new String[]{cygwinBashPath, "-s"});
            } else {
                proc = Runtime.getRuntime().exec("bash -s");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ps = new PrintStream(proc.getOutputStream());

        startThreads(proc.getInputStream(), proc.getErrorStream());

        initialized = true;
    }

    public void assemble(String directoryPath) throws IOException {
        if (!initialized) {
            initialize();
        }

        generateUtilFiles(directoryPath);

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
        ps.println("echo $'\\n'$?");
        ps.flush();
    }

    private void generateUtilFiles(String dir) {
        if (os == Os.WINDOWS) {
            AssemblerUtils.outputWindowsHelperFile(dir);
        }

        AssemblerUtils.outputUtilsFile(dir);
    }

    public void shutdown() {
        if (!initialized) return;

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

    public String getResult() {
        synchronized (unread) {
            while (unread.size() == 0) {
                try {
                    unread.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return unread.poll();
        }
    }
}
