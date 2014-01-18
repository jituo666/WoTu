package com.wotu.data;

import android.net.Uri;

import com.wotu.app.WoTuApp;

import java.util.concurrent.atomic.AtomicBoolean;

public class DataNotifier {

    private MediaSet mMediaSet;
    private AtomicBoolean mContentDirty = new AtomicBoolean(true);

    public DataNotifier(MediaSet set, Uri uri, WoTuApp app) {
        mMediaSet = set;
        app.getDataManager().registerDataNotifier(uri, this);
    }

    public boolean isDirty() {
        return mContentDirty.compareAndSet(true, false);
    }

    public void fakeChange() {
        onChange(false);
    }

    public void onChange(boolean selfChange) {
        if (mContentDirty.compareAndSet(false, true)) {
            mMediaSet.notifyContentChanged();
        }
    }
}