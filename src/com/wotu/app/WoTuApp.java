package com.wotu.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;

import com.wotu.common.ThreadPool;
import com.wotu.data.cache.ImageCacheService;

public interface WoTuApp {
    public Context getAndroidContext();

    public DataManager getDataManager();

    public ContentResolver getContentResolver();

    public Resources getResources();

    public Looper getMainLooper();

    public ThreadPool getThreadPool();

    public ImageCacheService getImageCacheService();
}
