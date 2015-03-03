package com.ggstudios.luju;

import com.ggstudios.env.*;
import com.ggstudios.env.Class;
import com.ggstudios.types.TypeDecl;
import com.ggstudios.utils.PrintUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileNode {
    private String filePath;
    private String className;
    private List<Token> tokens;

    private String packageName;
    private List<String> imports = new ArrayList<>();

    private TypeDecl typeDecl;

    private Environment env;
    private Class aClass;

    public FileNode() {
        imports.add("java.lang.*");
    }

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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void toPrettyString(StringBuilder sb, int level) {
        PrintUtils.level(sb, level);
        sb.append("(PackageName ");
        sb.append(packageName);
        sb.append("; FileName ");
        sb.append(getFileClassName());
        sb.append("; Imports [");
        if (imports.size() > 0) {
            for (String s : imports) {
                sb.append(s);
                sb.append(", ");
            }
            sb.setLength(sb.length() - 2);
        }
        sb.append("])\n");
        typeDecl.toPrettyString(sb, level + 1);
    }

    public List<String> getImports() {
        return imports;
    }

    public void addImport(String imp) {
        imports.add(imp);
    }

    public TypeDecl getTypeDecl() {
        return typeDecl;
    }

    public void setTypeDecl(TypeDecl typeDecl) {
        this.typeDecl = typeDecl;
    }

    public Environment getEnv() {
        return env;
    }

    public void setEnv(Environment env) {
        this.env = env;
    }

    public void setClass(Class aClass) {
        this.aClass = aClass;
    }

    public Class getThisClass() {
        return aClass;
    }

    public void setaClass(Class aClass) {
        this.aClass = aClass;
    }
}
