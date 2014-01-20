package com.wotu.anim;

import com.wotu.utils.UtilsBase;
import com.wotu.view.opengl.GLCanvas;


public class AlphaAnim extends CanvasAnim {
    private final float mStartAlpha;
    private final float mEndAlpha;
    private float mCurrentAlpha;

    public AlphaAnim(float from, float to) {
        mStartAlpha = from;
        mEndAlpha = to;
        mCurrentAlpha = from;
    }

    @Override
    public void apply(GLCanvas canvas) {
        canvas.multiplyAlpha(mCurrentAlpha);
    }

    @Override
    public int getCanvasSaveFlags() {
        return GLCanvas.SAVE_FLAG_ALPHA;
    }

    @Override
    protected void onCalculate(float progress) {
        mCurrentAlpha = UtilsBase.clamp(mStartAlpha
                + (mEndAlpha - mStartAlpha) * progress, 0f, 1f);
    }
}
