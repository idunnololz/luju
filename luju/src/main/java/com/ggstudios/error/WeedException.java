package com.ggstudios.error;

import com.ggstudios.luju.Token;

public class WeedException extends TokenException {
    public WeedException(String fileName, Token t, String message) {
        super(fileName, t, message);
    }
}
