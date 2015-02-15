package com.ggstudios.error;

import com.ggstudios.luju.Token;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TokenException extends RuntimeException {
    private Token t;
    private String file;

    public TokenException(String file, Token t, String message) {
        super(message);
        this.t = t;
        this.file = file;
    }

    public Token getToken() {
        return t;
    }

    public String getFile() {
        Path base = Paths.get(System.getProperty("user.dir"));
        Path p = Paths.get(file);
        return ".." + File.separator + base.relativize(p).toString();
    }
}
