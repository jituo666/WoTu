package com.wotu.activity;

import android.content.Context;

import com.wotu.app.PageManager;
import com.wotu.data.DataManager;
import com.wotu.view.GLController;
import com.wotu.view.opengl.TransitionStore;


public interface WoTuContext {
    public DataManager getDataManager();
    public PageManager getPageManager();
    public GLController getGLController();
    public OrientationManager getOrientationManager();
    public TransitionStore getTransitionStore();
    public Context getAndroidContext();
}
