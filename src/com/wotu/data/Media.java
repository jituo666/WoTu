package com.wotu.data;

import android.net.Uri;

public interface Media {
    public MediaPath getPath();

    public int getMediaType();

    public void delete();

    public Uri getContentUri();

    public int getSupportedOperations();

    public long getDataVersion();

    public int getCacheFlag();

    public int getCacheStatus();

    public long getCacheSize();

    public void cache(int flag);

}
