
package com.wotu.page;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.wotu.R;
import com.wotu.activity.WoTuContext;
import com.wotu.common.Future;
import com.wotu.data.DataManager;
import com.wotu.data.MediaSet;
import com.wotu.data.Path;
import com.wotu.data.load.AlbumDataLoader;
import com.wotu.data.load.DataLoadListener;
import com.wotu.utils.UtilsBase;
import com.wotu.view.SlotView;
import com.wotu.view.SlotView.SlotRenderer;

public class AlbumPage extends PageState implements MediaSet.SyncListener {

    //data
    private Path mDataPath;
    private MediaSet mData;
    private AlbumDataLoader mDataLoader;
    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC = 2;
    private int mLoadingBits = 0;
    private Future<Integer> mSyncTask = null; // synchronize data
    private boolean mInitialSynced = false;

    //views
    private SlotView mSlotView;
    private SlotRenderer mRender;

    private boolean mIsActive = false;

    private class AlbumLoadingListener implements DataLoadListener {
        @Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
        }

        @Override
        public void onLoadingFinished() {
            clearLoadingBit(BIT_LOADING_RELOAD);
        }
    }

    private void setLoadingBit(int loadTaskBit) {
        mLoadingBits |= loadTaskBit;
    }

    private void clearLoadingBit(int loadTaskBit) {
        mLoadingBits &= ~loadTaskBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (mDataLoader.size() == 0) {
                Toast.makeText((Context) mContext,
                        R.string.empty_album, Toast.LENGTH_LONG).show();
                mContext.getPageManager().finishPage(AlbumPage.this);
            }
        }
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        return super.onCreateActionBar(menu);
    }

    private void initializeViews() {

    }

    private void initializeData(Bundle data) {
        mDataPath = new Path(data.getString(DataManager.KEY_MEDIA_PATH), 0);
        mData = mContext.getDataManager().getMediaSet(mDataPath);
        if (mData == null) {
            UtilsBase.fail("MediaSet is null. Path = %s", mDataPath);
        }
        mDataLoader = new AlbumDataLoader(mContext, mData);
        mDataLoader.setLoadingListener(new AlbumLoadingListener());
        mRender.setModel(mDataLoader);
    }

    @Override
    protected void onCreate(Bundle data, Bundle storedPage) {
        initializeViews();
        initializeData(data);
        super.onCreate(data, storedPage);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mData.requestSync(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncTask != null) {
            mSyncTask.cancel();
            mSyncTask = null;
            clearLoadingBit(BIT_LOADING_SYNC);
        }
    }

    @Override
    protected void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onSyncDone(MediaSet mediaSet, int resultCode) {

    }

}
