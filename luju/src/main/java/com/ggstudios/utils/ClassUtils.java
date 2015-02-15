package com.ggstudios.utils;

import com.ggstudios.env.Clazz;
import com.ggstudios.env.Interface;
import com.ggstudios.error.NameResolutionException;

import java.util.List;
import java.util.Map;

public class ClassUtils {
    public static Interface isCyclic(List<Interface> interfaces, Map<Clazz, Integer> interfaceToIndex) {
        int v = interfaces.size();
        boolean[] visited = new boolean[v];
        boolean[] recStack = new boolean[v];
        for (int i = 0; i < v; i++) {
            visited[i] = false;
            recStack[i] = false;
        }

        for(int i = 0; i < v; i++)
            if (isCyclicUtil(interfaceToIndex, interfaces.get(i), i, visited, recStack)) {
                return interfaces.get(i);
            }

        return null;
    }

    private static boolean isCyclicUtil(Map<Clazz, Integer> interfaceToIndex, Interface interface_, int v, boolean[] visited, boolean[] recStack) {
        if (!visited[v]) {
            // Mark the current node as visited and part of recursion stack
            visited[v] = true;
            recStack[v] = true;

            // Recur for all the vertices adjacent to this vertex
            for(Clazz c : interface_.getInterfaces()) {
                int i = interfaceToIndex.get(c);
                if ( !visited[i] && isCyclicUtil(interfaceToIndex, (Interface) c, i, visited, recStack) )
                    return true;
                else if (recStack[i])
                    return true;
            }

        }
        recStack[v] = false;  // remove the vertex from recursion stack
        return false;
    }
}
