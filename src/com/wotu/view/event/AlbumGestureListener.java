package com.wotu.view.event;

import com.wotu.activity.WoTuContext;
import com.wotu.view.GLController;
import com.wotu.view.SlotView;
import com.wotu.view.SlotView.GestureListener;
import com.wotu.view.layout.Layout;

import android.view.MotionEvent;
import android.view.GestureDetector;

public class AlbumGestureListener implements GestureDetector.OnGestureListener {

    private int mOverscrollEffect = SlotView.OVERSCROLL_3D;
    private boolean isDown;
    private GestureListener mListener;
    private Layout mLayout;
    private SlotView mSlotView;
    private WoTuContext mContext;
    private final ScrollerHelper mScroller;
    private UserInteractionListener mUIListener;

    public AlbumGestureListener(WoTuContext context, SlotView slotView) {
        mSlotView = slotView;
        mLayout = mSlotView.getLayout();
        mContext = context;
        mScroller = new ScrollerHelper(mContext.getAndroidContext());
    }

    public void setListener(GestureListener listener, UserInteractionListener uiListener) {
        mListener = listener;
        mUIListener = uiListener;
    }

    /*
     * We call the listener's onDown() when our onShowPress() is called and call
     * the listener's onUp() when we receive any further event.
     */
    @Override
    public void onShowPress(MotionEvent e) {
        GLController root = mContext.getGLController();
        root.lockRenderThread();
        try {
            if (isDown)
                return;
            int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY());
            if (index != SlotView.INDEX_NONE) {
                isDown = true;
                mListener.onDown(index);
            }
        } finally {
            root.unlockRenderThread();
        }
    }

    private void cancelDown(boolean byLongPress) {
        if (!isDown)
            return;
        isDown = false;
        mListener.onUp(byLongPress);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1,
            MotionEvent e2, float velocityX, float velocityY) {
        cancelDown(false);
        int scrollLimit = mLayout.getScrollLimit();
        if (scrollLimit == 0)
            return false;
        float velocity = SlotView.WIDE_SCROLL ? velocityX : velocityY;
        mScroller.fling((int) -velocity, 0, scrollLimit);
        if (mUIListener != null)
            mUIListener.onUserInteractionBegin();
        mSlotView.invalidate();
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1,
            MotionEvent e2, float distanceX, float distanceY) {
        cancelDown(false);
        float distance = SlotView.WIDE_SCROLL ? distanceX : distanceY;
        int overDistance = mScroller.startScroll(
                Math.round(distance), 0, mLayout.getScrollLimit());
        if (mOverscrollEffect == SlotView.OVERSCROLL_3D && overDistance != 0) {
            mSlotView.getPaper().overScroll(overDistance);
        }
        mSlotView.invalidate();
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        cancelDown(false);
        if (mSlotView.isDownInScrolling())
            return true;
        int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY());
        if (index != SlotView.INDEX_NONE)
            mListener.onSingleTapUp(index);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        cancelDown(true);
        if (mSlotView.isDownInScrolling())
            return;
        mSlotView.lockRendering();
        try {
            int index = mLayout.getSlotIndexByPosition(e.getX(), e.getY());
            if (index != SlotView.INDEX_NONE)
                mListener.onLongTap(index);
        } finally {
            mSlotView.unlockRendering();
        }
    }
}