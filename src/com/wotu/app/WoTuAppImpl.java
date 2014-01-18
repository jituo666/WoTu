package com.wotu.app;

import com.wotu.common.ThreadPool;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;

public class WoTuAppImpl extends Application implements WoTuApp {

    private DataManager mDataManager;
    private ThreadPool mThreadPool;

    @Override
    public synchronized DataManager getDataManager() {
        if (mDataManager == null) {
            mDataManager = new DataManager(this);
        }
        return mDataManager;
    }

    @Override
    public ContentResolver getContentResolver() {
        return getContentResolver();
    }

    @Override
    public Resources getResources() {
        return getResources();
    }

    @Override
    public Looper getMainLooper() {
        return getMainLooper();
    }

    @Override
    public synchronized ThreadPool getThreadPool() {
        if (mThreadPool == null) {
            mThreadPool = new ThreadPool();
        }
        return mThreadPool;
    }

    @Override
    public Context getAndroidContext() {
        return getAndroidContext();
    }
}