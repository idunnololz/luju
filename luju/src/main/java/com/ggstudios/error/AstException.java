package com.ggstudios.error;

import com.ggstudios.luju.Token;
import com.ggstudios.types.AstNode;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AstException extends RuntimeException {
    private AstNode node;
    private String fileName;

    public AstException(String fileName, AstNode n, String message) {
        super(message);
        node = n;
        this.fileName = fileName;
    }

    public AstNode getNode() {
        return node;
    }

    public String getFile() {
        Path base = Paths.get(System.getProperty("user.dir"));
        Path p = Paths.get(fileName);
        try {
            return ".." + File.separator + base.relativize(p).toString();
        } catch (Exception e) {
            return ".." + File.separator + fileName;
        }
    }
}
