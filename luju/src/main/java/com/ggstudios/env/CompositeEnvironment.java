package com.ggstudios.env;

import com.ggstudios.types.AstNode;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * HACK: We are only using composite environment for class scope and class
 */
public class CompositeEnvironment extends Environment {
    private Environment classMemEnv;
    private Deque<Environment> environments = new LinkedList<>();

    public void addEnvironment(Environment e) {
        environments.addFirst(e);
    }

    public void setClassMemberEnvironment(Environment e) {
        classMemEnv = e;
    }

    @Override
    public LookupResult lookupName(String[] name) {
        if (classMemEnv != null) {
            LookupResult r = classMemEnv.lookupName(name);
            if (r != null) {
                return r;
            }
        }
        for (Environment e : environments) {
            LookupResult r = e.lookupName(name);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    public LookupResult lookupName(String[] name, boolean neglectClassMembers) {
        if (!neglectClassMembers && classMemEnv != null) {
            LookupResult r = classMemEnv.lookupName(name);
            if (r != null) {
                return r;
            }
        }
        for (Environment e : environments) {
            LookupResult r = e.lookupName(name);
            if (r != null) {
                return r;
            }
        }
        return null;
    }
}
