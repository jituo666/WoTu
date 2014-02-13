
package com.wotu.page;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Message;
import android.view.Menu;
import android.widget.Toast;

import com.wotu.R;
import com.wotu.app.MediaSelector;
import com.wotu.common.Future;
import com.wotu.common.SynchronizedHandler;
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
import com.wotu.view.layout.NormalLayout;
import com.wotu.view.opengl.FadeTexture;
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
    private AlbumDataLoader mAlbumDataLoader;
    private boolean mLoadingFailed;
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

    private boolean mGetContent;
    private SynchronizedHandler mHandler;
    private static final int MSG_PICK_PHOTO = 0;
    protected MediaSelector mSelector;

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
                        MediaItem item = mAlbumDataLoader.get(i);
                        if (item != null && item.getPath() == path)
                            return i;
                    }
                    return -1;
                }
            };

    private class AlbumLoadingListener implements DataLoadListener {
        @Override
        public void onLoadingStarted() {
            mLoadingFailed = false;
            setLoadingBit(BIT_LOADING_RELOAD);
        }

        @Override
        public void onLoadingFinished(boolean loadFailed) {
            mLoadingFailed = loadFailed;
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
            UtilsCom.setViewPointMatrix(mMatrix, (right - left) / 2, (bottom - top) / 2, -mUserDistance);
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
            return mAlbumDataLoader.size();
        }

        @Override
        public int setIndex() {
            //Path id = mSelector.getSelected(false).get(0);
            mIndex = mAlbumDataLoader.findItem(null);
            return mIndex;
        }

        @Override
        public MediaDetails getDetails() {
            // this relies on setIndex() being called beforehand
            MediaObject item = mAlbumDataLoader.get(mIndex);
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
            if (mAlbumDataLoader.size() == 0) {
                Toast.makeText((Context) mContext,
                        R.string.empty_album, Toast.LENGTH_LONG).show();
                mContext.getPageManager().finishPage(AlbumPage.this);
            }
        }
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

        if (mSelector.inSelectionMode()) {
            MediaItem item = mAlbumDataLoader.get(slotIndex);
            if (item == null)
                return; // Item not ready yet, ignore the click
            mSelector.toggle(item.getPath());
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
        MediaItem item = mAlbumDataLoader.get(slotIndex);
        if (item == null)
            return;
        mSelector.setAutoLeaveSelectionMode(true);
        mSelector.toggle(item.getPath());
        mSlotView.invalidate();
    }

    private void initializeViews() {
        mSlotView = new SlotView(mContext);
        //mSlotView.setBackgroundColor(UtilsCom.intColorToFloatARGBArray(mContext.getAndroidContext().getResources().getColor(getBackgroundColorId())));
        mSlotView.setSlotLayout(new NormalLayout(mContext.getAndroidContext()));
        mRender = new SlotViewRender(mContext, mSlotView, mSelector);
        mSlotView.setSlotRenderer(mRender);
        mRender.setModel(mAlbumDataLoader);
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

    private void initializeData(Bundle data) {
        mDataPath = new Path(data.getString(DataManager.KEY_MEDIA_PATH), -48385503);
        mData = mContext.getDataManager().getMediaSet(mDataPath);
        if (mData == null) {
            UtilsBase.fail("MediaSet is null. Path = %s", mDataPath);
        }
        mAlbumDataLoader = new AlbumDataLoader(mContext, mData);
        mAlbumDataLoader.setLoadingListener(new AlbumLoadingListener());
        mRender.setModel(mAlbumDataLoader);
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        return super.onCreateActionBar(menu);
    }

    @Override
    protected void onCreate(Bundle data, Bundle storedPage) {
        super.onCreate(data, storedPage);
        mSelector = new MediaSelector(mContext, false);
        initializeViews();
        initializeData(data);
        mHandler = new SynchronizedHandler(mContext.getGLController()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_PICK_PHOTO: {
                        //pickPhoto(message.arg1);
                        break;
                    }
                    default:
                        throw new AssertionError(message.what);
                }
            }
        };
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
        setContentPane(mRootPane);
        // Set the reload bit here to prevent it exit this page in clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        mLoadingFailed = false;
        mAlbumDataLoader.resume();
        mRender.resume();
        mRender.setPressedIndex(-1);
        if (!mInitialSynced) {
            setLoadingBit(BIT_LOADING_SYNC);
            mSyncTask = mData.requestSync(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;
        mRender.setSlotFilter(null);
        mAlbumDataLoader.pause();
        mRender.pause();
        DetailsHelper.pause();
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
    //
    //    private void pickPhoto(int slotIndex) {
    //        pickPhoto(slotIndex, false);
    //    }
    //
    //    private void pickPhoto(int slotIndex, boolean startInFilmstrip) {
    //        if (!mIsActive)
    //            return;
    //
    //        if (!startInFilmstrip) {
    //            // Launch photos in lights out mode
    //            mContext.getGLController().setLightsOutMode(true);
    //        }
    //
    //        MediaItem item = mAlbumDataLoader.get(slotIndex);
    //        if (item == null)
    //            return; // Item not ready yet, ignore the click
    //        if (mGetContent) {
    //            onGetContent(item);
    //        } else if (mLaunchedFromPhotoPage) {
    //            TransitionStore transitions = mContext.getTransitionStore();
    //            transitions.put(
    //                    PhotoPage.KEY_ALBUMPAGE_TRANSITION,
    //                    PhotoPage.MSG_ALBUMPAGE_PICKED);
    //            transitions.put(PhotoPage.KEY_INDEX_HINT, slotIndex);
    //            onBackPressed();
    //        } else {
    //            // Get into the PhotoPage.
    //            // mAlbumView.savePositions(PositionRepository.getInstance(mContext));
    //            Bundle data = new Bundle();
    //            data.putInt(PhotoPage.KEY_INDEX_HINT, slotIndex);
    //            data.putParcelable(PhotoPage.KEY_OPEN_ANIMATION_RECT,
    //                    mSlotView.getSlotRect(slotIndex, mRootPane));
    //            data.putString(PhotoPage.KEY_MEDIA_SET_PATH, mMediaSetPath.toString());
    //            data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, item.getPath().toString());
    //            data.putInt(PhotoPage.KEY_ALBUMPAGE_TRANSITION, PhotoPage.MSG_ALBUMPAGE_STARTED);
    //            data.putBoolean(PhotoPage.KEY_START_IN_FILMSTRIP, startInFilmstrip);
    //            data.putBoolean(PhotoPage.KEY_IN_CAMERA_ROLL, mMediaSet.isCameraRoll());
    //            if (startInFilmstrip) {
    //                mContext.getStateManager().switchState(this, FilmstripPage.class, data);
    //            } else {
    //                mContext.getStateManager().startStateForResult(SinglePhotoPage.class, REQUEST_PHOTO, data);
    //            }
    //        }
    //    }
    //
    //    private void onGetContent(final MediaItem item) {
    //        DataManager dm = mContext.getDataManager();
    //        Activity activity = mContext;
    //        if (mData.getString(WoTuActivity.EXTRA_CROP) != null) {
    //            Uri uri = dm.getContentUri(item.getPath());
    //            Intent intent = new Intent(CropActivity.CROP_ACTION, uri)
    //                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
    //                    .putExtras(getData());
    //            if (mData.getParcelable(MediaStore.EXTRA_OUTPUT) == null) {
    //                intent.putExtra(CropExtras.KEY_RETURN_DATA, true);
    //            }
    //            activity.startActivity(intent);
    //            activity.finish();
    //        } else {
    //            Intent intent = new Intent(null, item.getContentUri())
    //                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    //            activity.setResult(Activity.RESULT_OK, intent);
    //            activity.finish();
    //        }
    //    }
}
