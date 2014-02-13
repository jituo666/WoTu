package com.wotu.page;

import com.wotu.R;
import com.wotu.activity.WoTuContext;
import com.wotu.utils.UtilsCom;
import com.wotu.view.GLView;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

abstract public class PageState {
    protected static final int FLAG_HIDE_ACTION_BAR = 1;
    protected static final int FLAG_HIDE_STATUS_BAR = 2;
    protected static final int FLAG_SCREEN_ON_WHEN_PLUGGED = 4;
    protected static final int FLAG_SCREEN_ON_ALWAYS = 8;

    private static final int SCREEN_ON_FLAGS = (
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            );

    protected WoTuContext mContext;
    protected Bundle mData;
    protected int mFlags;

    protected ResultEntry mReceivedResults;
    protected ResultEntry mResult;

    protected static class ResultEntry {
        public int requestCode;
        public int resultCode = Activity.RESULT_CANCELED;
        public Intent resultData;
    }

    protected float[] mBackgroundColor;

    private boolean mDestroyed = false;
    private boolean mPlugged = false;
    boolean mIsFinishing = false;

    protected PageState() {
    }

    protected void setContentPane(GLView content) {
        mContext.getGLController().setContentPane(content);
    }

    void initialize(WoTuContext activity, Bundle data) {
        mContext = activity;
        mData = data;
    }

    public Bundle getData() {
        return mData;
    }

    protected void onBackPressed() {
        mContext.getPageManager().finishPage(this);
    }

    protected void setStateResult(int resultCode, Intent data) {
        if (mResult == null)
            return;
        mResult.resultCode = resultCode;
        mResult.resultData = data;
    }

    protected void onConfigurationChanged(Configuration config) {
    }

    protected void onSavePage(Bundle outPage) {
    }

    protected void onPageResult(int requestCode, int resultCode, Intent data) {
    }

    protected int getBackgroundColorId() {
        return R.color.default_background;
    }

    protected float[] getBackgroundColor() {
        return mBackgroundColor;
    }

    protected void onCreate(Bundle data, Bundle storedPage) {
        mBackgroundColor = UtilsCom.intColorToFloatARGBArray(mContext.getAndroidContext().getResources().getColor(getBackgroundColorId()));
    }

    BroadcastReceiver mPowerIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean plugged = (0 != intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));

                if (plugged != mPlugged) {
                    mPlugged = plugged;
                    setScreenOnFlags();
                }
            }
        }
    };

    void setScreenOnFlags() {
        final Window win = ((Activity) mContext).getWindow();
        final WindowManager.LayoutParams params = win.getAttributes();
        if ((0 != (mFlags & FLAG_SCREEN_ON_ALWAYS)) ||
                (mPlugged && 0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED))) {
            params.flags |= SCREEN_ON_FLAGS;
        } else {
            params.flags &= ~SCREEN_ON_FLAGS;
        }
        win.setAttributes(params);
    }

    protected void onPause() {
        if (0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED)) {
            ((Activity) mContext).unregisterReceiver(mPowerIntentReceiver);
        }
    }

    // should only be called by PageManager
    void resume() {
        Activity activity = (Activity) mContext;
        ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            if ((mFlags & FLAG_HIDE_ACTION_BAR) != 0) {
                actionBar.hide();
            } else {
                actionBar.show();
            }
            int stateCount = mContext.getPageManager().getStateCount();
            mContext.getWoTuActionBar().setDisplayOptions(stateCount > 1, true);
            // Default behavior, this can be overridden in ActivityState's onResume.
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
        activity.invalidateOptionsMenu();
        setScreenOnFlags();

        boolean lightsOut = ((mFlags & FLAG_HIDE_STATUS_BAR) != 0);
        mContext.getGLController().setLightsOutMode(lightsOut);

        ResultEntry entry = mReceivedResults;
        if (entry != null) {
            mReceivedResults = null;
            onPageResult(entry.requestCode, entry.resultCode, entry.resultData);
        }

        if (0 != (mFlags & FLAG_SCREEN_ON_WHEN_PLUGGED)) {
            // we need to know whether the device is plugged in to do this correctly
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            activity.registerReceiver(mPowerIntentReceiver, filter);
        }
        onResume();

        // the transition store should be cleared after resume;
        mContext.getTransitionStore().clear();
    }

    // a subclass of Page should override the method to resume itself
    protected void onResume() {
    }

    protected boolean onCreateActionBar(Menu menu) {
        return true;
    }

    protected boolean onItemSelected(MenuItem item) {
        return false;
    }

    protected void onDestroy() {
        mDestroyed = true;
    }

    boolean isDestroyed() {
        return mDestroyed;
    }

    public boolean isFinishing() {
        return mIsFinishing;
    }
}
