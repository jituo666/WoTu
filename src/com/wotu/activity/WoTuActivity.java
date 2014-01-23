package com.wotu.activity;

import com.wotu.R;
import com.wotu.app.WoTuApp;
import com.wotu.data.DataManager;
import com.wotu.data.MediaItem;
import com.wotu.page.PageManager;
import com.wotu.view.GLController;
import com.wotu.view.GLRootView;
import com.wotu.view.opengl.TransitionStore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

public abstract class WoTuActivity extends Activity implements WoTuContext {
    private GLRootView mGLRootView;
    private TransitionStore mTransitionStore = new TransitionStore();
    private OrientationManager mOrientationManager;
    private PageManager mPageManager;
    private WoTuActionBar mActionBar;

    private AlertDialog mAlertDialog = null;
    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getExternalCacheDir() != null)
                onStorageReady();
        }
    };
    private IntentFilter mMountFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);

    protected void onStorageReady() {
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
            unregisterReceiver(mMountReceiver);
        }
    }

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
        mGLRootView.lockRenderThread();
        try {
            getPageManager().resume();
            getDataManager().resume();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        mGLRootView.onResume();
        mOrientationManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationManager.pause();
        mGLRootView.onPause();
        mGLRootView.lockRenderThread();
        try {
            getPageManager().pause();
            getDataManager().pause();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        MediaItem.getMicroThumbPool().clear();
        MediaItem.getThumbPool().clear();
        MediaItem.getBytesBufferPool().clear();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getExternalCacheDir() == null) {
            OnCancelListener onCancel = new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            };
            OnClickListener onClick = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            };
            mAlertDialog = new AlertDialog.Builder(this)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle("No Storage")
                    .setMessage("No external storage available.")
                    .setNegativeButton(android.R.string.cancel, onClick)
                    .setOnCancelListener(onCancel)
                    .show();
            registerReceiver(mMountReceiver, mMountFilter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAlertDialog != null) {
            unregisterReceiver(mMountReceiver);
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGLRootView.lockRenderThread();
        try {
            getPageManager().notifyActivityResult(
                    requestCode, resultCode, data);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLController glController = getGLController();
        glController.lockRenderThread();
        try {
            getPageManager().onBackPressed();
        } finally {
            glController.unlockRenderThread();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLController glController = getGLController();
        glController.lockRenderThread();
        try {
            return getPageManager().itemSelected(item);
        } finally {
            glController.unlockRenderThread();
        }
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
    public GLController getGLController() {
        return mGLRootView;
    }

    @Override
    public WoTuActionBar getWoTuActionBar() {
        if (mActionBar == null) {
            mActionBar = new WoTuActionBar(this);
        }
        return mActionBar;
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
