package com.wotu.data.bitmap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import com.wotu.common.WLog;
import com.wotu.common.ThreadPool.JobContext;
import com.wotu.util.UtilsBase;

import java.io.FileDescriptor;
import java.util.ArrayList;

public class BitmapPool {
    private static final String TAG = "BitmapPool";

    private final ArrayList<Bitmap> mPool;
    private final int mPoolLimit;

    // mOneSize is true if the pool can only cache Bitmap with one size.
    private final boolean mOneSize;
    private final int mWidth, mHeight;  // only used if mOneSize is true

    // Construct a BitmapPool which caches bitmap with the specified size.
    public BitmapPool(int width, int height, int poolLimit) {
        mWidth = width;
        mHeight = height;
        mPoolLimit = poolLimit;
        mPool = new ArrayList<Bitmap>(poolLimit);
        mOneSize = true;
    }

    // Construct a BitmapPool which caches bitmap with any size;
    public BitmapPool(int poolLimit) {
        mWidth = -1;
        mHeight = -1;
        mPoolLimit = poolLimit;
        mPool = new ArrayList<Bitmap>(poolLimit);
        mOneSize = false;
    }

    // Get a Bitmap from the pool.
    public synchronized Bitmap getBitmap() {
        UtilsBase.assertTrue(mOneSize);
        int size = mPool.size();
        return size > 0 ? mPool.remove(size - 1) : null;
    }

    // Get a Bitmap from the pool with the specified size.
    public synchronized Bitmap getBitmap(int width, int height) {
        UtilsBase.assertTrue(!mOneSize);
        for (int i = mPool.size() - 1; i >= 0; i--) {
            Bitmap b = mPool.get(i);
            if (b.getWidth() == width && b.getHeight() == height) {
                return mPool.remove(i);
            }
        }
        return null;
    }

    // Put a Bitmap into the pool, if the Bitmap has a proper size. Otherwise
    // the Bitmap will be recycled. If the pool is full, an old Bitmap will be
    // recycled.
    public void recycle(Bitmap bitmap) {
        if (bitmap == null) return;
        if (mOneSize && ((bitmap.getWidth() != mWidth) ||
                (bitmap.getHeight() != mHeight))) {
            bitmap.recycle();
            return;
        }
        synchronized (this) {
            if (mPool.size() >= mPoolLimit) mPool.remove(0);
            mPool.add(bitmap);
        }
    }

    public synchronized void clear() {
        mPool.clear();
    }

    private Bitmap findCachedBitmap(JobContext jc,
            byte[] data, int offset, int length, Options options) {
        if (mOneSize) return getBitmap();
        BitmapDecoder.decodeBounds(jc, data, offset, length, options);
        return getBitmap(options.outWidth, options.outHeight);
    }

    private Bitmap findCachedBitmap(JobContext jc,
            FileDescriptor fileDescriptor, Options options) {
        if (mOneSize) return getBitmap();
        BitmapDecoder.decodeBounds(jc, fileDescriptor, options);
        return getBitmap(options.outWidth, options.outHeight);
    }

    public Bitmap decode(JobContext jc,
            byte[] data, int offset, int length, BitmapFactory.Options options) {
        if (options == null) options = new BitmapFactory.Options();
        if (options.inSampleSize < 1) options.inSampleSize = 1;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inBitmap = (options.inSampleSize == 1)
                ? findCachedBitmap(jc, data, offset, length, options) : null;
        try {
            Bitmap bitmap = BitmapDecoder.decode(jc, data, offset, length, options);
            if (options.inBitmap != null && options.inBitmap != bitmap) {
                recycle(options.inBitmap);
                options.inBitmap = null;
            }
            return bitmap;
        } catch (IllegalArgumentException e) {
            if (options.inBitmap == null) throw e;

            WLog.w(TAG, "decode fail with a given bitmap, try decode to a new bitmap");
            recycle(options.inBitmap);
            options.inBitmap = null;
            return BitmapDecoder.decode(jc, data, offset, length, options);
        }
    }

    // This is the same as the method above except the source data comes
    // from a file descriptor instead of a byte array.
    public Bitmap decode(JobContext jc,
            FileDescriptor fileDescriptor, Options options) {
        if (options == null) options = new BitmapFactory.Options();
        if (options.inSampleSize < 1) options.inSampleSize = 1;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inBitmap = (options.inSampleSize == 1)
                ? findCachedBitmap(jc, fileDescriptor, options) : null;
        try {
            Bitmap bitmap = BitmapDecoder.decode(jc, fileDescriptor, options);
            if (options.inBitmap != null&& options.inBitmap != bitmap) {
                recycle(options.inBitmap);
                options.inBitmap = null;
            }
            return bitmap;
        } catch (IllegalArgumentException e) {
            if (options.inBitmap == null) throw e;

            WLog.w(TAG, "decode fail with a given bitmap, try decode to a new bitmap");
            recycle(options.inBitmap);
            options.inBitmap = null;
            return BitmapDecoder.decode(jc, fileDescriptor, options);
        }
    }
}
