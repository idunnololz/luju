package com.ggstudios.luju;

import com.ggstudios.utils.Print;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNode {
    private String filePath;
    private String className;
    private List<Token> tokens;
    private boolean isInterface;
    private boolean hasConstructor;

    public FileNode() {}

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public String getFileClassName() {
        Pattern pat = Pattern.compile("(?:.*[/\\\\])?([a-zA-Z_0-9]+).(joos|java)");
        Matcher m = pat.matcher(filePath);
        m.find();
        return m.group(1);
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setIsInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

    public boolean hasConstructor() {
        return hasConstructor;
    }

    public void setHasConstructor(boolean hasConstructor) {
        this.hasConstructor = hasConstructor;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
