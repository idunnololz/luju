package com.ggstudios.luju;

import com.ggstudios.error.ParseException;
import com.ggstudios.error.TokenException;
import com.ggstudios.error.WeedException;
import com.ggstudios.utils.Print;

import java.io.BufferedReader;
import java.util.List;

public class LuJuCompiler {
    private Tokenizer tokenizer;
    private Parser parser;
    private AstGenerator astGen;

    public static final int RETURN_CODE_SUCCESS = 0;
    public static final int RETURN_CODE_ERROR = 42;

    public LuJuCompiler() {
        tokenizer = new Tokenizer();
        parser = new Parser();
        astGen = new AstGenerator();
    }

    public int compileWith(Main.ArgList args) {
        Ast ast = new Ast();
        for (String fileName : args.fileNames) {
            FileNode fn = new FileNode();
            List<Token> tokens = null;
            try {
                if (args.isTokenizeEnabled()) {
                    tokens = tokenizer.tokenizeWith(fileName);
                } else {
                    return RETURN_CODE_SUCCESS;
                }
                if (args.isPrintTokens()) {
                    for (Token t : tokens) {
                        //Print.ln(t.toString());
                        Print.ln(t.toString());
                    }
                }
            } catch (TokenException e) {
                Token t = e.getToken();
                Print.e(String.format("LuJu: TokenException(%d, %d): %s", t.getRow(), t.getCol(), e.getMessage()));

                return RETURN_CODE_ERROR;
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

                astGen.generateAst(fn, n);
                ast.addFileNode(fn);
            } catch (ParseException e) {
                Token t = e.getToken();
                Print.e(String.format("LuJu: ParseException(%d, %d): %s", t.getRow(), t.getCol(), e.getMessage()));

                return RETURN_CODE_ERROR;
            } catch (WeedException e) {
                Token t = e.getToken();
                Print.e(String.format("LuJu: ParseException(%d, %d): %s", t.getRow(), t.getCol(), e.getMessage()));

                return RETURN_CODE_ERROR;
            }
        }

        if (args.isPrintAst()) {
            Print.ln(ast.toPrettyString());
        }

        return RETURN_CODE_SUCCESS;
    }
}
