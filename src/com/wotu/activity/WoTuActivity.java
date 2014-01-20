
package com.wotu.activity;

import com.wotu.R;
import com.wotu.app.PageManager;
import com.wotu.app.WoTuApp;
import com.wotu.data.DataManager;
import com.wotu.view.GLRootView;
import com.wotu.view.OrientationManager;
import com.wotu.view.TransitionStore;
import com.wotu.view.opengl.GLRoot;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

public abstract class WoTuActivity extends Activity implements WoTuContext {
    private GLRootView mGLRootView;
    private TransitionStore mTransitionStore = new TransitionStore();
    private OrientationManager mOrientationManager;
    private PageManager mPageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mOrientationManager = new OrientationManager(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int resId) {
        super.setContentView(resId);
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
    }
    
    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mPageManager.onConfigurationChange(config);
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public DataManager getDataManager() {
        return ((WoTuApp) getApplication()).getDataManager();
    }

    @Override
    public PageManager getPageManager() {
        if (mPageManager == null) {
            mPageManager = new PageManager(this);
        }
        return mPageManager;
    }

    @Override
    public GLRoot getGLRoot() {
        return mGLRootView;
    }

    @Override
    public OrientationManager getOrientationManager() {
        return mOrientationManager;
    }

    @Override
    public TransitionStore getTransitionStore() {
        return mTransitionStore;
    }

    @Override
    public Context getAndroidContext() {
        return this;
    }

}
