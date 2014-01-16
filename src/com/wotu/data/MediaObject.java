package com.wotu.data;

import android.net.Uri;

public abstract class MediaObject implements Media{
    @SuppressWarnings("unused")
    private static final String TAG = "MediaObject";
    public static final long INVALID_DATA_VERSION = -1;

    public static final int MEDIA_TYPE_UNKNOWN = 1;
    public static final int MEDIA_TYPE_IMAGE = 2;
    public static final int MEDIA_TYPE_VIDEO = 4;
    public static final int MEDIA_TYPE_ALL = MEDIA_TYPE_IMAGE | MEDIA_TYPE_VIDEO;

    public static final String MEDIA_TYPE_IMAGE_STRING = "image";
    public static final String MEDIA_TYPE_VIDEO_STRING = "video";
    public static final String MEDIA_TYPE_ALL_STRING = "all";

    public static final int CACHE_FLAG_NO = 0;
    public static final int CACHE_FLAG_SCREENNAIL = 1;
    public static final int CACHE_FLAG_FULL = 2;

    public static final int CACHE_STATUS_NOT_CACHED = 0;
    public static final int CACHE_STATUS_CACHING = 1;
    public static final int CACHE_STATUS_CACHED_SCREENNAIL = 2;
    public static final int CACHE_STATUS_CACHED_FULL = 3;

    protected static long sVersionSerial = 0;

    protected long mDataVersion;

    protected final MediaPath mPath;

    public MediaObject(MediaPath path, long version) {
        path.setObject(this);
        mPath = path;
        mDataVersion = version;
    }

    @Override
    public MediaPath getPath() {
        return mPath;
    }
    @Override
    public int getSupportedOperations() {
        return 0;
    }
    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }
    @Override
    public Uri getContentUri() {
        throw new UnsupportedOperationException();
    }

    public int getMediaType() {
        return MEDIA_TYPE_UNKNOWN;
    }
    @Override
    public long getDataVersion() {
        return mDataVersion;
    }
    @Override
    public int getCacheFlag() {
        return CACHE_FLAG_NO;
    }
    @Override
    public int getCacheStatus() {
        throw new UnsupportedOperationException();
    }
    @Override
    public long getCacheSize() {
        throw new UnsupportedOperationException();
    }
    @Override
    public void cache(int flag) {
        throw new UnsupportedOperationException();
    }

    public static synchronized long nextVersionNumber() {
        return ++MediaObject.sVersionSerial;
    }

    public static int getTypeFromString(String s) {
        if (MEDIA_TYPE_ALL_STRING.equals(s))
            return MediaObject.MEDIA_TYPE_ALL;
        if (MEDIA_TYPE_IMAGE_STRING.equals(s))
            return MediaObject.MEDIA_TYPE_IMAGE;
        if (MEDIA_TYPE_VIDEO_STRING.equals(s))
            return MediaObject.MEDIA_TYPE_VIDEO;
        throw new IllegalArgumentException(s);
    }

    public static String getTypeString(int type) {
        switch (type) {
            case MEDIA_TYPE_IMAGE:
                return MEDIA_TYPE_IMAGE_STRING;
            case MEDIA_TYPE_VIDEO:
                return MEDIA_TYPE_VIDEO_STRING;
            case MEDIA_TYPE_ALL:
                return MEDIA_TYPE_ALL_STRING;
        }
        throw new IllegalArgumentException();
    }
}
