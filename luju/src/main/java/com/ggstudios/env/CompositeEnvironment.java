package com.ggstudios.env;

import java.util.Deque;
import java.util.LinkedList;

/**
 * HACK: We are only using composite environment for class scope and class
 */
public class CompositeEnvironment extends Environment {
    private Environment classMemEnv;

    private Environment baseEnvironment;
    private Environment multiImportEnvironment;
    private Environment packageEnvironment;
    private Environment singleImportEnvironment;

    public void setClassMemberEnvironment(Environment e) {
        classMemEnv = e;
    }

    @Override
    public LookupResult lookup(String[] name) {
        return lookupName(name, false);
    }

    public LookupResult lookupName(String[] name, boolean neglectClassMembers) {
        boolean temp = Environment.allowNonStatic;
        boolean temp2 = Environment.allowProtected;

        LookupResult r;
        if (!neglectClassMembers && classMemEnv != null) {
            Environment.allowProtected = true;
            r = classMemEnv.lookup(name);
            if (r != null) {
                return r;
            }
            Environment.allowNonStatic = temp;
        }
        Environment.allowProtected = false;
        r = singleImportEnvironment.lookup(name);
        if (r != null) {
            return r;
        }
        Environment.allowNonStatic = temp;
        Environment.allowProtected = true;
        r = packageEnvironment.lookup(name);
        if (r != null) {
            return r;
        }
        Environment.allowNonStatic = temp;
        Environment.allowProtected = false;
        r = multiImportEnvironment.lookup(name);
        if (r != null) {
            return r;
        }
        Environment.allowNonStatic = temp;
        r = baseEnvironment.lookup(name);
        if (r != null) {
            return r;
        }

        Environment.allowNonStatic = temp;
        Environment.allowProtected = temp2;
        return null;
    }

    public void setBaseEnvironment(Environment baseEnvironment) {
        this.baseEnvironment = baseEnvironment;
    }

    public void setMultiImportEnvironment(Environment multiImportEnvironment) {
        this.multiImportEnvironment = multiImportEnvironment;
    }

    public void setPackageEnvironment(Environment packageEnvironment) {
        this.packageEnvironment = packageEnvironment;
    }

    public void setSingleImportEnvironment(Environment singleImportEnvironment) {
        this.singleImportEnvironment = singleImportEnvironment;
    }
}
