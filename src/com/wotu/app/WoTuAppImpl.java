package com.wotu.app;

import com.wotu.data.DataManager;

import android.app.Application;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Looper;

public class WoTuAppImpl extends Application implements WoTuApp {

    @Override
    public DataManager getDataManager() {
        return new DataManager(this);
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
}