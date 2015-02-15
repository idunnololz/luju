package com.ggstudios.env;

import com.ggstudios.types.AstNode;

public class LookupResult {
    public Object result;
    public int tokensConsumed;

    public LookupResult(Object o, int i) {
        result = o;
        tokensConsumed = i;
    }
}
