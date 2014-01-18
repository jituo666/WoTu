
package com.wotu.data;

import android.net.Uri;

public abstract class MediaSource {
    private static final String TAG = "MediaSource";

    public static final int TYPE_NOT_CATEGORIZED = 0;
    public static final int TYPE_LOCAL = 1;
    public static final int TYPE_PICASA = 2;
    public static final int TYPE_MTP = 3;
    public static final int TYPE_CAMERA = 4;


    private static final String LOCAL_ROOT = "/local";
    private static final String PICASA_ROOT = "/picasa";
    private static final String MTP_ROOT = "/mtp";

    private String mPrefix;

    protected MediaSource(String prefix) {
        mPrefix = prefix;
    }

    public String getPrefix() {
        return mPrefix;
    }

    public Path findPathByUri(Uri uri, String type) {
        return null;
    }

    public abstract MediaObject createMediaObject(Path path);

    public void pause() {
    }

    public void resume() {
    }

    public long getTotalUsedCacheSize() {
        return 0;
    }

    public long getTotalTargetCacheSize() {
        return 0;
    }

    public static int identifySourceType(MediaSet set) {
        if (set == null) {
            return TYPE_NOT_CATEGORIZED;
        }
        Path path = set.getPath();
        String prefix = path.getPrefix();

        if (prefix.startsWith(PICASA_ROOT))
            return TYPE_PICASA;
        if (prefix.startsWith(MTP_ROOT))
            return TYPE_MTP;
        if (prefix.startsWith(LOCAL_ROOT))
            return TYPE_LOCAL;

        return TYPE_NOT_CATEGORIZED;
    }
}
