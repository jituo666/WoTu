package com.wotu.view;

import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;

import com.wotu.activity.WoTuContext;
import com.wotu.anim.AnimTimer;
import com.wotu.anim.Animation;
import com.wotu.common.SynchronizedHandler;
import com.wotu.utils.UtilsBase;
import com.wotu.view.event.AlbumGestureListener;
import com.wotu.view.event.ScrollerHelper;
import com.wotu.view.event.UserInteractionListener;
import com.wotu.view.layout.Layout;
import com.wotu.view.opengl.GLCanvas;

public class SlotView extends GLView {

    private static final String TAG = "SlotView";

    private WoTuContext mContext;

    public static final int INDEX_NONE = -1;
    public static final boolean WIDE_SCROLL = true;
    public static final int OVERSCROLL_3D = 0;
    public static final int OVERSCROLL_SYSTEM = 1;
    public static final int OVERSCROLL_NONE = 2;
    public static final int RENDER_MORE_PASS = 1;
    public static final int RENDER_MORE_FRAME = 2;

    private int mOverscrollEffect = OVERSCROLL_3D;

    private Layout mLayout;
    private boolean mMoreAnimation = false;
    private SlotAnimation mAnimation = null;

    private int[] mRequestRenderSlots = new int[16];
    private SlotRenderer mRenderer;
    private Paper mPaper = new Paper();
    private final GestureDetector mGestureDetector;
    private AlbumGestureListener mAlbumGestureListener;
    private final ScrollerHelper mScroller;
    private GestureListener mGestureListener;
    private UserInteractionListener mUIListener;
    private final SynchronizedHandler mHandler;
    private boolean mDownInScrolling;
    // to prevent allocating memory
    private final Rect mTempRect = new Rect();

    public interface GestureListener {
        public void onDown(int index);

        public void onUp(boolean followedByLongPress);

        public void onSingleTapUp(int index);

        public void onLongTap(int index);

        public void onScrollPositionChanged(int position, int total);
    }

    public static class SimpleListener implements GestureListener {
        @Override
        public void onDown(int index) {
        }

        @Override
        public void onUp(boolean followedByLongPress) {
        }

        @Override
        public void onSingleTapUp(int index) {
        }

        @Override
        public void onLongTap(int index) {
        }

        @Override
        public void onScrollPositionChanged(int position, int total) {
        }
    }

    public static interface SlotRenderer {
        public void prepareDrawing();

        public void onVisibleRangeChanged(int visibleStart, int visibleEnd);

        public void onSlotSizeChanged(int width, int height);

