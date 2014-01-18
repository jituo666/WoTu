package com.wotu.data;

import android.os.Environment;

import com.wotu.util.UtilsCom;

import java.util.Comparator;

public class MediaSetUtils {
    public static final String IMPORTED = "Imported";
    public static final String DOWNLOAD = "download";
    public static final Comparator<MediaSet> NAME_COMPARATOR = new NameComparator();

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

    private static final Path[] CAMERA_PATHS = {
            new Path("/local/all/", CAMERA_BUCKET_ID),
            new Path("/local/image/", CAMERA_BUCKET_ID),
            new Path("/local/video/", CAMERA_BUCKET_ID)
    };

    public static boolean isCameraSource(Path path) {
        return CAMERA_PATHS[0] == path || CAMERA_PATHS[1] == path
                || CAMERA_PATHS[2] == path;
    }

    // Sort MediaSets by name
    public static class NameComparator implements Comparator<MediaSet> {
        public int compare(MediaSet set1, MediaSet set2) {
            int result = set1.getName().compareToIgnoreCase(set2.getName());
            if (result != 0)
                return result;
            return set1.getPath().toString().compareTo(set2.getPath().toString());
        }
    }
}
