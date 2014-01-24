package com.wotu.view.adapter;

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

public class AlbumDataWindow implements AlbumDataLoader.DataListener {

    private static final String TAG = "AlbumDataWindow";

    private static final int MSG_UPDATE_ENTRY = 0;
    private static final int JOB_LIMIT = 2;
    private int mActiveRequestCount = 0;
    private boolean mIsActive = false;

    public static class AlbumEntry {
        public MediaItem item;
        public Path path;
        public boolean isPanorama;
        public int rotation;
        public int mediaType;
        public boolean isWaitDisplayed;
        public TiledTexture bitmapTexture;
        public Texture content;
        private BitmapLoader contentLoader;
    }

    private final AlbumDataLoader mSource;
    private final AlbumEntry mData[];
    private final SynchronizedHandler mHandler;
    private final JobLimiter mThreadPool;
    private final TiledTexture.Uploader mTileUploader;

    private int mSize;

    private int mContentStart = 0;
    private int mContentEnd = 0;

    private int mActiveStart = 0;
    private int mActiveEnd = 0;

    private Listener mListener;

    public static interface Listener {
        public void onSizeChanged(int size);

        public void onContentChanged();
    }

    public AlbumDataWindow(WoTuContext context,
            AlbumDataLoader source, int cacheSize) {
        source.setDataListener(this);
        mSource = source;
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

    @Override
    public void onContentChanged(int index) {

    }

    @Override
    public void onSizeChanged(int size) {

    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public boolean isActiveSlot(int slotIndex) {
        return slotIndex >= mActiveStart && slotIndex < mActiveEnd;
    }

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
                if (mListener != null)
                    mListener.onContentChanged();
            } else {
                mTileUploader.addTexture(entry.bitmapTexture);
            }
        }

        @Override
        protected void recycleBitmap(Bitmap bitmap) {
            // TODO Auto-generated method stub
            
        }
    }
}
