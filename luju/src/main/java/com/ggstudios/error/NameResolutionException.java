package com.ggstudios.error;

import com.ggstudios.types.AstNode;

public class NameResolutionException extends AstException {
    public NameResolutionException(AstNode n, String message) {
        super(n, message);
    }
}
