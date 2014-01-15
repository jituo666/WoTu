package com.xjt.wotu.data;

import android.net.Uri;

import com.xjt.wotu.app.WoTuApp;

import java.util.concurrent.atomic.AtomicBoolean;

// This handles change notification for media sets.
public class ChangeNotifier {

    private MediaSet mMediaSet;
    private AtomicBoolean mContentDirty = new AtomicBoolean(true);

    public ChangeNotifier(MediaSet set, Uri uri, WoTuApp application) {
        mMediaSet = set;
        application.getDataManager().registerChangeNotifier(uri, this);
    }

    // Returns the dirty flag and clear it.
    public boolean isDirty() {
        return mContentDirty.compareAndSet(true, false);
    }

    // For debugging only.
    public void fakeChange() {
        onChange(false);
    }

    protected void onChange(boolean selfChange) {
        if (mContentDirty.compareAndSet(false, true)) {
            mMediaSet.notifyContentChanged();
        }
    }
}