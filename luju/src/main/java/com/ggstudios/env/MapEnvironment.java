package com.ggstudios.env;

import java.util.HashMap;
import java.util.Map;

public class MapEnvironment extends SimpleEnvironment {
    protected Map<String, Object> map = new HashMap<String, Object>();

    public Object put(String s, Object o) {
        return map.put(s, o);
    }

    @Override
    protected Object lookupNameOneStep(String name) {
        return map.get(name);
    }
}
