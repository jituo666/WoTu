package com.wotu.data;

import com.wotu.app.WoTuApp;

import android.content.ContentProviderClient;
import android.graphics.Path;

public class LocalMediaSource extends MediaSource {

    public static final String TAG = "LocalMediaSource";
    private WoTuApp mApplication;
    private ContentProviderClient mClient;

    private static final int LOCAL_IMAGE_ALBUMSET = 0;
    private static final int LOCAL_VIDEO_ALBUMSET = 1;
    private static final int LOCAL_IMAGE_ALBUM = 2;
    private static final int LOCAL_VIDEO_ALBUM = 3;
    private static final int LOCAL_IMAGE_ITEM = 4;
    private static final int LOCAL_VIDEO_ITEM = 5;
    private static final int LOCAL_ALL_ALBUMSET = 6;
    private static final int LOCAL_ALL_ALBUM = 7;

    protected LocalMediaSource(String source) {
        super(source);
        // TODO Auto-generated constructor stub
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        // TODO Auto-generated method stub
        return null;
    }

}
