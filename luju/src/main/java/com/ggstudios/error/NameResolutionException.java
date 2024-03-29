package com.ggstudios.error;

import com.ggstudios.types.AstNode;

public class NameResolutionException extends AstException {
    public NameResolutionException(String fileName, AstNode n, String message) {
        super(fileName, n, message);
    }

    public NameResolutionException(String fileName, AstNode n, String message, Throwable cause) {
        super(fileName, n, message, cause);
    }
}
