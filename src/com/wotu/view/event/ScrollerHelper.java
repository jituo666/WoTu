package com.wotu.view.event;

import com.wotu.utils.UtilsBase;

import android.content.Context;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

public class ScrollerHelper {
    private OverScroller mScroller;
    private int mOverflingDistance;
    private boolean mOverflingEnabled;

    public ScrollerHelper(Context context) {
        mScroller = new OverScroller(context);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        mOverflingDistance = configuration.getScaledOverflingDistance();
    }

    public void setOverfling(boolean enabled) {
        mOverflingEnabled = enabled;
    }

    /**
     * Call this when you want to know the new location. The position will be
     * updated and can be obtained by getPosition(). Returns true if  the
     * animation is not yet finished.
     */
    public boolean advanceAnimation(long currentTimeMillis) {
        return mScroller.computeScrollOffset();
    }

    public boolean isFinished() {
        return mScroller.isFinished();
    }

    public void forceFinished() {
        mScroller.forceFinished(true);
    }

    public int getPosition() {
        return mScroller.getCurrX();
    }

    public float getCurrVelocity() {
        return mScroller.getCurrVelocity();
    }

    public void setPosition(int position) {
        mScroller.startScroll(
                position, 0,    // startX, startY
                0, 0, 0);       // dx, dy, duration

        // This forces the scroller to reach the final position.
        mScroller.abortAnimation();
    }

    public void fling(int velocity, int min, int max) {
        int currX = getPosition();
        mScroller.fling(
                currX, 0,      // startX, startY
                velocity, 0,   // velocityX, velocityY
                min, max,      // minX, maxX
                0, 0,          // minY, maxY
                mOverflingEnabled ? mOverflingDistance : 0, 0);
    }

    // Returns the distance that over the scroll limit.
    public int startScroll(int distance, int min, int max) {
        int currPosition = mScroller.getCurrX();
        int finalPosition = mScroller.isFinished() ? currPosition :
                mScroller.getFinalX();
        int newPosition = UtilsBase.clamp(finalPosition + distance, min, max);
        if (newPosition != currPosition) {
            mScroller.startScroll(
                currPosition, 0,                    // startX, startY
                newPosition - currPosition, 0, 0);  // dx, dy, duration
        }
        return finalPosition + distance - newPosition;
    }
}
