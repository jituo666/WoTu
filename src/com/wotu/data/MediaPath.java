package com.wotu.data;

import com.wotu.util.UtilsBase;

import java.lang.ref.WeakReference;

public class MediaPath {

    private String mPathStr;
    private WeakReference<MediaObject> mMediaObject;

    public MediaPath(String path) {
        mPathStr = path;
    }

    public String getPathStr() {
        return mPathStr;
    }

    public void setObject(MediaObject object) {
        synchronized (MediaPath.class) {
            UtilsBase.assertTrue(mMediaObject == null || mMediaObject.get() == null);
            mMediaObject = new WeakReference<MediaObject>(object);
        }
    }

    public MediaObject getObject() {
        synchronized (MediaPath.class) {
            return (mMediaObject == null) ? null : mMediaObject.get();
        }
    }
}
