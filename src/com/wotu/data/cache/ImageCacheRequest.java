package com.wotu.data.cache;

import com.wotu.app.WoTuApp;
import com.wotu.common.BytesBufferPool.BytesBuffer;
import com.wotu.common.ThreadPool.Job;
import com.wotu.common.ThreadPool.JobContext;
import com.wotu.data.MediaItem;
import com.wotu.data.Path;
import com.wotu.data.utils.BitmapUtils;
import com.wotu.data.utils.DecodeUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


public abstract class ImageCacheRequest implements Job<Bitmap> {
    private static final String TAG = "ImageCacheRequest";

    protected WoTuApp mApplication;
    private Path mPath;
    private int mType;
    private int mTargetSize;
    private long mTimeModified;

    public ImageCacheRequest(WoTuApp application,
            Path path, long timeModified, int type, int targetSize) {
        mApplication = application;
        mPath = path;
        mType = type;
        mTargetSize = targetSize;
        mTimeModified = timeModified;
    }

    private String debugTag() {
        return mPath + "," + mTimeModified + "," +
                ((mType == MediaItem.TYPE_THUMBNAIL) ? "THUMB" :
                (mType == MediaItem.TYPE_MICROTHUMBNAIL) ? "MICROTHUMB" : "?");
    }

    @Override
    public Bitmap run(JobContext jc) {
        ImageCacheService cacheService = mApplication.getImageCacheService();

        BytesBuffer buffer = MediaItem.getBytesBufferPool().get();
        try {
            boolean found = cacheService.getImageData(mPath, mTimeModified, mType, buffer);
            if (jc.isCancelled()) return null;
            if (found) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap;
                if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
                    bitmap = DecodeUtils.decodeUsingPool(jc,
                            buffer.data, buffer.offset, buffer.length, options);
                } else {
                    bitmap = DecodeUtils.decodeUsingPool(jc,
                            buffer.data, buffer.offset, buffer.length, options);
                }
                if (bitmap == null && !jc.isCancelled()) {
                    Log.w(TAG, "decode cached failed " + debugTag());
                }
                return bitmap;
            }
        } finally {
            MediaItem.getBytesBufferPool().recycle(buffer);
        }
        Bitmap bitmap = onDecodeOriginal(jc, mType);
        if (jc.isCancelled()) return null;

        if (bitmap == null) {
            Log.w(TAG, "decode orig failed " + debugTag());
            return null;
        }

        if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
            bitmap = BitmapUtils.resizeAndCropCenter(bitmap, mTargetSize, true);
        } else {
            bitmap = BitmapUtils.resizeDownBySideLength(bitmap, mTargetSize, true);
        }
        if (jc.isCancelled()) return null;

        byte[] array = BitmapUtils.compressToBytes(bitmap);
        if (jc.isCancelled()) return null;

        cacheService.putImageData(mPath, mTimeModified, mType, array);
        return bitmap;
    }

    public abstract Bitmap onDecodeOriginal(JobContext jc, int targetSize);
}
