package com.ggstudios.env;

import java.util.HashMap;
import java.util.Map;

public class ClassEnvironment extends Environment {
    private final Clazz map;

    public ClassEnvironment(Clazz c) {
        map = c;
    }

    public Object put(String s, Object o) {
        return map.put(s, o);
    }

    @Override
    public LookupResult lookupName(String[] name) {
        if (name.length == 0) return null;

        Object o = map.get(name[0]);
        return o == null ? null : new LookupResult(o, 1);
    }
}
