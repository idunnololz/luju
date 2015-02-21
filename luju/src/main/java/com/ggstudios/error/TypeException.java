package com.ggstudios.error;

import com.ggstudios.types.AstNode;

public class TypeException extends AstException {
    public TypeException(String fileName, AstNode n, String message) {
        super(fileName, n, message);
    }

    public TypeException(String fileName, AstNode n, String message, Throwable t) {
        super(fileName, n, message, t);
    }
}
