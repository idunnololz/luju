package com.ggstudios.env;

import java.util.HashMap;
import java.util.Map;

public class Package {
    private static final Map<String, Package> packageNameToPackage = new HashMap<>();

    private String packageName;

    protected Package(String packageName) {
        this.packageName = packageName;
    }

    protected static void addPackage(Package p) {
        packageNameToPackage.put(p.getName(), p);
    }

    public static Package getPackage(String packageName) {
        return packageNameToPackage.get(packageName);
    }

    public String getName() {
        return packageName;
    }
}
