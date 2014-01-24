package com.wotu.view;

public class RelativePosition {
    private float mAbsoluteX;
    private float mAbsoluteY;
    private float mReferenceX;
    private float mReferenceY;

    public void setAbsolutePosition(int absoluteX, int absoluteY) {
        mAbsoluteX = absoluteX;
        mAbsoluteY = absoluteY;
    }

    public void setReferencePosition(int x, int y) {
        mReferenceX = x;
        mReferenceY = y;
    }

    public float getX() {
        return mAbsoluteX - mReferenceX;
    }

    public float getY() {
        return mAbsoluteY - mReferenceY;
    }
}
