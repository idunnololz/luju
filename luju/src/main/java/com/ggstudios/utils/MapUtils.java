package com.ggstudios.utils;

import java.util.Map;

public class MapUtils {
    public static <K,V> V getOrDefault(Map<K,V> map, Object key, V defaultValue) {
        V e;
        return (e = map.get(key)) == null ? defaultValue : e;
    }
}
