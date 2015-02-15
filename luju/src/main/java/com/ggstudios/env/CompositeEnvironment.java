package com.ggstudios.env;

import com.ggstudios.types.AstNode;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class CompositeEnvironment extends Environment {
    private Deque<Environment> environments = new LinkedList<>();

    public void addEnvironment(Environment e) {
        environments.addFirst(e);
    }

    @Override
    public LookupResult lookupName(String[] name) {
        for (Environment e : environments) {
            LookupResult r = e.lookupName(name);
            if (r != null) {
                return r;
            }
        }
        return null;
    }
}
