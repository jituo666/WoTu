package com.wotu.data;

import java.util.HashMap;
import java.util.WeakHashMap;

import com.wotu.app.WoTuApp;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

public class DataManager {

    private WoTuApp mWoTuApp;
    private HashMap<Uri, DataObserver> mNotifierMap = new HashMap<Uri, DataObserver>();
    private final Handler mMainHandler;

    public DataManager(WoTuApp app) {
        mWoTuApp = app;
        mMainHandler = new Handler(app.getMainLooper()); 
    }

    public void registerDataNotifier(Uri uri, DataNotifier notifier) {
        DataObserver observer = null;
        synchronized (mNotifierMap) {
            observer = mNotifierMap.get(uri);
            if (observer == null) {
                observer = new DataObserver(mMainHandler);
                mWoTuApp.getContentResolver().registerContentObserver(uri, true, observer);
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
