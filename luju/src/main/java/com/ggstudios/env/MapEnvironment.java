package com.ggstudios.env;

import java.util.HashMap;
import java.util.Map;

public class MapEnvironment extends Environment {
    private Map<String, Object> map = new HashMap<String, Object>();

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
