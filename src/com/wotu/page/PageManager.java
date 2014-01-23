
package com.wotu.page;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Stack;

import com.wotu.activity.WoTuContext;
import com.wotu.common.WLog;
import com.wotu.utils.UtilsBase;

public class PageManager {
    @SuppressWarnings("unused")
    private static final String TAG = "PageManager";
    private boolean mIsResumed = false;

    private static final String KEY_MAIN = "page-state";
    private static final String KEY_DATA = "data";
    private static final String KEY_STATE = "bundle";
    private static final String KEY_CLASS = "class";

    private WoTuContext mContext;
    private Stack<PageEntry> mStack = new Stack<PageEntry>();
    private PageState.ResultEntry mResult;

    public PageManager(WoTuContext context) {
        mContext = context;
    }

    public void startPage(Class<? extends PageState> klass,
            Bundle data) {
        WLog.v(TAG, "startPage " + klass);
        PageState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        if (!mStack.isEmpty()) {
            PageState top = getTopPage();
            if (mIsResumed)
                top.onPause();
        }
        state.initialize(mContext, data);

        mStack.push(new PageEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed)
            state.resume();
    }

    public void startPageForResult(Class<? extends PageState> klass,
            int requestCode, Bundle data) {
        WLog.v(TAG, "startPageForResult " + klass + ", " + requestCode);
        PageState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        state.initialize(mContext, data);
        state.mResult = new PageState.ResultEntry();
        state.mResult.requestCode = requestCode;

        if (!mStack.isEmpty()) {
            PageState as = getTopPage();
            as.mReceivedResults = state.mResult;
            if (mIsResumed)
                as.onPause();
        } else {
            mResult = state.mResult;
        }

        mStack.push(new PageEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed)
            state.resume();
    }

    public boolean createOptionsMenu(Menu menu) {
        if (mStack.isEmpty()) {
            return false;
        } else {
            return getTopPage().onCreateActionBar(menu);
        }
    }

    public void onConfigurationChange(Configuration config) {
        for (PageEntry entry : mStack) {
            entry.pageState.onConfigurationChanged(config);
        }
    }

    public void resume() {
        if (mIsResumed)
            return;
        mIsResumed = true;
        if (!mStack.isEmpty())
            getTopPage().resume();
    }

    public void pause() {
        if (!mIsResumed)
            return;
        mIsResumed = false;
        if (!mStack.isEmpty())
            getTopPage().onPause();
    }

    public int getStateCount() {
        return mStack.size();
    }

    public void notifyActivityResult(int requestCode, int resultCode, Intent data) {
        getTopPage().onPageResult(requestCode, resultCode, data);
    }

    public int getPageCount() {
        return mStack.size();
    }

    public boolean itemSelected(MenuItem item) {
        if (!mStack.isEmpty()) {
            if (getTopPage().onItemSelected(item))
                return true;
            if (item.getItemId() == android.R.id.home) {
                if (mStack.size() > 1) {
                    getTopPage().onBackPressed();
                }
                return true;
            }
        }
        return false;
    }

    public void onBackPressed() {
        if (!mStack.isEmpty()) {
            getTopPage().onBackPressed();
        } else {
            Activity activity = (Activity) mContext.getAndroidContext();
            activity.finish();
        }
    }

    public void finishPage(PageState state) {
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

        WLog.v(TAG, "finishPage " + state);
        if (state != mStack.peek().pageState) {
            if (state.isDestroyed()) {
                WLog.d(TAG, "The state is already destroyed");
                return;
            } else {
                throw new IllegalArgumentException("The stateview to be finished"
                        + " is not at the top of the stack: " + state + ", "
                        + mStack.peek().pageState);
            }
        }

        // Remove the top state.
        mStack.pop();
        state.mIsFinishing = true;
        if (mIsResumed)
            state.onPause();
        mContext.getGLController().setContentPane(null);
        state.onDestroy();

        if (!mStack.isEmpty()) {
            // Restore the immediately previous state
            PageState top = mStack.peek().pageState;
            if (mIsResumed)
                top.resume();
        }
    }

    void switchPage(PageState oldPage,
            Class<? extends PageState> klass, Bundle data) {
        WLog.v(TAG, "switchPage " + oldPage + ", " + klass);
        if (oldPage != mStack.peek().pageState) {
            throw new IllegalArgumentException("The stateview to be finished"
                    + " is not at the top of the stack: " + oldPage + ", "
                    + mStack.peek().pageState);
        }
        // Remove the top state.
        mStack.pop();
        if (mIsResumed)
            oldPage.onPause();
        oldPage.onDestroy();

        // Create new state.
        PageState state = null;
        try {
            state = klass.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        state.initialize(mContext, data);
        mStack.push(new PageEntry(data, state));
        state.onCreate(data, null);
        if (mIsResumed)
            state.resume();
    }

    public void destroy() {
        WLog.v(TAG, "destroy");
        while (!mStack.isEmpty()) {
            mStack.pop().pageState.onDestroy();
        }
        mStack.clear();
    }

    @SuppressWarnings("unchecked")
    public void restoreFromPage(Bundle inPage) {
        WLog.v(TAG, "restoreFromPage");
        Parcelable list[] = inPage.getParcelableArray(KEY_MAIN);
        for (Parcelable parcelable : list) {
            Bundle bundle = (Bundle) parcelable;
            Class<? extends PageState> klass =
                    (Class<? extends PageState>) bundle.getSerializable(KEY_CLASS);

            Bundle data = bundle.getBundle(KEY_DATA);
            Bundle state = bundle.getBundle(KEY_STATE);

            PageState pageState;
            try {
                WLog.v(TAG, "restoreFromPage " + klass);
                pageState = klass.newInstance();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            pageState.initialize(mContext, data);
            pageState.onCreate(data, state);
            mStack.push(new PageEntry(data, pageState));
        }
    }

    public void savePage(Bundle outPage) {
        WLog.v(TAG, "savePage");

        Parcelable list[] = new Parcelable[mStack.size()];
        int i = 0;
        for (PageEntry entry : mStack) {
            Bundle bundle = new Bundle();
            bundle.putSerializable(KEY_CLASS, entry.pageState.getClass());
            bundle.putBundle(KEY_DATA, entry.data);
            Bundle state = new Bundle();
            entry.pageState.onSavePage(state);
            bundle.putBundle(KEY_STATE, state);
            WLog.v(TAG, "savePage " + entry.pageState.getClass());
            list[i++] = bundle;
        }
        outPage.putParcelableArray(KEY_MAIN, list);
    }

    public boolean hasPageClass(Class<? extends PageState> klass) {
        for (PageEntry entry : mStack) {
            if (klass.isInstance(entry.pageState)) {
                return true;
            }
        }
        return false;
    }

    public PageState getTopPage() {
        UtilsBase.assertTrue(!mStack.isEmpty());
        return mStack.peek().pageState;
    }

    private static class PageEntry {
        public Bundle data;
        public PageState pageState;

        public PageEntry(Bundle data, PageState state) {
            this.data = data;
            this.pageState = state;
        }
    }
}
