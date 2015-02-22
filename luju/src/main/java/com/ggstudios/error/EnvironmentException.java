package com.ggstudios.error;

public class EnvironmentException extends RuntimeException {
    private Object o;
    private int type;

    public static final int ERROR_CLASS_NAME_CLASH = 1;
    public static final int ERROR_NOT_FOUND = 2;
    public static final int ERROR_PACKAGE_IS_CLASS = 3;
    public static final int ERROR_SAME_VARIABLE_IN_SCOPE = 4;
    public static final int ERROR_NON_STATIC_FIELD_FROM_STATIC_CONTEXT = 5;
    public static final int ERROR_NON_STATIC_METHOD_FROM_STATIC_CONTEXT = 6;
    public static final int ERROR_STATIC_FIELD_FROM_NON_STATIC_CONTEXT = 7;
    public static final int ERROR_STATIC_METHOD_FROM_NON_STATIC_CONTEXT = 8;

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