        public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height);
    }

    public static abstract class SlotAnimation extends Animation {
        protected float mProgress = 0;

        public SlotAnimation() {
            setInterpolator(new DecelerateInterpolator(4));
            setDuration(1500);
        }

        @Override
        protected void onCalculate(float progress) {
            mProgress = progress;
        }

        abstract public void apply(GLCanvas canvas, int slotIndex, Rect target);
    }

    // This Spec class is used to specify the size of each slot in the SlotView.
    // There are two ways to do it:
    //
    // (1) Specify slotWidth and slotHeight: they specify the width and height
    //     of each slot. The number of rows and the gap between slots will be
    //     determined automatically.
    // (2) Specify rowsLand, rowsPort, and slotGap: they specify the number
    //     of rows in landscape/portrait mode and the gap between slots. The
    //     width and height of each slot is determined automatically.
    //
    // The initial value of -1 means they are not specified.
    public static class Spec {
        public int slotWidth = -1;
        public int slotHeight = -1;

        public int rowsLand = -1;
        public int rowsPort = -1;
        public int slotGap = -1;
        public int slotHeightAdditional = 0;
    }

    public static class LabelSpec {
        public int labelBackgroundHeight;
        public int titleOffset;
        public int countOffset;
        public int titleFontSize;
        public int countFontSize;
        public int leftMargin;
        public int iconSize;
    }

    public SlotView(WoTuContext context) {
        mContext = context;
        mAlbumGestureListener = new AlbumGestureListener(mContext, this);
        mGestureDetector = new GestureDetector(context.getAndroidContext(), mAlbumGestureListener);
        mScroller = new ScrollerHelper(mContext.getAndroidContext());
        mHandler = new SynchronizedHandler(mContext.getGLController());
    }

    public void setSlotLayout(Layout layout) {
        mLayout = layout;
        mAlbumGestureListener.setLayout(layout);
    }

    public void setSlotRenderer(SlotRenderer render) {
        mRenderer = render;
        mLayout.setSlotRenderer(render);
        if (mRenderer != null) {
            mRenderer.onSlotSizeChanged(mLayout.getSlotWidth(), mLayout.getSlotHeight());
            mRenderer.onVisibleRangeChanged(mLayout.getVisibleStart(), mLayout.getVisibleEnd());
        }
    }

    public void setGestureListener(GestureListener listener) {
        mGestureListener = listener;
        mAlbumGestureListener.setGestureListener(listener);
    }

    public void setUserInteractionListener(UserInteractionListener listener) {
        mUIListener = listener;
        mAlbumGestureListener.setUserInteractionListener(listener);
    }
    
    @Override
    protected void onLayout(boolean changeSize, int l, int t, int r, int b) {
        if (!changeSize)
            return;
        // Make sure we are still at a resonable scroll position after the size
        // is changed (like orientation change). We choose to keep the center
        // visible slot still visible. This is arbitrary but reasonable.
        int visibleIndex = (mLayout.getVisibleStart() + mLayout.getVisibleEnd()) / 2;
        mLayout.setSize(r - l, b - t);
        makeSlotVisible(visibleIndex);
        if (mOverscrollEffect == OVERSCROLL_3D) {
            mPaper.setSize(r - l, b - t);
        }
    }

    public void setCenterIndex(int index) {
        int slotCount = mLayout.getSlotCount();
        if (index < 0 || index >= slotCount) {
            return;
        }
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        int position = mLayout.isWideScroll()
                ? (rect.left + rect.right - getWidth()) / 2
                : (rect.top + rect.bottom - getHeight()) / 2;
        setScrollPosition(position);
    }

    public void makeSlotVisible(int index) {
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        int visibleBegin = mLayout.isWideScroll() ? mScrollX : mScrollY;
        int visibleLength = mLayout.isWideScroll() ? getWidth() : getHeight();
        int visibleEnd = visibleBegin + visibleLength;
        int slotBegin = mLayout.isWideScroll() ? rect.left : rect.top;
        int slotEnd = mLayout.isWideScroll() ? rect.right : rect.bottom;

        int position = visibleBegin;
        if (visibleLength < slotEnd - slotBegin) {
            position = visibleBegin;
        } else if (slotBegin < visibleBegin) {
            position = slotBegin;
        } else if (slotEnd > visibleEnd) {
            position = slotEnd - visibleLength;
        }

        setScrollPosition(position);
    }

    public void setScrollPosition(int position) {
        position = UtilsBase.clamp(position, 0, mLayout.getScrollLimit());
        mScroller.setPosition(position);
        updateScrollPosition(position, false);
    }

    public void setSlotCount(int slotCount) {
        mLayout.setSlotCount(slotCount);
    }

    private void updateScrollPosition(int position, boolean force) {
        if (!force && (mLayout.isWideScroll() ? position == mScrollX : position == mScrollY))
            return;
        if (mLayout.isWideScroll()) {
            mScrollX = position;
        } else {
            mScrollY = position;
        }
        mLayout.setScrollPosition(position);
        onScrollPositionChanged(position);
    }

    protected void onScrollPositionChanged(int newPosition) {
        int limit = mLayout.getScrollLimit();
        mGestureListener.onScrollPositionChanged(newPosition, limit);
    }

    private int renderItem(GLCanvas canvas, int index, int pass, boolean paperActive) {
        canvas.save(GLCanvas.SAVE_FLAG_ALPHA | GLCanvas.SAVE_FLAG_MATRIX);
        Rect rect = mLayout.getSlotRect(index, mTempRect);
        if (paperActive) {
            canvas.multiplyMatrix(mPaper.getTransform(rect, mScrollX), 0);
        } else {
            canvas.translate(rect.left, rect.top, 0);
        }
        if (mAnimation != null && mAnimation.isActive()) {
            mAnimation.apply(canvas, index, rect);
        }
        //WLog.i(TAG, " x:" + rect.left + " y:" + rect.top);
        int result = mRenderer.renderSlot(canvas, index, pass, rect.right - rect.left, rect.bottom - rect.top);
        canvas.restore();
        return result;
    }

    @Override
    protected void render(GLCanvas canvas) {
        super.render(canvas);

        if (mRenderer == null)
            return;
        mRenderer.prepareDrawing();

        long animTime = AnimTimer.get();
        boolean more = mScroller.advanceAnimation(animTime);
        int oldX = mScrollX;
        updateScrollPosition(mScroller.getPosition(), false);

        boolean paperActive = false;
        if (mOverscrollEffect == OVERSCROLL_3D) {
            // Check if an edge is reached and notify mPaper if so.
            int newX = mScrollX;
            int limit = mLayout.getScrollLimit();
            if (oldX > 0 && newX == 0 || oldX < limit && newX == limit) {
                float v = mScroller.getCurrVelocity();
                if (newX == limit)
                    v = -v;

                // I don't know why, but getCurrVelocity() can return NaN.
                if (!Float.isNaN(v)) {
                    mPaper.edgeReached(v);
                }
            }
            paperActive = mPaper.advanceAnimation();
        }

        more |= paperActive;

        if (mAnimation != null) {
            more |= mAnimation.calculate(animTime);
        }

        canvas.translate(-mScrollX, -mScrollY);

        int requestCount = 0;
        int requestedSlot[] = expandIntArray(mRequestRenderSlots, mLayout.getVisibleEnd() - mLayout.getVisibleStart());

        for (int i = mLayout.getVisibleEnd() - 1; i >= mLayout.getVisibleStart(); --i) {
            int r = renderItem(canvas, i, 0, paperActive);
            if ((r & RENDER_MORE_FRAME) != 0)
                more = true;
            if ((r & RENDER_MORE_PASS) != 0)
                requestedSlot[requestCount++] = i;
        }

        for (int pass = 1; requestCount != 0; ++pass) {
            int newCount = 0;
            for (int i = 0; i < requestCount; ++i) {
                int r = renderItem(canvas, requestedSlot[i], pass, paperActive);
                if ((r & RENDER_MORE_FRAME) != 0)
                    more = true;
                if ((r & RENDER_MORE_PASS) != 0)
                    requestedSlot[newCount++] = i;
            }
            requestCount = newCount;
        }

        canvas.translate(mScrollX, mScrollY);

        if (more)
            invalidate();

        final UserInteractionListener listener = mUIListener;
        if (mMoreAnimation && !more && listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onUserInteractionEnd();
                }
            });
        }
        mMoreAnimation = more;
    }

    private static int[] expandIntArray(int array[], int capacity) {
        while (array.length < capacity) {
            array = new int[array.length * 2];
        }
        return array;
    }

    public Rect getSlotRect(int slotIndex) {
        return mLayout.getSlotRect(slotIndex, new Rect());
    }

    public Layout getLayout() {
        return mLayout;
    }

    public Paper getPaper() {
        return mPaper;
    }

    public int getVisibleStart() {
        return mLayout.getVisibleStart();
    }

    public int getVisibleEnd() {
        return mLayout.getVisibleEnd();
    }

    public int getScrollX() {
        return mScrollX;
    }

    public int getScrollY() {
        return mScrollY;
    }

    public boolean isDownInScrolling() {
        return mDownInScrolling;
    }

    @Override
    protected boolean onTouch(MotionEvent event) {
        if (mUIListener != null)
            mUIListener.onUserInteraction();
        mGestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            mDownInScrolling = !mScroller.isFinished();
            mScroller.forceFinished();
            break;
        case MotionEvent.ACTION_UP:
            mPaper.onRelease();
            invalidate();
            break;
        }
        return true;
    }
}
