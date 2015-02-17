package com.ggstudios.env;

import com.ggstudios.utils.Print;

import java.util.HashMap;
import java.util.Map;

public class MapEnvironment extends Environment {
    protected Map<String, Object> map = new HashMap<String, Object>();

    public Object put(String s, Object o) {
        return map.put(s, o);
    }

    @Override
    public LookupResult lookupName(String[] name) {
        if (name.length == 0) return null;

        Object o = map.get(name[0]);
        int i = 1;
        while (i != name.length) {
            if (o instanceof Map) {
                o = ((Map) o).get(name[i++]);
            } else if (o instanceof Field) {
                o = ((Field) o).getType().get(name[i++]);
            } else {
                break;
            }
        }
        return o == null ? null : new LookupResult(o, i);
    }
}
