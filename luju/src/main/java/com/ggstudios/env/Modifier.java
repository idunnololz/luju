package com.ggstudios.env;

public class Modifier {
    public static final int PUBLIC          = 0x00000001;
    public static final int PRIVATE         = 0x00000002;
    public static final int PROTECTED       = 0x00000004;
    public static final int STATIC          = 0x00000008;
    public static final int FINAL           = 0x00000016;
    public static final int NATIVE          = 0x00000100;
    public static final int ABSTRACT        = 0x00000400;

    private static final int CLASS_MODIFIERS = PUBLIC | PROTECTED | ABSTRACT;

    public static int classModifiers() {
        return CLASS_MODIFIERS;
    }

    public static boolean isPublic(int mod) {
        return (mod & PUBLIC) != 0;
    }

    public static boolean isPrivate(int mod) {
        return (mod & PRIVATE) != 0;
    }

    public static boolean isProtected(int mod) {
        return (mod & PROTECTED) != 0;
    }

    public static boolean isStatic(int mod) {
        return (mod & STATIC) != 0;
    }

    public static boolean isFinal(int mod) {
        return (mod & FINAL) != 0;
    }

    public static boolean isNative(int mod) {
        return (mod & NATIVE) != 0;
    }

    public static boolean isAbstract(int mod) {
        return (mod & ABSTRACT) != 0;
    }
}
