package com.ggstudios.error;

public class TestFailedException extends RuntimeException {
    int res;
    public TestFailedException(int res) {
        super(String.format("Test failed. Expected '123' got '%d'", res));
        this.res = res;
    }

    public int getReturnCode() {
        return res;
    }
}
