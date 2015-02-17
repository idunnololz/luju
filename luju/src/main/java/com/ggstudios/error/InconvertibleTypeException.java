package com.ggstudios.error;

import com.ggstudios.env.Clazz;
import com.ggstudios.types.AstNode;

public class InconvertibleTypeException extends TypeException {
    public InconvertibleTypeException(String fileName, AstNode n, Clazz cast, Clazz original) {
        super(fileName, n, String.format("Inconvertible types; cannot cast '%s' to '%s'",
                original.getCanonicalName() + (original.isArray() ? "[]" : ""),
                cast.getCanonicalName() + (cast.isArray() ? "[]" : "")));
    }
}
