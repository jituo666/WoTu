package com.wotu.activity;

import android.content.Context;

import com.wotu.app.PageManager;
import com.wotu.data.DataManager;
import com.wotu.view.OrientationManager;
import com.wotu.view.TransitionStore;
import com.wotu.view.opengl.GLRoot;


public interface WoTuContext {
    public DataManager getDataManager();
    public PageManager getPageManager();
    public GLRoot getGLRoot();
    public OrientationManager getOrientationManager();
    public TransitionStore getTransitionStore();
    public Context getAndroidContext();
}
