package com.wotu.view.layout;

import android.graphics.Rect;

import com.wotu.view.SlotView;
import com.wotu.view.SlotView.SlotRenderer;

public abstract class Layout {

    public static final int INDEX_NONE = -1;

    protected boolean mWideScroll = false;
    protected int mVisibleStart;
    protected int mVisibleEnd;

    protected int mSlotCount;
    protected int mSlotWidth;
    protected int mSlotHeight;
    protected int mSlotGap;

    protected int mWidth;
    protected int mHeight;

    protected int mUnitCount;
    protected int mContentLength;
    protected int mScrollPosition;

    protected SlotRenderer mRenderer;

    public void setSlotRenderer(SlotRenderer render) {
        mRenderer = render;
    }

    public int getVisibleStart() {
        return mVisibleStart;
    }

    public int getSlotWidth() {
        return mSlotWidth;
    }

    public int getSlotHeight() {
        return mSlotHeight;
    }

    public int getVisibleEnd() {
        return mVisibleEnd;
    }

    public int getSlotCount() {
        return mSlotCount;
    }

    public int getScrollLimit() {
        int limit = SlotView.WIDE_SCROLL ? mContentLength - mWidth : mContentLength - mHeight;
        return limit <= 0 ? 0 : limit;
    }

    public boolean isWideScroll() {
        return mWideScroll;
    }

    public abstract boolean setSlotCount(int slotCount);

    public abstract void setSize(int w, int h);

    public abstract int getSlotIndexByPosition(float x, float y);

    public abstract Rect getSlotRect(int index, Rect rect);

    public abstract void setScrollPosition(int position);

}
