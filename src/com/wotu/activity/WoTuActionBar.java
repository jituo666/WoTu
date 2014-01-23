
package com.wotu.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ShareActionProvider;
import android.widget.TextView;

import com.wotu.R;

import java.util.ArrayList;

public class WoTuActionBar implements ActionBar.OnNavigationListener {

    private static final String TAG = "WoTuActionBar";
    private static final int NAVI_ACTION_ALBUM = 1000;
    private static final int NAVI_ACTION_PICTURES = 1001;
    private NaviRunner mNaviRunner;
    private CharSequence[] mTitles;
    private ArrayList<Integer> mActions;
    private Context mContext;
    private LayoutInflater mInflater;
    private WoTuActivity mActivity;
    private ActionBar mActionBar;
    private int mCurrentIndex;
    private NaviAdapter mAdapter = new NaviAdapter();

    public interface NaviRunner {
        public void doNavi(int id);
    }

    private static class ActionItem {

        public int action;
        public boolean enabled;
        public boolean visible;
        public int spinnerTitle;
        public int dialogTitle;
        public int clusterBy;

        public ActionItem(int action, boolean applied, boolean enabled, int title, int clusterBy) {
            this(action, applied, enabled, title, title, clusterBy);
        }

        public ActionItem(int action, boolean applied, boolean enabled, int spinnerTitle,
                int dialogTitle, int clusterBy) {
            this.action = action;
            this.enabled = enabled;
            this.spinnerTitle = spinnerTitle;
            this.dialogTitle = dialogTitle;
            this.clusterBy = clusterBy;
            this.visible = true;
        }
    }

    private static final ActionItem[] sNaviItems = new ActionItem[] {
            new ActionItem(NAVI_ACTION_ALBUM, true, false, R.string.navi_to_album, R.string.navi_to_album),
            new ActionItem(NAVI_ACTION_PICTURES, true, false, R.string.navi_to_albumset, R.string.navi_to_albumset)
    };

    private class NaviAdapter extends BaseAdapter {

        public int getCount() {
            return sNaviItems.length;
        }

        public Object getItem(int position) {
            return sNaviItems[position];
        }

        public long getItemId(int position) {
            return sNaviItems[position].action;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.action_bar_text, parent, false);
            }
            TextView view = (TextView) convertView;
            view.setText(sNaviItems[position].spinnerTitle);
            return convertView;
        }
    }

    public static String getNaviByTypeString(Context context, int type) {
        for (ActionItem item : sNaviItems) {
            if (item.action == type) {
                return context.getString(item.clusterBy);
            }
        }
        return null;
    }

    public static ShareActionProvider initializeShareActionProvider(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_share);
        ShareActionProvider shareActionProvider = null;
        if (item != null) {
            shareActionProvider = (ShareActionProvider) item.getActionProvider();
        }
        return shareActionProvider;
    }

    public WoTuActionBar(WoTuActivity activity) {
        mActionBar = ((Activity) activity).getActionBar();
        mContext = activity.getAndroidContext();
        mActivity = activity;
        mInflater = ((Activity) mActivity).getLayoutInflater();
        mCurrentIndex = 0;
    }

    private void createDialogData() {
        ArrayList<CharSequence> titles = new ArrayList<CharSequence>();
        mActions = new ArrayList<Integer>();
        for (ActionItem item : sNaviItems) {
            if (item.enabled && item.visible) {
                titles.add(mContext.getString(item.dialogTitle));
                mActions.add(item.action);
            }
        }
        mTitles = new CharSequence[titles.size()];
        titles.toArray(mTitles);
    }

    public int getHeight() {
        return mActionBar != null ? mActionBar.getHeight() : 0;
    }

    public void setNaviItemEnabled(int id, boolean enabled) {
        for (ActionItem item : sNaviItems) {
            if (item.action == id) {
                item.enabled = enabled;
                return;
            }
        }
    }

    public void setNaviItemVisibility(int id, boolean visible) {
        for (ActionItem item : sNaviItems) {
            if (item.action == id) {
                item.visible = visible;
                return;
            }
        }
    }

    public int getNaviTypeAction() {
        return sNaviItems[mCurrentIndex].action;
    }

    public void enableNaviMenu(int action, NaviRunner runner) {
        if (mActionBar != null) {
            // Don't set cluster runner until action bar is ready.
            mNaviRunner = null;
            mActionBar.setListNavigationCallbacks(mAdapter, this);
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            setSelectedAction(action);
            mNaviRunner = runner;
        }
    }

    // The only use case not to hideMenu in this method is to ensure
    // all elements disappear at the same time when exiting gallery.
    // hideMenu should always be true in all other cases.
    public void disableNaviMenu(boolean hideMenu) {
        if (mActionBar != null) {
            mNaviRunner = null;
            if (hideMenu) {
                mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            }
        }
    }

    public void showNaviDialog(final NaviRunner naviRunner) {
        createDialogData();
        final ArrayList<Integer> actions = mActions;
        new AlertDialog.Builder(mContext).setTitle(R.string.navi_tips)
                .setItems(mTitles, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Need to lock rendering when operations invoked by
                        // system UI (main thread) are
                        // modifying slot data used in GL thread for rendering.
                        mActivity.getGLController().lockRenderThread();
                        try {
                            naviRunner.doNavi(actions.get(which).intValue());
                        } finally {
                            mActivity.getGLController().unlockRenderThread();
                        }
                    }
                }).create().show();
    }

    public void setDisplayOptions(boolean displayHomeAsUp, boolean showTitle) {
        if (mActionBar != null) {
            int options = (displayHomeAsUp ? ActionBar.DISPLAY_HOME_AS_UP : 0)
                    | (showTitle ? ActionBar.DISPLAY_SHOW_TITLE : 0);
            mActionBar.setDisplayOptions(options, ActionBar.DISPLAY_HOME_AS_UP
                    | ActionBar.DISPLAY_SHOW_TITLE);
            mActionBar.setHomeButtonEnabled(displayHomeAsUp);
        }
    }

    public void setTitle(String title) {
        if (mActionBar != null)
            mActionBar.setTitle(title);
    }

    public void setTitle(int titleId) {
        if (mActionBar != null)
            mActionBar.setTitle(titleId);
    }

    public void setSubtitle(String title) {
        if (mActionBar != null)
            mActionBar.setSubtitle(title);
    }

    public void show() {
        if (mActionBar != null)
            mActionBar.show();
    }

    public void hide() {
        if (mActionBar != null)
            mActionBar.hide();
    }

    public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        if (mActionBar != null)
            mActionBar.addOnMenuVisibilityListener(listener);
    }

    public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        if (mActionBar != null)
            mActionBar.removeOnMenuVisibilityListener(listener);
    }

    public boolean setSelectedAction(int type) {
        if (mActionBar == null)
            return false;
        for (int i = 0, n = sNaviItems.length; i < n; i++) {
            ActionItem item = sNaviItems[i];
            if (item.action == type) {
                mActionBar.setSelectedNavigationItem(i);
                mCurrentIndex = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (itemPosition != mCurrentIndex && mNaviRunner != null) {
            // Need to lock rendering when operations invoked by system UI (main
            // thread) are
            // modifying slot data used in GL thread for rendering.
            mActivity.getGLController().lockRenderThread();
            try {
                mNaviRunner.doNavi(sNaviItems[itemPosition].action);
            } finally {
                mActivity.getGLController().unlockRenderThread();
            }
        }
        return false;
    }
}
