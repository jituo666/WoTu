package com.wotu.view.adapter;

import src.com.android.gallery3d.ui.AlbumSlidingWindow.AlbumEntry;
import android.graphics.Bitmap;
import android.os.Message;

import com.wotu.activity.WoTuContext;
import com.wotu.common.Future;
import com.wotu.common.FutureListener;
import com.wotu.common.JobLimiter;
import com.wotu.common.SynchronizedHandler;
import com.wotu.data.MediaItem;
import com.wotu.data.Path;
import com.wotu.data.image.BitmapLoader;
import com.wotu.data.load.AlbumDataLoader;
import com.wotu.utils.UtilsBase;
import com.wotu.view.opengl.Texture;
import com.wotu.view.opengl.TiledTexture;

/**
 * control the data window ,[activate Range], [content range]
 * 
 * @author jetoo
 * 
 */
public class AlbumDataWindow implements AlbumDataLoader.DataListener {

    private static final String TAG = "AlbumDataWindow";

    private static final int MSG_UPDATE_ENTRY = 0;
    private static final int JOB_LIMIT = 2;
    private int mActiveRequestCount = 0;
    private boolean mIsActive = false;

    private final AlbumDataLoader mDataSource;
    private final AlbumEntry mData[];
    private final SynchronizedHandler mHandler;
    private final JobLimiter mThreadPool;
    private final TiledTexture.Uploader mTileUploader;

    private int mSize;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    private DataListener mDataListener;

    public static interface DataListener {
        public void onSizeChanged(int size);

        public void onContentChanged();
    }

    public static class AlbumEntry {
        public MediaItem item;
        public Path path;
        public int rotation;
        public int mediaType;
        public boolean isWaitDisplayed;
        public TiledTexture bitmapTexture;
        public Texture content;
        private BitmapLoader contentLoader;
    }

    public AlbumDataWindow(WoTuContext context,
            AlbumDataLoader source, int cacheSize) {
        source.setDataListener(this);
        mDataSource = source;
        mData = new AlbumEntry[cacheSize];
        mSize = source.size();

        mHandler = new SynchronizedHandler(context.getGLController()) {
            @Override
            public void handleMessage(Message message) {
                UtilsBase.assertTrue(message.what == MSG_UPDATE_ENTRY);
                ((ThumbnailLoader) message.obj).updateEntry();
            }
        };

        mThreadPool = new JobLimiter(context.getThreadPool(), JOB_LIMIT);
        mTileUploader = new TiledTexture.Uploader(context.getGLController());
    }

    public void setListener(DataListener listener) {
        mDataListener = listener;
    }

    private void setContentWindow(int contentStart, int contentEnd) {
        if (contentStart == mContentStart && contentEnd == mContentEnd)
            return;

        if (!mIsActive) {
            mContentStart = contentStart;
            mContentEnd = contentEnd;
            mDataSource.setActiveWindow(contentStart, contentEnd);
            return;
        }

        if (contentStart >= mContentEnd || mContentStart >= contentEnd) {
            for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
                freeSlotContent(i);
            }
            mDataSource.setActiveWindow(contentStart, contentEnd);
            for (int i = contentStart; i < contentEnd; ++i) {
                prepareSlotContent(i);
            }
        } else {
            for (int i = mContentStart; i < contentStart; ++i) {
                freeSlotContent(i);
            }
            for (int i = contentEnd, n = mContentEnd; i < n; ++i) {
                freeSlotContent(i);
            }
            mDataSource.setActiveWindow(contentStart, contentEnd);
            for (int i = contentStart, n = mContentStart; i < n; ++i) {
                prepareSlotContent(i);
            }
            for (int i = mContentEnd; i < contentEnd; ++i) {
                prepareSlotContent(i);
            }
        }

