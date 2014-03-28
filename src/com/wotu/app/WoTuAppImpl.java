package com.wotu.app;

import com.wotu.common.ThreadPool;
import com.wotu.data.DataManager;
import com.wotu.data.cache.ImageCacheService;
import com.wotu.utils.UtilsCom;

import android.app.Application;
import android.content.Context;

public class WoTuAppImpl extends Application implements WoTuApp {

    private Object mLock = new Object();
    private ImageCacheService mImageCacheService;
    private DataManager mDataManager;
    private ThreadPool mThreadPool;

    @Override
    public void onCreate() {
        UtilsCom.initialize(this);
        // TODO 
        //initializeAsyncTask();
        UtilsCom.initialize(this);
        /*WidgetUtils.initialize(this);
        PicasaSource.initialize(this);
        UsageStatistics.initialize(this);*/
        super.onCreate();
    }


    @Override
    public synchronized DataManager getDataManager() {
        if (mDataManager == null) {
            mDataManager = new DataManager(this);
            mDataManager.initializeSourceMap();
        }
        return mDataManager;
    }


    @Override
    public synchronized ThreadPool getThreadPool() {
        if (mThreadPool == null) {
            mThreadPool = new ThreadPool();
        }
        return mThreadPool;
    }

    @Override
    public ImageCacheService getImageCacheService() {
        // This method may block on file I/O so a dedicated lock is needed here.
        synchronized (mLock) {
            if (mImageCacheService == null) {
                mImageCacheService = new ImageCacheService(getAndroidContext());
            }
            return mImageCacheService;
        }
    }


    @Override
    public Context getAndroidContext() {
        return this;
    }
}