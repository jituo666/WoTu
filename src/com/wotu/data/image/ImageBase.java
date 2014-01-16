package com.wotu.data.image;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;

import com.wotu.common.BytesBufferPool;
import com.wotu.common.ThreadPool.Job;
import com.wotu.data.MediaObject;
import com.wotu.data.MediaPath;
import com.wotu.data.bitmap.BitmapPool;
import com.wotu.data.bitmap.BitmapUtils;
import com.wotu.util.UtilsCom;

public abstract class ImageBase extends MediaObject {

    public static final int TYPE_THUMBNAIL = 1;
    public static final int TYPE_MICROTHUMBNAIL = 2;

    public static final int THUMBNAIL_TARGET_SIZE = 640;
    public static final int MICROTHUMBNAIL_TARGET_SIZE = 200;
    public static final int CACHED_IMAGE_QUALITY = 95;

    public static final int IMAGE_READY = 0;
    public static final int IMAGE_WAIT = 1;
    public static final int IMAGE_ERROR = -1;

    private static final int BYTESBUFFE_POOL_SIZE = 4;
    private static final int BYTESBUFFER_SIZE = 200 * 1024;

    private static final BitmapPool sMicroThumbPool = new BitmapPool(MICROTHUMBNAIL_TARGET_SIZE, MICROTHUMBNAIL_TARGET_SIZE, 16);
    private static final BitmapPool sThumbPool = new BitmapPool(4);
    private static final BytesBufferPool sMicroThumbBufferPool = new BytesBufferPool(BYTESBUFFE_POOL_SIZE, BYTESBUFFER_SIZE);

    public ImageBase(MediaPath path, long version) {
        super(path, version);
    }

    public static int getTargetSize(int type) {
        switch (type) {
        case TYPE_THUMBNAIL:
            return THUMBNAIL_TARGET_SIZE;
        case TYPE_MICROTHUMBNAIL:
            return MICROTHUMBNAIL_TARGET_SIZE;
        default:
            throw new RuntimeException(
                    "should only request thumb/microthumb from cache");
        }
    }

    protected abstract boolean updateFromCursor(Cursor cursor);

    protected void updateContent(Cursor cursor) {
        if (updateFromCursor(cursor)) {
            mDataVersion = nextVersionNumber();
        }
    }

    public static BitmapPool getMicroThumbPool() {
        return sMicroThumbPool;
    }

    public static BitmapPool getThumbPool() {
        return sThumbPool;
    }

    public static BytesBufferPool getBytesBufferPool() {
        return sMicroThumbBufferPool;
    }
}
