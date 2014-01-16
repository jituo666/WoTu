package com.wotu.data;

import com.wotu.common.Future;
import com.wotu.common.WLog;
import com.wotu.util.UtilsBase;

import java.util.WeakHashMap;

// MediaSet is a directory-like data structure.
// It contains MediaItems and sub-MediaSets.
//
// The primary interface are:
// getMediaItemCount(), getMediaItem() and
// getSubMediaSetCount(), getSubMediaSet().
//
// getTotalMediaItemCount() returns the number of all MediaItems, including
// those in sub-MediaSets.
public abstract class MediaSetObject extends MediaObject {

    private static final String TAG = "MediaSet";

    public static final int MEDIAITEM_BATCH_FETCH_COUNT = 500;
    public static final int INDEX_NOT_FOUND = -1;

    public static final int SYNC_RESULT_SUCCESS = 0;
    public static final int SYNC_RESULT_CANCELLED = 1;
    public static final int SYNC_RESULT_ERROR = 2;

    /** Listener to be used with requestSync(SyncListener). */
    public static interface SyncListener {
        /**
         * Called when the sync task completed. Completion may be due to normal
         * termination, an exception, or cancellation.
         * 
         * @param mediaSet
         *            the MediaSet that's done with sync
         * @param resultCode
         *            one of the SYNC_RESULT_* constants
         */
        void onSyncDone(MediaSetObject mediaSet, int resultCode);
    }

    public MediaSetObject(MediaPath path, long version) {
        super(path, version);
    }

    public int getMediaItemCount() {
        return 0;
    }

    public boolean isLoading() {
        return false;
    }

    public abstract String getName();

    private WeakHashMap<ContentListener, Object> mListeners = new WeakHashMap<ContentListener, Object>();

    public void addContentListener(ContentListener listener) {
        if (mListeners.containsKey(listener)) {
            throw new IllegalArgumentException();
        }
        mListeners.put(listener, null);
    }

    public void removeContentListener(ContentListener listener) {
        if (!mListeners.containsKey(listener)) {
            throw new IllegalArgumentException();
        }
        mListeners.remove(listener);
    }

    public void notifyContentChanged() {
        for (ContentListener listener : mListeners.keySet()) {
            listener.onContentDirty();
        }
    }

    public abstract long reload();

    /**
     * Requests sync on this MediaSet. It returns a Future object that can be
     * used by the caller to query the status of the sync. The sync result code
     * is one of the SYNC_RESULT_* constants defined in this class and can be
     * obtained by Future.get().
     * 
     * Subclasses should perform sync on a different thread.
     * 
     * The default implementation here returns a Future stub that does nothing
     * and returns SYNC_RESULT_SUCCESS by get().
     */
    public Future<Integer> requestSync(SyncListener listener) {
        listener.onSyncDone(this, SYNC_RESULT_SUCCESS);
        return FUTURE_STUB;
    }

    private static final Future<Integer> FUTURE_STUB = new Future<Integer>() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Integer get() {
            return SYNC_RESULT_SUCCESS;
        }

        @Override
        public void waitDone() {
        }
    };

    protected Future<Integer> requestSyncOnMultipleSets(MediaSetObject[] sets, SyncListener listener) {
        return new MultiSetSyncFuture(sets, listener);
    }

    private class MultiSetSyncFuture implements Future<Integer>, SyncListener {
        private static final String TAG = "Gallery.MultiSetSync";

        private final SyncListener mListener;
        private final Future<Integer> mFutures[];

        private boolean mIsCancelled = false;
        private int mResult = -1;
        private int mPendingCount;

        @SuppressWarnings("unchecked")
        MultiSetSyncFuture(MediaSetObject[] sets, SyncListener listener) {
            mListener = listener;
            mPendingCount = sets.length;
            mFutures = new Future[sets.length];

            synchronized (this) {
                for (int i = 0, n = sets.length; i < n; ++i) {
                    mFutures[i] = sets[i].requestSync(this);
                    WLog.d(TAG, "  request sync: " + UtilsBase.maskDebugInfo(sets[i].getName()));
                }
            }
        }

        @Override
        public synchronized void cancel() {
            if (mIsCancelled)
                return;
            mIsCancelled = true;
            for (Future<Integer> future : mFutures)
                future.cancel();
            if (mResult < 0)
                mResult = SYNC_RESULT_CANCELLED;
        }

        @Override
        public synchronized boolean isCancelled() {
            return mIsCancelled;
        }

        @Override
        public synchronized boolean isDone() {
            return mPendingCount == 0;
        }

        @Override
        public synchronized Integer get() {
            waitDone();
            return mResult;
        }

        @Override
        public synchronized void waitDone() {
            try {
                while (!isDone())
                    wait();
            } catch (InterruptedException e) {
                WLog.d(TAG, "waitDone() interrupted");
            }
        }

        // SyncListener callback
        @Override
        public void onSyncDone(MediaSetObject mediaSet, int resultCode) {
            SyncListener listener = null;
            synchronized (this) {
                if (resultCode == SYNC_RESULT_ERROR)
                    mResult = SYNC_RESULT_ERROR;
                --mPendingCount;
                if (mPendingCount == 0) {
                    listener = mListener;
                    notifyAll();
                }
                WLog.d(TAG, "onSyncDone: " + UtilsBase.maskDebugInfo(mediaSet.getName())
                        + " #pending=" + mPendingCount);
            }
            if (listener != null)
                listener.onSyncDone(MediaSetObject.this, mResult);
        }
    }
}
