
package com.wotu.data;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;

import com.wotu.app.WoTuApp;
import com.wotu.common.WLog;
import com.wotu.data.source.LocalSource;

import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

public class DataManager {

    private static final String TAG = "DataManager";

    private static final String ACTION_DELETE_PICTURE = "com.android.gallery3d.action.DELETE_PICTURE";

    public final static Object LOCK = new Object();
    private WoTuApp mApplication;
    private HashMap<String, MediaSource> mSourceMap = new LinkedHashMap<String, MediaSource>();
    private HashMap<Uri, DataObserver> mNotifierMap = new HashMap<Uri, DataObserver>();
    private final Handler mMainHandler;
    private int mActiveCount = 0;

    public DataManager(WoTuApp app) {
        mApplication = app;
        mMainHandler = new Handler(app.getMainLooper());
    }

    private void addSource(MediaSource source) {
        mSourceMap.put(source.getPrefix(), source);
    }

    public synchronized void initializeSourceMap() {
        if (!mSourceMap.isEmpty())
            return;

        // the order matters, the UriSource must come last
        addSource(new LocalSource(mApplication));
        /*
         * addSource(new PicasaSource(mApplication)); addSource(new
         * MtpSource(mApplication)); addSource(new ComboSource(mApplication));
         * addSource(new ClusterSource(mApplication)); addSource(new
         * FilterSource(mApplication)); addSource(new UriSource(mApplication));
         * addSource(new SnailSource(mApplication));
         */

        if (mActiveCount > 0) {
            for (MediaSource source : mSourceMap.values()) {
                source.resume();
            }
        }
    }

    public MediaObject getMediaObject(Path path) {
        MediaObject obj = path.getObject();
        if (obj != null)
            return obj;
        MediaSource source = mSourceMap.get(path.getPrefix());
        if (source == null) {
            WLog.w(TAG, "cannot find media source for path: " + path);
            return null;
        }

        try {
            MediaObject object = source.createMediaObject(path);
            if (object == null) {
                WLog.w(TAG, "cannot create media object: " + path);
            }
            return object;
        } catch (Throwable t) {
            WLog.w(TAG, "exception in creating media object: " + path, t);
            return null;
        }
    }

    public MediaObject getMediaObject(String prefix, int identity) {
        return getMediaObject(new Path(prefix, identity));
    }

    public MediaSet getMediaSet(Path path) {
        return (MediaSet) getMediaObject(path);
    }

    public MediaSet getMediaSet(String prefix, int identity) {
        return (MediaSet) getMediaObject(prefix, identity);
    }

    public MediaSet[] getMediaSetsFromString(String prefix) {
        String segment[] = prefix.split("\\");
        if (segment.length != 2)
            return new MediaSet[0];
        MediaSet[] sets = new MediaSet[segment.length];
        return sets;
    }

    public void resume() {
        if (++mActiveCount == 1) {
            for (MediaSource source : mSourceMap.values()) {
                source.resume();
            }
        }
    }

    public void pause() {
        if (--mActiveCount == 0) {
            for (MediaSource source : mSourceMap.values()) {
                source.pause();
            }
        }
    }

    public void broadcastLocalDeletion() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(
                mApplication.getAndroidContext());
        Intent intent = new Intent(ACTION_DELETE_PICTURE);
        manager.sendBroadcast(intent);
    }

    public void registerDataNotifier(Uri uri, DataNotifier notifier) {
        DataObserver observer = null;
        synchronized (mNotifierMap) {
            observer = mNotifierMap.get(uri);
            if (observer == null) {
                observer = new DataObserver(mMainHandler);
                mApplication.getContentResolver().registerContentObserver(uri, true, observer);
                mNotifierMap.put(uri, observer);
            }
        }
        observer.registerNotifier(notifier);
    }

    private static class DataObserver extends ContentObserver {
        private WeakHashMap<DataNotifier, Object> mNotifiers = new WeakHashMap<DataNotifier, Object>();

        public DataObserver(Handler handler) {
            super(handler);
        }

        public synchronized void registerNotifier(DataNotifier notifier) {
            mNotifiers.put(notifier, null);
        }

        @Override
        public synchronized void onChange(boolean selfChange) {
            for (DataNotifier notifier : mNotifiers.keySet()) {
                notifier.onChange(selfChange);
            }
        }
    }
}
