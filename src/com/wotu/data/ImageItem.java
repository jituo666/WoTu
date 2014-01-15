package com.wotu.data;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;

import com.wotu.common.BytesBufferPool;
import com.wotu.common.ThreadPool.Job;
import com.wotu.data.bitmap.BitmapPool;

public abstract class ImageItem extends MediaObject {

    // These are the bits returned from getSupportedOperations():
    public static final int SUPPORT_DELETE = 1 << 0;
    public static final int SUPPORT_ROTATE = 1 << 1;
    public static final int SUPPORT_SHARE = 1 << 2;
    public static final int SUPPORT_CROP = 1 << 3;
    public static final int SUPPORT_SHOW_ON_MAP = 1 << 4;
    public static final int SUPPORT_SETAS = 1 << 5;
    public static final int SUPPORT_FULL_IMAGE = 1 << 6;
    public static final int SUPPORT_CACHE = 1 << 7;
    public static final int SUPPORT_EDIT = 1 << 8;
    public static final int SUPPORT_INFO = 1 << 9;
    public static final int SUPPORT_IMPORT = 1 << 10;
    public static final int SUPPORT_ALL = 0xffffffff;

    // NOTE: These type numbers are stored in the image cache, so it should not
    // not be changed without resetting the cache.
    public static final int TYPE_THUMBNAIL = 1;
    public static final int TYPE_MICROTHUMBNAIL = 2;

    public static final int THUMBNAIL_TARGET_SIZE = 640;
    public static final int MICROTHUMBNAIL_TARGET_SIZE = 200;
    public static final int CACHED_IMAGE_QUALITY = 95;

    public static final int IMAGE_READY = 0;
    public static final int IMAGE_WAIT = 1;
    public static final int IMAGE_ERROR = -1;

    public static final String MIME_TYPE_JPEG = "image/jpeg";

    private static final int BYTESBUFFE_POOL_SIZE = 4;
    private static final int BYTESBUFFER_SIZE = 200 * 1024;

    private static final BitmapPool sMicroThumbPool = new BitmapPool(MICROTHUMBNAIL_TARGET_SIZE, MICROTHUMBNAIL_TARGET_SIZE, 16);
    private static final BitmapPool sThumbPool = new BitmapPool(4);
    private static final BytesBufferPool sMicroThumbBufferPool = new BytesBufferPool(BYTESBUFFE_POOL_SIZE, BYTESBUFFER_SIZE);

    // TODO: fix default value for latlng and change this.
    public static final double INVALID_LATLNG = 0f;

    public ImageItem(MediaPath path, long version) {
        super(path, version);
    }

    public abstract Job<Bitmap> requestThumnail(int type);

    public abstract Job<BitmapRegionDecoder> requestImage();

    public void rotate(int degrees) {
        throw new UnsupportedOperationException();
    }

    public long getDateInMs() {
        return 0;
    }

    public String getName() {
        return null;
    }

    public void getLatLong(double[] latLong) {
        latLong[0] = INVALID_LATLNG;
        latLong[1] = INVALID_LATLNG;
    }

    public String[] getTags() {
        return null;
    }

    public ImageDetails getDetails() {
        ImageDetails details = new ImageDetails();
        return details;
    }

    // The rotation of the full-resolution image. By default, it returns the value of
    // getRotation().
    public int getFullImageRotation() {
        return getRotation();
    }

    public int getRotation() {
        return 0;
    }

    public long getSize() {
        return 0;
    }

    public abstract String getMimeType();

    // Returns width and height of the media item.
    // Returns 0, 0 if the information is not available.
    public abstract int getWidth();

    public abstract int getHeight();

    abstract protected boolean updateFromCursor(Cursor cursor);

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