        mContentStart = contentStart;
        mContentEnd = contentEnd;
    }

    public void setActiveWindow(int start, int end) {
        if (!(start <= end && end - start <= mData.length && end <= mSize)) {
            UtilsBase.fail("%s, %s, %s, %s", start, end, mData.length, mSize);
        }
        AlbumEntry data[] = mData;

        mActiveStart = start;
        mActiveEnd = end;

        int contentStart = UtilsBase.clamp((start + end) / 2 - data.length / 2,
                0, Math.max(0, mSize - data.length));
        int contentEnd = Math.min(contentStart + data.length, mSize);
        setContentWindow(contentStart, contentEnd);
        updateTextureUploadQueue();
        if (mIsActive)
            updateAllImageRequests();
    }

    public AlbumEntry get(int slotIndex) {
        if (!isActiveSlot(slotIndex)) {
            UtilsBase.fail("invalid slot: %s outsides (%s, %s)",
                    slotIndex, mActiveStart, mActiveEnd);
        }
        return mData[slotIndex % mData.length];
    }

    public boolean isActiveSlot(int slotIndex) {
        return slotIndex >= mActiveStart && slotIndex < mActiveEnd;
    }

    private void uploadBgTextureInSlot(int index) {
        if (index < mContentEnd && index >= mContentStart) {
            AlbumEntry entry = mData[index % mData.length];
            if (entry.bitmapTexture != null) {
                mTileUploader.addTexture(entry.bitmapTexture);
            }
        }
    }

    private void updateTextureUploadQueue() {
        if (!mIsActive)
            return;
        mTileUploader.clear();

        // add foreground textures
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            AlbumEntry entry = mData[i % mData.length];
            if (entry.bitmapTexture != null) {
                mTileUploader.addTexture(entry.bitmapTexture);
            }
        }

        // add background textures
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            uploadBgTextureInSlot(mActiveEnd + i);
            uploadBgTextureInSlot(mActiveStart - i - 1);
        }
    }

    // We would like to request non active slots in the following order:
    // Order:    8 6 4 2                   1 3 5 7
    //         |---------|---------------|---------|
    //                   |<-  active  ->|
    //         |<-------- cached range ----------->|
    private void requestNonactiveImages() {
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            requestSlotImage(mActiveEnd + i);
            requestSlotImage(mActiveStart - 1 - i);
        }
    }

    private void cancelNonactiveImages() {
        int range = Math.max(
                (mContentEnd - mActiveEnd), (mActiveStart - mContentStart));
        for (int i = 0; i < range; ++i) {
            cancelSlotImage(mActiveEnd + i);
            cancelSlotImage(mActiveStart - 1 - i);
        }
    }

    // return whether the request is in progress or not
    private boolean requestSlotImage(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd)
            return false;
        AlbumEntry entry = mData[slotIndex % mData.length];
        if (entry.content != null || entry.item == null)
            return false;
        entry.contentLoader.startLoad();
        return entry.contentLoader.isRequestInProgress();
    }

    private void cancelSlotImage(int slotIndex) {
        if (slotIndex < mContentStart || slotIndex >= mContentEnd)
            return;
        AlbumEntry item = mData[slotIndex % mData.length];
        if (item.contentLoader != null)
            item.contentLoader.cancelLoad();
    }

    private void prepareSlotContent(int slotIndex) {
        AlbumEntry entry = new AlbumEntry();
        MediaItem item = mDataSource.get(slotIndex); // item could be null;
        entry.item = item;
        entry.mediaType = (item == null)
                ? MediaItem.MEDIA_TYPE_UNKNOWN
                : entry.item.getMediaType();
        entry.path = (item == null) ? null : item.getPath();
        entry.rotation = (item == null) ? 0 : item.getRotation();
        entry.contentLoader = new ThumbnailLoader(slotIndex, entry.item);
        mData[slotIndex % mData.length] = entry;
    }

    private void freeSlotContent(int slotIndex) {
        AlbumEntry data[] = mData;
        int index = slotIndex % data.length;
        AlbumEntry entry = data[index];
        if (entry.contentLoader != null)
            entry.contentLoader.recycle();
        if (entry.bitmapTexture != null)
            entry.bitmapTexture.recycle();
        data[index] = null;
    }

    private void updateAllImageRequests() {
        mActiveRequestCount = 0;
        for (int i = mActiveStart, n = mActiveEnd; i < n; ++i) {
            if (requestSlotImage(i))
                ++mActiveRequestCount;
        }
        if (mActiveRequestCount == 0) {
            requestNonactiveImages();
        } else {
            cancelNonactiveImages();
        }
    }

    /*
     * 位图/缩略图加载
     */
    private class ThumbnailLoader extends BitmapLoader {
        private final int mSlotIndex;
        private final MediaItem mItem;

        public ThumbnailLoader(int slotIndex, MediaItem item) {
            mSlotIndex = slotIndex;
            mItem = item;
        }

        @Override
        protected Future<Bitmap> submitBitmapTask(FutureListener<Bitmap> l) {
            return mThreadPool.submit(
                    mItem.requestImage(MediaItem.TYPE_MICROTHUMBNAIL), this);
        }

        @Override
        protected void onLoadComplete(Bitmap bitmap) {
            mHandler.obtainMessage(MSG_UPDATE_ENTRY, this).sendToTarget();
        }

        public void updateEntry() {
            Bitmap bitmap = getBitmap();
            if (bitmap == null)
                return; // error or recycled
            AlbumEntry entry = mData[mSlotIndex % mData.length];
            entry.bitmapTexture = new TiledTexture(bitmap);
            entry.content = entry.bitmapTexture;

            if (isActiveSlot(mSlotIndex)) {
                mTileUploader.addTexture(entry.bitmapTexture);
                --mActiveRequestCount;
                if (mActiveRequestCount == 0)
                    requestNonactiveImages();
                if (mDataListener != null)
                    mDataListener.onContentChanged();
            } else {
                mTileUploader.addTexture(entry.bitmapTexture);
            }
        }

        @Override
        protected void recycleBitmap(Bitmap bitmap) {
            // TODO Auto-generated method stub

        }
    }

    public void resume() {
        mIsActive = true;
        TiledTexture.prepareResources();
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            prepareSlotContent(i);
        }
        updateAllImageRequests(); // Frist start no use, just for backing from other
    }

    public void pause() {
        mIsActive = false;
        mTileUploader.clear();
        TiledTexture.freeResources();
        for (int i = mContentStart, n = mContentEnd; i < n; ++i) {
            freeSlotContent(i);
        }
    }

    @Override
    public void onContentChanged(int index) {

    }

    @Override
    public void onSizeChanged(int size) {

    }
}
