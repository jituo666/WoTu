package com.wotu.data;


import com.wotu.util.UtilsBase;

import java.lang.ref.WeakReference;

public class MediaPath {
    private String mPathStr;
    private WeakReference<MediaObject> mObject;

    public MediaPath(String path) {
        mPathStr = path;
    }
    public String getPathStr() {
        return mPathStr;
    }
    public void setObject(MediaObject object) {
        synchronized (MediaPath.class) {
            UtilsBase.assertTrue(mObject == null || mObject.get() == null);
            mObject = new WeakReference<MediaObject>(object);
        }
    }

    public MediaObject getObject() {
        synchronized (MediaPath.class) {
            return (mObject == null) ? null : mObject.get();
        }
    }
}
