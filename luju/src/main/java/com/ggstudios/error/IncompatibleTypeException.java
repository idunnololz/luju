package com.ggstudios.error;

import com.ggstudios.env.Clazz;
import com.ggstudios.types.AstNode;

public class IncompatibleTypeException extends TypeException {
    public IncompatibleTypeException(String fileName, AstNode n, Clazz required, Clazz found) {
        super(fileName, n, String.format("Incompatible types.\n" +
                "\tRequired: %s\n" +
                "\tFound: %s",
                required.getCanonicalName() + (required.isArray() ? "[]" : ""),
                found.getCanonicalName() + (found.isArray() ? "[]" : "")));
    }
}
