package com.ggstudios.asm;

import java.util.HashMap;
import java.util.Map;

public enum Register {
    EAX(0, "eax"),
    EBX(1, "ebx"),
    ECX(2, "ecx"),
    EDX(3, "edx"),

    EBP(0x10, "ebp"),
    ESP(0x11, "esp");

    private static final Map<Integer, Register> intToTypeMap = new HashMap<Integer, Register>();
    static {
        for (Register type : Register.values()) {
            intToTypeMap.put(type.value, type);
        }
    }


    private final int value;
    private final String asm;
    private Register(int value, String asm) {
        this.value = value;
        this.asm = asm;
    }

    public int getValue() {
        return value;
    }

    public String getAsm() {
        return asm;
    }

    public static Register getNextRegisterFrom(Register r) {
        return intToTypeMap.get((r.getValue() + 1) % 4);
    }

    public static Register getNextRegisterFrom(RegisterExpression re) {
        int i = 0;
        Register r;
        while (re.isRegisterUsed(r = intToTypeMap.get(i))) {
            i = (i + 1) % 4;
        }
        return r;
    }
}
