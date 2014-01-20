package com.wotu.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.Stack;

import com.wotu.activity.WoTuContext;
import com.wotu.common.WLog;
import com.wotu.utils.UtilsBase;

public class StateManager {
    @SuppressWarnings("unused")
    private static final String TAG = "StateManager";
    private boolean mIsResumed = false;

    private static final String KEY_MAIN = "activity-state";
    private static final String KEY_DATA = "data";
    private static final String KEY_STATE = "bundle";
    private static final String KEY_CLASS = "class";

    private WoTuContext mContext;
    private Stack<StateEntry> mStack = new Stack<StateEntry>();
    private ActivityState.ResultEntry mResult;

    public StateManager(WoTuContext context) {
        mContext = context;
    }

    public void startState(Class<? extends ActivityState> klass,
            Bundle data) {
        WLog.v(TAG, "startState " + klass);
        ActivityState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        if (!mStack.isEmpty()) {
            ActivityState top = getTopState();
            if (mIsResumed) top.onPause();
        }
        state.initialize(mContext, data);

        mStack.push(new StateEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed) state.resume();
    }

    public void startStateForResult(Class<? extends ActivityState> klass,
            int requestCode, Bundle data) {
        WLog.v(TAG, "startStateForResult " + klass + ", " + requestCode);
        ActivityState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        state.initialize(mContext, data);
        state.mResult = new ActivityState.ResultEntry();
        state.mResult.requestCode = requestCode;

        if (!mStack.isEmpty()) {
            ActivityState as = getTopState();
            as.mReceivedResults = state.mResult;
            if (mIsResumed) as.onPause();
        } else {
            mResult = state.mResult;
        }

        mStack.push(new StateEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed) state.resume();
    }

    public boolean createOptionsMenu(Menu menu) {
        if (mStack.isEmpty()) {
            return false;
        } else {
            return getTopState().onCreateActionBar(menu);
        }
    }

    public void onConfigurationChange(Configuration config) {
        for (StateEntry entry : mStack) {
            entry.activityState.onConfigurationChanged(config);
        }
    }

    public void resume() {
        if (mIsResumed) return;
        mIsResumed = true;
        if (!mStack.isEmpty()) getTopState().resume();
    }

    public void pause() {
        if (!mIsResumed) return;
        mIsResumed = false;
        if (!mStack.isEmpty()) getTopState().onPause();
    }

    public void notifyActivityResult(int requestCode, int resultCode, Intent data) {
        getTopState().onStateResult(requestCode, resultCode, data);
    }

    public int getStateCount() {
        return mStack.size();
    }

    public boolean itemSelected(MenuItem item) {
        if (!mStack.isEmpty()) {
            if (getTopState().onItemSelected(item)) return true;
            if (item.getItemId() == android.R.id.home) {
                if (mStack.size() > 1) {
                    getTopState().onBackPressed();
                }
                return true;
            }
        }
        return false;
    }

    public void onBackPressed() {
        if (!mStack.isEmpty()) {
            getTopState().onBackPressed();
        }
    }

    void finishState(ActivityState state) {
        // The finish() request could be rejected (only happens under Monkey),
        // If it is rejected, we won't close the last page.
        if (mStack.size() == 1) {
            Activity activity = (Activity) mContext.getAndroidContext();
            if (mResult != null) {
                activity.setResult(mResult.resultCode, mResult.resultData);
            }
            activity.finish();
            if (!activity.isFinishing()) {
                WLog.w(TAG, "finish is rejected, keep the last state");
                return;
            }
            WLog.v(TAG, "no more state, finish activity");
        }

        WLog.v(TAG, "finishState " + state);
        if (state != mStack.peek().activityState) {
            if (state.isDestroyed()) {
                WLog.d(TAG, "The state is already destroyed");
                return;
            } else {
                throw new IllegalArgumentException("The stateview to be finished"
                        + " is not at the top of the stack: " + state + ", "
                        + mStack.peek().activityState);
            }
        }

        // Remove the top state.
        mStack.pop();
        state.mIsFinishing = true;
        if (mIsResumed) state.onPause();
        mContext.getGLRoot().setContentPane(null);
        state.onDestroy();

        if (!mStack.isEmpty()) {
            // Restore the immediately previous state
            ActivityState top = mStack.peek().activityState;
            if (mIsResumed) top.resume();
        }
    }

    void switchState(ActivityState oldState,
            Class<? extends ActivityState> klass, Bundle data) {
        WLog.v(TAG, "switchState " + oldState + ", " + klass);
        if (oldState != mStack.peek().activityState) {
            throw new IllegalArgumentException("The stateview to be finished"
                    + " is not at the top of the stack: " + oldState + ", "
                    + mStack.peek().activityState);
        }
        // Remove the top state.
        mStack.pop();
        if (mIsResumed) oldState.onPause();
        oldState.onDestroy();

        // Create new state.
        ActivityState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        state.initialize(mContext, data);
        mStack.push(new StateEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed) state.resume();
    }

    public void destroy() {
        WLog.v(TAG, "destroy");
        while (!mStack.isEmpty()) {
            mStack.pop().activityState.onDestroy();
        }
        mStack.clear();
    }

    @SuppressWarnings("unchecked")
    public void restoreFromState(Bundle inState) {
        WLog.v(TAG, "restoreFromState");
        Parcelable list[] = inState.getParcelableArray(KEY_MAIN);
        for (Parcelable parcelable : list) {
            Bundle bundle = (Bundle) parcelable;
            Class<? extends ActivityState> klass =
                    (Class<? extends ActivityState>) bundle.getSerializable(KEY_CLASS);

            Bundle data = bundle.getBundle(KEY_DATA);
            Bundle state = bundle.getBundle(KEY_STATE);

            ActivityState activityState;
            try {
                WLog.v(TAG, "restoreFromState " + klass);
                activityState = klass.newInstance();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            activityState.initialize(mContext, data);
            activityState.onCreate(data, state);
            mStack.push(new StateEntry(data, activityState));
        }
    }

    public void saveState(Bundle outState) {
        WLog.v(TAG, "saveState");

        Parcelable list[] = new Parcelable[mStack.size()];
        int i = 0;
        for (StateEntry entry : mStack) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_CLASS, entry.activityState.getClass());
            bundle.putBundle(KEY_DATA, entry.data);
            Bundle state = new Bundle();
            entry.activityState.onSaveState(state);
            bundle.putBundle(KEY_STATE, state);
            WLog.v(TAG, "saveState " + entry.activityState.getClass());
            list[i++] = bundle;
        }
        outState.putParcelableArray(KEY_MAIN, list);
    }

    public boolean hasStateClass(Class<? extends ActivityState> klass) {
        for (StateEntry entry : mStack) {
            if (klass.isInstance(entry.activityState)) {
                return true;
            }
        }
        return false;
    }

    public ActivityState getTopState() {
        UtilsBase.assertTrue(!mStack.isEmpty());
        return mStack.peek().activityState;
    }

    private static class StateEntry {
        public Bundle data;
        public ActivityState activityState;

        public StateEntry(Bundle data, ActivityState state) {
            this.data = data;
            this.activityState = state;
        }
    }
}
