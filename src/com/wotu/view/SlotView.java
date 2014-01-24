package com.wotu.view;

import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.wotu.activity.WoTuContext;
import com.wotu.common.SynchronizedHandler;
import com.wotu.view.event.AlbumGestureListener;
import com.wotu.view.event.ScrollerHelper;
import com.wotu.view.event.UserInteractionListener;
import com.wotu.view.layout.Layout;
import com.wotu.view.opengl.GLCanvas;

public class SlotView extends GLView {

    private WoTuContext mContext;

    public static final int INDEX_NONE = -1;
    public static final boolean WIDE_SCROLL = true;
    public static final int OVERSCROLL_3D = 0;
    public static final int OVERSCROLL_SYSTEM = 1;
    public static final int OVERSCROLL_NONE = 2;

    private SlotRenderer mRenderer;
    private final Layout mLayout = new Layout();
    private final Paper mPaper = new Paper();
    private final GestureDetector mGestureDetector;
    private final ScrollerHelper mScroller;
    private GestureListener mGestureListener;
    private UserInteractionListener mUIListener;
    private final SynchronizedHandler mHandler;
    private boolean mDownInScrolling;

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
        mGestureDetector = new GestureDetector(
                context.getAndroidContext(), new AlbumGestureListener(mContext, this));
        mScroller = new ScrollerHelper(mContext.getAndroidContext());
        mHandler = new SynchronizedHandler(mContext.getGLController());
    }

    public void setSlotRenderer(SlotRenderer render) {
        mRenderer = render;
        if (mRenderer != null) {
            mRenderer.onSlotSizeChanged(mLayout.getSlotWidth(), mLayout.getSlotHeight());
            mRenderer.onVisibleRangeChanged(mLayout.getVisibleStart(), mLayout.getVisibleEnd());
        }
    }

    public void setGestureListener(GestureListener listener) {
        mGestureListener = listener;
    }

    public void setSlotCount(int slotCount) {

    }

    @Override
    protected void render(GLCanvas canvas) {
        super.render(canvas);
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
