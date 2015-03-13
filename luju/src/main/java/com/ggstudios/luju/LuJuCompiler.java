package com.ggstudios.luju;

import com.ggstudios.env.Class;
import com.ggstudios.env.Field;
import com.ggstudios.env.Interface;
import com.ggstudios.env.Method;
import com.ggstudios.error.AstException;
import com.ggstudios.error.NameResolutionException;
import com.ggstudios.error.ParseException;
import com.ggstudios.error.TokenException;
import com.ggstudios.error.WeedException;
import com.ggstudios.types.AstNode;
import com.ggstudios.utils.ExceptionUtils;
import com.ggstudios.utils.Print;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LuJuCompiler {
    private NameResolver nameResolver = new NameResolver();
    private CodeGenerator codeGenerator = new CodeGenerator();
    private Assembler assembler;

    public static final int RETURN_CODE_SUCCESS = 0;
    public static final int RETURN_CODE_ERROR = 42;
    public static final int RETURN_CODE_FATAL_ERROR = 43;

    private static final Set<String> stdPackage = new HashSet<>();
    static {
        stdPackage.add("java.lang");
    }

    private ExecutorService executor;
    private WorkerPool workerPool;

    public LuJuCompiler(int maxThreads, boolean useCygwin) {
        executor = Executors.newFixedThreadPool(maxThreads);
        workerPool = new WorkerPool(maxThreads * 2);

        Assembler.Builder builder = new Assembler.Builder();
        try {
            assembler = builder.setTargetOs(Assembler.Os.WINDOWS)
                    .setUseCygwin(useCygwin, "C:\\cygwin64\\bin\\bash.exe")
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        executor.shutdown();
        workerPool.ensureNoLostWorker();
        assembler.shutdown();
    }

    private HashMap<String, FileNode> cachedFileNodes = new HashMap<>();

    public int compileWith(final Main.ArgList args) {
        final Ast ast = new Ast();

        Class.reset();
        Field.reset();
        Interface.reset();
        Method.reset();

        final CountDownLatch doneSignal = new CountDownLatch(args.fileNames.size());

        final Result result = new Result();

        for (final String fileName : args.fileNames) {
            FileNode fn;
            if (args.useCache && (fn = cachedFileNodes.get(fileName)) != null) {
                ast.addFileNode(fn);
                doneSignal.countDown();
            } else {
                FileNodeWorker worker = workerPool.getWorker();
                worker.setup(args, ast, fileName, doneSignal, result);
                executor.execute(worker);
            }
        }

        try {
            doneSignal.await();
        } catch (InterruptedException e) {
            Print.e(ExceptionUtils.exceptionToString(e));
            result.error = RETURN_CODE_FATAL_ERROR;
        }

        if (result.error != 0) {
            return processError(result.error);
        }

        if (args.useCache && cachedFileNodes.size() == 0) {
            for (int i = 0; i < ast.size(); i++) {
                cachedFileNodes.put(ast.get(i).getFilePath(), ast.get(i));
            }
        }

        if (args.isPrintAst()) {
            Print.ln(ast.toPrettyString(stdPackage));
        }

        try {
            nameResolver.resolveNames(ast);
        } catch (NameResolutionException e) {
            AstNode n = e.getNode();
            Print.e(String.format("LuJu: %s: NameResolutionException(%d, %d): %s", e.getFile(), n.getRow(), n.getCol(), e.getMessage()));
            return processError(RETURN_CODE_ERROR);
        } catch (AstException e) {
            AstNode n = e.getNode();
            Print.e(String.format("LuJu: %s: %s[%s](%d, %d): %s", e.getFile(), e.getClass().getSimpleName(), n.getClass().getSimpleName(), n.getRow(), n.getCol(), e.getMessage()));
            return processError(RETURN_CODE_ERROR);
        }

        if (args.isGenerateCode()) {
            codeGenerator.generateCode(ast, assembler);
        }

        return RETURN_CODE_SUCCESS;
    }

    private int processError(int error) {
        switch (error) {
            case RETURN_CODE_ERROR:
                return error;
            default:
                throw new RuntimeException("Fatal error");
        }
    }

    private static class FileNodeWorker implements Runnable {
        private Tokenizer tokenizer = new Tokenizer();
        private Parser parser = new Parser();
        private AstGenerator astGenerator = new AstGenerator();
        private Ast ast;
        private Main.ArgList args;
        private String fileName;
        private CountDownLatch doneSignal;
        private Result result;

        private WorkerPool parent;

        public FileNodeWorker(WorkerPool pool) {
            this.parent = pool;
        }

        public void setup(Main.ArgList args, Ast ast, String fileName, CountDownLatch doneSignal,
                          Result result) {
            this.ast = ast;
            this.args = args;
            this.fileName = fileName;
            this.doneSignal = doneSignal;
            this.result = result;
        }

        @Override
        public void run() {
            FileNode fn;
            try {
                fn = generateFileNode(args, fileName);

                if (fn == null) {
                    result.error = RETURN_CODE_ERROR;
                } else {
                    synchronized (ast) {
                        ast.addFileNode(fn);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                result.error = RETURN_CODE_FATAL_ERROR;
            }

            doneSignal.countDown();
            parent.free(this);
        }

        private FileNode generateFileNode(Main.ArgList args, String fileName) {
            FileNode fn = new FileNode();
            List<Token> tokens;
            try {
                tokens = tokenizer.tokenizeWith(fileName);

                if (args.isPrintTokens()) {
                    for (Token t : tokens) {
                        Print.ln(t.toString());
                    }
                }
            } catch (TokenException e) {
                Token t = e.getToken();
                Print.e(String.format("LuJu: %s: TokenException(%d, %d): %s", e.getFile(), t.getRow(), t.getCol(), e.getMessage()));

                return null;
            }

            fn.setFilePath(fileName);
            fn.setTokens(tokens);

            try {
                Parser.Node n = null;
                if (args.isParseEnabled()) {
                    n = parser.parse(fn);
                }

                if (args.isPrintParseTree()) {
                    Print.ln(n.toPrettyString());
                }

                astGenerator.generateAst(fn, n);
            } catch (ParseException e) {
                Token t = e.getToken();
                Print.e(String.format("LuJu: %s: ParseException(%d, %d): %s",  e.getFile(), t.getRow(), t.getCol(), e.getMessage()));

                return null;
            } catch (WeedException e) {
                Token t = e.getToken();
                Print.e(String.format("LuJu: %s: ParseException(%d, %d): %s",  e.getFile(), t.getRow(), t.getCol(), e.getMessage()));

                return null;
            }

            return fn;
        }
    }

    private static class WorkerPool {
        private Object lock = new Object();
        private List<FileNodeWorker> workers = new ArrayList<>();
        private Stack<FileNodeWorker> workerStack = new Stack<>();

        public WorkerPool(int maxWorkers) {
            for (int i = 0; i < maxWorkers; i++) {
                FileNodeWorker worker = new FileNodeWorker(this);
                workers.add(worker);
                workerStack.push(worker);
            }
        }

        public FileNodeWorker getWorker() {
            synchronized (lock) {
                while (workerStack.empty()) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return workerStack.pop();
            }
        }

        public void free(FileNodeWorker worker) {
            synchronized (lock) {
                workerStack.push(worker);
                lock.notify();
            }
        }

        public void ensureNoLostWorker() {
            if (workers.size() != workerStack.size()) {
                System.err.println(String.format("Worker(s) lost! Total worker(s): %s. Free worker(s): %s.",
                        workers.size(), workerStack.size()));
            }
        }
    }

    private static class Result {
        int error = 0;
    }
}
