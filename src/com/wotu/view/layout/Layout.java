package com.wotu.view.layout;

import com.wotu.view.SlotView;

public class Layout {

    private boolean mWideScroll = false;
    private int mVisibleStart;
    private int mVisibleEnd;

    private int mSlotCount;
    private int mSlotWidth;
    private int mSlotHeight;
    private int mSlotGap;

    private int mWidth;
    private int mHeight;

    private int mUnitCount;
    private int mContentLength;
    private int mScrollPosition;

    public Layout() {

    }

    public int getVisibleStart() {
        return mVisibleStart;
    }

    public int getSlotWidth() {
        return mWidth;
    }

    public int getSlotHeight() {
        return mHeight;
    }

    public int getVisibleEnd() {
        return mVisibleEnd;
    }

    public int getSlotIndexByPosition(float x, float y) {
        return 0;
    }

    public int getScrollLimit() {
        int limit = SlotView.WIDE_SCROLL ? mContentLength - mWidth : mContentLength - mHeight;
        return limit <= 0 ? 0 : limit;
    }
}
