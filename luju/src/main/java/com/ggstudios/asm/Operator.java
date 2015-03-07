package com.ggstudios.asm;

public enum Operator {
    PLUS("+"), MINUS("-");

    private String symbol;

    private Operator(String symbol) {
        this.symbol = symbol;
    }

    public String toString() {
        return symbol;
    }
}
