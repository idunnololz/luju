package com.ggstudios.env;

import java.util.Deque;
import java.util.LinkedList;

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
    public LookupResult lookup(String[] name) {
        boolean temp = Environment.allowNonStatic;
        if (classMemEnv != null) {
            LookupResult r = classMemEnv.lookup(name);
            if (r != null) {
                return r;
            }
            Environment.allowNonStatic = temp;
        }
        for (Environment e : environments) {
            LookupResult r = e.lookup(name);
            if (r != null) {
                return r;
            }
            Environment.allowNonStatic = temp;
        }
        return null;
    }

    public LookupResult lookupName(String[] name, boolean neglectClassMembers) {
        if (!neglectClassMembers && classMemEnv != null) {
            LookupResult r = classMemEnv.lookup(name);
            if (r != null) {
                return r;
            }
        }
        for (Environment e : environments) {
            LookupResult r = e.lookup(name);
            if (r != null) {
                return r;
            }
        }
        return null;
    }
}
