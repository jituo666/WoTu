package com.wotu.activity;

import android.content.Context;

import com.wotu.common.ThreadPool;
import com.wotu.data.DataManager;
import com.wotu.page.PageManager;
import com.wotu.view.GLController;
import com.wotu.view.opengl.TransitionStore;


public interface WoTuContext {
    public DataManager getDataManager();
    public PageManager getPageManager();
    public GLController getGLController();
    public WoTuActionBar getWoTuActionBar();
    public OrientationManager getOrientationManager();
    public TransitionStore getTransitionStore();
    public Context getAndroidContext();
    public ThreadPool getThreadPool();
}
