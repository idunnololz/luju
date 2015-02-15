package com.ggstudios.error;

public class EnvironmentException extends RuntimeException {
    private Object o;
    private int type;

    public static final int ERROR_CLASS_NAME_CLASH = 1;
    public static final int ERROR_NOT_FOUND = 2;
    public static final int ERROR_PACKAGE_IS_CLASS = 3;
    public static final int ERROR_SAME_VARIABLE_IN_SCOPE = 4;

    public EnvironmentException(String message, int type) {
        super(message);
        this.type = type;
    }
    public EnvironmentException(String message, int type, Object extra) {
        super(message);
        this.type = type;
        o = extra;
    }

    public int getType() {
        return type;
    }

    public Object getExtra() {
        return o;
    }
}
