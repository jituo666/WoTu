package com.wotu.page;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.widget.Toast;

import com.wotu.R;
import com.wotu.common.Future;
import com.wotu.data.DataManager;
import com.wotu.data.MediaDetails;
import com.wotu.data.MediaItem;
import com.wotu.data.MediaObject;
import com.wotu.data.MediaSet;
import com.wotu.data.Path;
import com.wotu.data.load.AlbumDataLoader;
import com.wotu.data.load.DataLoadListener;
import com.wotu.utils.UtilsBase;
import com.wotu.utils.UtilsCom;
import com.wotu.view.DetailsHelper;
import com.wotu.view.DetailsHelper.CloseListener;
import com.wotu.view.GLView;
import com.wotu.view.PhotoFallbackEffect;
import com.wotu.view.RelativePosition;
import com.wotu.view.SlotView;
import com.wotu.view.opengl.GLCanvas;
import com.wotu.view.render.SlotViewRender;

public class AlbumPage extends PageState implements MediaSet.SyncListener {

    public static final String TAG = "AlbumPage";
    public static final String KEY_RESUME_ANIMATION = "resume_animation";

    //data
    private Path mDataPath;
    private MediaSet mData;
    private DetailsHelper mDetailsHelper;
    private MyDetailsSource mDetailsSource;
    private AlbumDataLoader mDataLoader;
    private static final int BIT_LOADING_RELOAD = 1;
    private static final int BIT_LOADING_SYNC = 2;
    private int mLoadingBits = 0;
    private Future<Integer> mSyncTask = null; // synchronize data
    private boolean mInitialSynced = false;
    private boolean mShowDetails;

    //views
    private SlotView mSlotView;
    private SlotViewRender mRender;
    private RelativePosition mOpenCenter = new RelativePosition();
    private boolean mIsActive = false;
    private float mUserDistance; // in pixel

    private PhotoFallbackEffect mResumeEffect;
    private PhotoFallbackEffect.PositionProvider mPositionProvider =
            new PhotoFallbackEffect.PositionProvider() {
                @Override
                public Rect getPosition(int index) {
                    Rect rect = mSlotView.getSlotRect(index);
                    Rect bounds = mSlotView.bounds();
                    rect.offset(bounds.left - mSlotView.getScrollX(),
                            bounds.top - mSlotView.getScrollY());
                    return rect;
                }

                @Override
                public int getItemIndex(Path path) {
                    int start = mSlotView.getVisibleStart();
                    int end = mSlotView.getVisibleEnd();
                    for (int i = start; i < end; ++i) {
                        MediaItem item = mDataLoader.get(i);
                        if (item != null && item.getPath() == path)
                            return i;
                    }
                    return -1;
                }
            };

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

    private final GLView mRootPane = new GLView() {
        private final float mMatrix[] = new float[16];

        @Override
        protected void onLayout(
                boolean changed, int left, int top, int right, int bottom) {

            int slotViewTop = mContext.getWoTuActionBar().getHeight();
            int slotViewBottom = bottom - top;
            int slotViewRight = right - left;

            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mRender.setHighlightItemPath(null);
            }

            // Set the mSlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            mSlotView.layout(0, slotViewTop, slotViewRight, slotViewBottom);
            UtilsCom.setViewPointMatrix(mMatrix,
                    (right - left) / 2, (bottom - top) / 2, -mUserDistance);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);

            if (mResumeEffect != null) {
                boolean more = mResumeEffect.draw(canvas);
                if (!more) {
                    mResumeEffect = null;
                    mRender.setSlotFilter(null);
                }
                // We want to render one more time even when no more effect
                // required. So that the animated thumbnails could be draw
                // with declarations in super.render().
                invalidate();
            }
            canvas.restore();
        }
    };

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex;

        @Override
        public int size() {
            return mDataLoader.size();
        }

        @Override
        public int setIndex() {
            //Path id = mSelectionManager.getSelected(false).get(0);
            mIndex = mDataLoader.findItem(null);
            return mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            // this relies on setIndex() being called beforehand
            MediaObject item = mDataLoader.get(mIndex);
            if (item != null) {
                mRender.setHighlightItemPath(item.getPath());
                return item.getDetails();
            } else {
                return null;
            }
        }
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mContext, mRootPane, mDetailsSource);
            mDetailsHelper.setCloseListener(new CloseListener() {
                @Override
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.show();
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
        mRender.setHighlightItemPath(null);
        mSlotView.invalidate();
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
        mSlotView = new SlotView(mContext);
        mRender = new SlotViewRender(mContext, mSlotView);
        mSlotView.setSlotRenderer(mRender);
        mRender.setModel(mDataLoader);
        mRootPane.addChild(mSlotView);
        mSlotView.setGestureListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                AlbumPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                AlbumPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                AlbumPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                AlbumPage.this.onLongTap(slotIndex);
            }
        });
    }

    private void onDown(int index) {
        mRender.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mRender.setPressedIndex(-1);
        } else {
            mRender.setPressedUp();
        }
    }

    private void onSingleTapUp(int slotIndex) {
        if (!mIsActive)
            return;

        if (mSelectionManager.inSelectionMode()) {
            MediaItem item = mAlbumDataAdapter.get(slotIndex);
            if (item == null)
                return; // Item not ready yet, ignore the click
            mSelectionManager.toggle(item.getPath());
            mSlotView.invalidate();
        } else {
            // Render transition in pressed state
            mRender.setPressedIndex(slotIndex);
            mRender.setPressedUp();
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_PHOTO, slotIndex, 0),
                    FadeTexture.DURATION);
        }
    }

    public void onLongTap(int slotIndex) {
        if (mGetContent)
            return;
        MediaItem item = mAlbumDataAdapter.get(slotIndex);
        if (item == null)
            return;
        mSelectionManager.setAutoLeaveSelectionMode(true);
        mSelectionManager.toggle(item.getPath());
        mSlotView.invalidate();
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
        mIsActive = true;

        mResumeEffect = mContext.getTransitionStore().get(KEY_RESUME_ANIMATION);
        if (mResumeEffect != null) {
            mRender.setSlotFilter(mResumeEffect);
            mResumeEffect.setPositionProvider(mPositionProvider);
            mResumeEffect.start();
        }

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
