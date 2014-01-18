package com.wotu.data;

import java.util.HashMap;

public class PathMatcher {

    private HashMap<String, Integer> mPathMap = new HashMap<String, Integer>();

    public void add(String pattern, int kind) {
        mPathMap.put(pattern, kind);
    }

    public int match(String key) {
        return mPathMap.get(key);
    }
}
