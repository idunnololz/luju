package com.ggstudios.error;

import com.ggstudios.types.AstNode;

public class StaticAnalyzerException extends AstException {
    public StaticAnalyzerException(String fileName, AstNode n, String message) {
        super(fileName, n, message);
    }

    public StaticAnalyzerException(AstNode n, String message) {
        super("", n, message);
    }

    public void setFileName(String fileName) {
        super.setFileName(fileName);
    }
}
