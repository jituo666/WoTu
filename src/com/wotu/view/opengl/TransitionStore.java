package com.wotu.view.opengl;

import java.util.HashMap;

public class TransitionStore {
    private HashMap<Object, Object> mStorage = new HashMap<Object, Object>();

    public void put(Object key, Object value) {
        mStorage.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object key) {
        return (T) mStorage.get(key);
    }

    public void clear() {
        mStorage.clear();
    }
}
