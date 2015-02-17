package com.ggstudios.error;

import com.ggstudios.env.Class;
import com.ggstudios.types.AstNode;

public class InconvertibleTypeException extends TypeException {
    public InconvertibleTypeException(String fileName, AstNode n, Class cast, Class original) {
        super(fileName, n, String.format("Inconvertible types; cannot cast '%s' to '%s'",
                original.getCanonicalName() + (original.isArray() ? "[]" : ""),
                cast.getCanonicalName() + (cast.isArray() ? "[]" : "")));
    }
}
