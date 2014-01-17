package com.wotu.data;

import android.graphics.Path;

public abstract class MediaSource {
    // MediaSource, local |cloud |Mtp | piscas | LAN
    private static final String TAG = "MediaSource";
    private String mSourceName;

    protected MediaSource(String source) {
        mSourceName = source;
    }

    public String getSourceName() {
        return mSourceName;
    }

    public void pause() {
    }

    public void resume() {
    }

    public abstract MediaObject createMediaObject(Path path);
}
