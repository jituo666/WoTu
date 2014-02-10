package com.wotu.view.layout;

import android.graphics.Rect;

import com.wotu.anim.Animation;
import com.wotu.view.SlotView.Spec;
import com.wotu.view.layout.Layout;

public class NormalLayout extends Layout {

    protected Spec mSpec;
    private IntegerAnimation mVerticalPadding = new IntegerAnimation();
    private IntegerAnimation mHorizontalPadding = new IntegerAnimation();

    public NormalLayout() {

    }

    @Override
    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        initLayoutParameters();
    }

    @Override
    public boolean setSlotCount(int slotCount) {
        if (slotCount == mSlotCount)
            return false;
        if (mSlotCount != 0) {
            mHorizontalPadding.setEnabled(true);
            mVerticalPadding.setEnabled(true);
        }
        mSlotCount = slotCount;
        int hPadding = mHorizontalPadding.getTarget();
        int vPadding = mVerticalPadding.getTarget();
        initLayoutParameters();
        return vPadding != mVerticalPadding.getTarget()
                || hPadding != mHorizontalPadding.getTarget();
    }

    // Calculate
    // (1) mUnitCount: the number of slots we can fit into one column (or row).
    // (2) mContentLength: the width (or height) we need to display all the
    //     columns (rows).
    // (3) padding[]: the vertical and horizontal padding we need in order
    //     to put the slots towards to the center of the display.
    //
    // The "major" direction is the direction the user can scroll. The other
    // direction is the "minor" direction.
    //
    // The comments inside this method are the description when the major
    // directon is horizontal (X), and the minor directon is vertical (Y).
    private void initLayoutParameters(
            int majorLength, int minorLength, /* The view width and height */
            int majorUnitSize, int minorUnitSize, /* The slot width and height */
            int[] padding) {
        int unitCount = (minorLength + mSlotGap) / (minorUnitSize + mSlotGap);
        if (unitCount == 0)
            unitCount = 1;
        mUnitCount = unitCount;

        // We put extra padding above and below the column.
        int availableUnits = Math.min(mUnitCount, mSlotCount);
        int usedMinorLength = availableUnits * minorUnitSize +
                (availableUnits - 1) * mSlotGap;
        padding[0] = (minorLength - usedMinorLength) / 2;

        // Then calculate how many columns we need for all slots.
        int count = ((mSlotCount + mUnitCount - 1) / mUnitCount);
        mContentLength = count * majorUnitSize + (count - 1) * mSlotGap;

        // If the content length is less then the screen width, put
        // extra padding in left and right.
        padding[1] = Math.max(0, (majorLength - mContentLength) / 2);
    }

    private void initLayoutParameters() {
        // Initialize mSlotWidth and mSlotHeight from mSpec
        if (mSpec.slotWidth != -1) {
            mSlotGap = 0;
            mSlotWidth = mSpec.slotWidth;
            mSlotHeight = mSpec.slotHeight;
        } else {
            int rows = (mWidth > mHeight) ? mSpec.rowsLand : mSpec.rowsPort;
            mSlotGap = mSpec.slotGap;
            mSlotHeight = Math.max(1, (mHeight - (rows - 1) * mSlotGap) / rows);
            mSlotWidth = mSlotHeight - mSpec.slotHeightAdditional;
        }

        if (mRenderer != null) {
            mRenderer.onSlotSizeChanged(mSlotWidth, mSlotHeight);
        }

        int[] padding = new int[2];
        if (mWideScroll) {
            initLayoutParameters(mWidth, mHeight, mSlotWidth, mSlotHeight, padding);
            mVerticalPadding.startAnimateTo(padding[0]);
            mHorizontalPadding.startAnimateTo(padding[1]);
        } else {
            initLayoutParameters(mHeight, mWidth, mSlotHeight, mSlotWidth, padding);
            mVerticalPadding.startAnimateTo(padding[1]);
            mHorizontalPadding.startAnimateTo(padding[0]);
        }
        updateVisibleSlotRange();
    }

    @Override
    public Rect getSlotRect(int index, Rect rect) {
        int col, row;
        if (mWideScroll) {
            col = index / mUnitCount;
            row = index - col * mUnitCount;
        } else {
            row = index / mUnitCount;
            col = index - row * mUnitCount;
        }

        int x = mHorizontalPadding.get() + col * (mSlotWidth + mSlotGap);
        int y = mVerticalPadding.get() + row * (mSlotHeight + mSlotGap);
        rect.set(x, y, x + mSlotWidth, y + mSlotHeight);
        return rect;
    }

    private void updateVisibleSlotRange() {
        int position = mScrollPosition;

        if (mWideScroll) {
            int startCol = position / (mSlotWidth + mSlotGap);
            int start = Math.max(0, mUnitCount * startCol);
            int endCol = (position + mWidth + mSlotWidth + mSlotGap - 1) /
                    (mSlotWidth + mSlotGap);
            int end = Math.min(mSlotCount, mUnitCount * endCol);
            setVisibleRange(start, end);
        } else {
            int startRow = position / (mSlotHeight + mSlotGap);
            int start = Math.max(0, mUnitCount * startRow);
            int endRow = (position + mHeight + mSlotHeight + mSlotGap - 1) /
                    (mSlotHeight + mSlotGap);
            int end = Math.min(mSlotCount, mUnitCount * endRow);
            setVisibleRange(start, end);
        }
    }

    private void setVisibleRange(int start, int end) {
        if (start == mVisibleStart && end == mVisibleEnd)
            return;
        if (start < end) {
            mVisibleStart = start;
            mVisibleEnd = end;
        } else {
            mVisibleStart = mVisibleEnd = 0;
        }
        if (mRenderer != null) {
            mRenderer.onVisibleRangeChanged(mVisibleStart, mVisibleEnd);
        }
    }

    @Override
    public void setScrollPosition(int position) {
        if (mScrollPosition == position)
            return;
        mScrollPosition = position;
        updateVisibleSlotRange();
    }

    public int getSlotIndexByPosition(float x, float y) {
        int absoluteX = Math.round(x) + (mWideScroll ? mScrollPosition : 0);
        int absoluteY = Math.round(y) + (mWideScroll ? 0 : mScrollPosition);

        absoluteX -= mHorizontalPadding.get();
        absoluteY -= mVerticalPadding.get();

        if (absoluteX < 0 || absoluteY < 0) {
            return INDEX_NONE;
        }

        int columnIdx = absoluteX / (mSlotWidth + mSlotGap);
        int rowIdx = absoluteY / (mSlotHeight + mSlotGap);

        if (!mWideScroll && columnIdx >= mUnitCount) {
            return INDEX_NONE;
        }

        if (mWideScroll && rowIdx >= mUnitCount) {
            return INDEX_NONE;
        }

        if (absoluteX % (mSlotWidth + mSlotGap) >= mSlotWidth) {
            return INDEX_NONE;
        }

        if (absoluteY % (mSlotHeight + mSlotGap) >= mSlotHeight) {
            return INDEX_NONE;
        }

        int index = mWideScroll
                ? (columnIdx * mUnitCount + rowIdx)
                : (rowIdx * mUnitCount + columnIdx);

        return index >= mSlotCount ? INDEX_NONE : index;
    }

    private static class IntegerAnimation extends Animation {
        private int mTarget;
        private int mCurrent = 0;
        private int mFrom = 0;
        private boolean mEnabled = false;

        public void setEnabled(boolean enabled) {
            mEnabled = enabled;
        }

        public void startAnimateTo(int target) {
            if (!mEnabled) {
                mTarget = mCurrent = target;
                return;
            }
            if (target == mTarget)
                return;

            mFrom = mCurrent;
            mTarget = target;
            setDuration(180);
            start();
        }

        public int get() {
            return mCurrent;
        }

        public int getTarget() {
            return mTarget;
        }

        @Override
        protected void onCalculate(float progress) {
            mCurrent = Math.round(mFrom + progress * (mTarget - mFrom));
            if (progress == 1f)
                mEnabled = false;
        }
    }

}
