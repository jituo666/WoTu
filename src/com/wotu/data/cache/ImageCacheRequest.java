package com.wotu.data.cache;

import com.wotu.app.WoTuApp;
import com.wotu.common.BytesBufferPool.BytesBuffer;
import com.wotu.common.ThreadPool.Job;
import com.wotu.common.ThreadPool.JobContext;
import com.wotu.common.WLog;
import com.wotu.data.MediaItem;
import com.wotu.data.Path;
import com.wotu.data.utils.BitmapUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public abstract class ImageCacheRequest implements Job<Bitmap> {
    private static final String TAG = "ImageCacheRequest";

    protected WoTuApp mApplication;
    private Path mPath;
    private int mType;
    private int mTargetSize;

    public ImageCacheRequest(WoTuApp application,
            Path path, int type, int targetSize) {
        mApplication = application;
        mPath = path;
        mType = type;
        mTargetSize = targetSize;
    }

    @Override
    public Bitmap run(JobContext jc) {
        String debugTag = mPath + "," +
                 ((mType == MediaItem.TYPE_THUMBNAIL) ? "THUMB" :
                 (mType == MediaItem.TYPE_MICROTHUMBNAIL) ? "MICROTHUMB" : "?");
        ImageCacheService cacheService = mApplication.getImageCacheService();

        BytesBuffer buffer = MediaItem.getBytesBufferPool().get();
        try {
            boolean found = cacheService.getImageData(mPath, mType, buffer);
            if (jc.isCancelled()) return null;
            if (found) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap;
                if (mType == MediaItem.TYPE_MICROTHUMBNAIL) {
                    bitmap = MediaItem.getMicroThumbPool().decode(jc,
                            buffer.data, buffer.offset, buffer.length, options);
                } else {
                    bitmap = MediaItem.getThumbPool().decode(jc,
                            buffer.data, buffer.offset, buffer.length, options);
                }
                if (bitmap == null && !jc.isCancelled()) {
                    WLog.w(TAG, "decode cached failed " + debugTag);
                }
                //WLog.i("albumTag", "-------------------------------------ImageCacheRequest:----1-found" + mPath);
                return bitmap;
            }
        } finally {
            MediaItem.getBytesBufferPool().recycle(buffer);
        }
        Bitmap bitmap = onDecodeOriginal(jc, mType);
        if (jc.isCancelled()) return null;

        if (bitmap == null) {
            WLog.w(TAG, "decode orig failed " + debugTag);
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

        cacheService.putImageData(mPath, mType, array);
        return bitmap;
    }

    public abstract Bitmap onDecodeOriginal(JobContext jc, int targetSize);
}
