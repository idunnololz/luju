package com.ggstudios.env;

import com.ggstudios.types.AstNode;

public abstract class Environment {
    public abstract AstNode lookupName(String name);
}