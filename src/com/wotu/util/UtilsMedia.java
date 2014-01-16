package com.wotu.util;

import android.os.Environment;

import com.wotu.data.MediaPath;
import com.wotu.data.MediaSetObject;

import java.util.Comparator;

public class UtilsMedia {
    
    public static final String IMPORTED = "Imported";
    public static final String DOWNLOAD = "download";
    public static final Comparator<MediaSetObject> NAME_COMPARATOR = new NameComparator();

    public static final int CAMERA_BUCKET_ID = UtilsCom.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera");
    public static final int DOWNLOAD_BUCKET_ID = UtilsCom.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
            + DOWNLOAD);
    public static final int IMPORTED_BUCKET_ID = UtilsCom.getBucketId(
            Environment.getExternalStorageDirectory().toString() + "/"
            + IMPORTED);
    public static final int SNAPSHOT_BUCKET_ID = UtilsCom.getBucketId(
            Environment.getExternalStorageDirectory().toString() +
            "/Pictures/Screenshots");

    private static final MediaPath[] CAMERA_PATHS = {
            new MediaPath("/local/all/" + CAMERA_BUCKET_ID),
            new MediaPath("/local/image/" + CAMERA_BUCKET_ID),
            new MediaPath("/local/video/" + CAMERA_BUCKET_ID)};

    public static boolean isCameraSource(MediaPath path) {
        return CAMERA_PATHS[0] == path || CAMERA_PATHS[1] == path
                || CAMERA_PATHS[2] == path;
    }

    // Sort MediaSets by name
    public static class NameComparator implements Comparator<MediaSetObject> {
        public int compare(MediaSetObject set1, MediaSetObject set2) {
            int result = set1.getName().compareToIgnoreCase(set2.getName());
            if (result != 0) return result;
            return set1.getPath().toString().compareTo(set2.getPath().toString());
        }
    }
}
