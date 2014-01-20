package com.wotu.activity;

import com.wotu.app.StateManager;
import com.wotu.data.DataManager;
import com.wotu.view.OrientationManager;
import com.wotu.view.TransitionStore;
import com.wotu.view.opengl.GLRoot;


public interface WoTuContext {
    public DataManager getDataManager();
    public StateManager getStateManager();
    public GLRoot getGLRoot();
    public OrientationManager getOrientationManager();
    public TransitionStore getTransitionStore();
}
