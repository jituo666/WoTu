package com.wotu.data.cache;

import android.content.Context;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.wotu.common.BytesBufferPool.BytesBuffer;
import com.wotu.data.Path;
import com.wotu.data.cache.BlobCache.LookupRequest;
import com.wotu.utils.UtilsBase;
import com.wotu.utils.UtilsCom;

public class ImageCacher {
    @SuppressWarnings("unused")
    private static final String TAG = "ImageCacheService";

    private static final String IMAGE_CACHE_FILE = "imgcache";
    private static final int IMAGE_CACHE_MAX_ENTRIES = 5000;
    private static final int IMAGE_CACHE_MAX_BYTES = 200 * 1024 * 1024;
    private static final int IMAGE_CACHE_VERSION = 4;

    private BlobCache mCache;

    public ImageCacher(Context context) {
        mCache = CacheManager.getCache(context, IMAGE_CACHE_FILE,
                IMAGE_CACHE_MAX_ENTRIES, IMAGE_CACHE_MAX_BYTES,
                IMAGE_CACHE_VERSION);
    }

    /**
     * Gets the cached image data for the given <code>path</code> and <code>type</code>.
     *
     * The image data will be stored in <code>buffer.data</code>, started from
     * <code>buffer.offset</code> for <code>buffer.length</code> bytes. If the
     * buffer.data is not big enough, a new byte array will be allocated and returned.
     *
     * @return true if the image data is found; false if not found.
     */
    public boolean getImageData(Path path, int type, BytesBuffer buffer) {
        byte[] key = makeKey(path, type);
        long cacheKey = UtilsBase.crc64Long(key);
        try {
            LookupRequest request = new LookupRequest();
            request.key = cacheKey;
            request.buffer = buffer.data;
            synchronized (mCache) {
                if (!mCache.lookup(request)) return false;
            }
            if (isSameKey(key, request.buffer)) {
                buffer.data = request.buffer;
                buffer.offset = key.length;
                buffer.length = request.length - buffer.offset;
                return true;
            }
        } catch (IOException ex) {
            // ignore.
        }
        return false;
    }

    public void putImageData(Path path, int type, byte[] value) {
        byte[] key = makeKey(path, type);
        long cacheKey = UtilsBase.crc64Long(key);
        ByteBuffer buffer = ByteBuffer.allocate(key.length + value.length);
        buffer.put(key);
        buffer.put(value);
        synchronized (mCache) {
            try {
                mCache.insert(cacheKey, buffer.array());
            } catch (IOException ex) {
                // ignore.
            }
        }
    }

    private static byte[] makeKey(Path path, int type) {
        return UtilsCom.getBytes(path.toString() + "+" + type);
    }

    private static boolean isSameKey(byte[] key, byte[] buffer) {
        int n = key.length;
        if (buffer.length < n) {
            return false;
        }
        for (int i = 0; i < n; ++i) {
            if (key[i] != buffer[i]) {
                return false;
            }
        }
        return true;
    }
}
