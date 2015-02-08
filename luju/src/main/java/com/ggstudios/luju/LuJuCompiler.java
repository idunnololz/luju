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

    public LuJuCompiler() {
        tokenizer = new Tokenizer();
        parser = new Parser();
    }

    public void compileWith(Main.ArgList args) {
        FileNode fn = new FileNode();
        List<Token> tokens = null;
        try {
            if (args.isTokenizeEnabled()) {
                tokens = tokenizer.tokenizeWith(args);
            } else {
                return;
            }
            if (args.isPrintTokens()) {
                for (Token t : tokens) {
                    //Print.ln(t.toString());
                    Print.ln(t.getType().toString());
                }
            }
        } catch (TokenException e) {
            Token t = e.getToken();
            Print.e(String.format("LuJu: TokenException(%d, %d): %s", t.getRow(), t.getCol(), e.getMessage()));
        }

        fn.setFilePath(args.fileName);
        fn.setTokens(tokens);

        try {
            Parser.Node n = null;
            if (args.isParseEnabled()) {
                n = parser.parse(fn);
            }

            if (args.isPrintParseTree()) {
                Print.ln(n.toPrettyString());
            }
        } catch (ParseException e) {
            Token t = e.getToken();
            Print.e(String.format("LuJu: ParseException(%d, %d): %s", t.getRow(), t.getCol(), e.getMessage()));
        } catch (WeedException e) {
            Token t = e.getToken();
            Print.e(String.format("LuJu: ParseException(%d, %d): %s", t.getRow(), t.getCol(), e.getMessage()));
        }
    }
}
