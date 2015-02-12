package com.ggstudios.error;

import com.ggstudios.luju.Token;
import com.ggstudios.types.AstNode;

public class AstException extends RuntimeException {
    private AstNode node;

    public AstException(AstNode n, String message) {
        super(message);
        node = n;
    }


    public AstNode getNode() {
        return node;
    }
}
