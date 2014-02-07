package com.wotu.view.render;

import com.wotu.R;
import com.wotu.view.opengl.FadeOutTexture;
import com.wotu.view.opengl.GLCanvas;
import com.wotu.view.opengl.NinePatchTexture;
import com.wotu.view.opengl.ResourceTexture;
import com.wotu.view.opengl.Texture;

import android.content.Context;
import android.graphics.Rect;

public abstract class SlotViewRenderBase implements SlotView.SlotRenderer {

    private final ResourceTexture mVideoOverlay;
    private final ResourceTexture mVideoPlayIcon;
    private final ResourceTexture mPanoramaIcon;
    private final NinePatchTexture mFramePressed;
    private final NinePatchTexture mFrameSelected;
    private FadeOutTexture mFramePressedUp;

    protected SlotViewRenderBase(Context context) {
        mVideoOverlay = new ResourceTexture(context, R.drawable.ic_video_thumb);
        mVideoPlayIcon = new ResourceTexture(context, R.drawable.ic_gallery_play);
        mPanoramaIcon = new ResourceTexture(context, R.drawable.ic_360pano_holo_light);
        mFramePressed = new NinePatchTexture(context, R.drawable.grid_pressed);
        mFrameSelected = new NinePatchTexture(context, R.drawable.grid_selected);
    }

    protected void drawContent(GLCanvas canvas,
            Texture content, int width, int height, int rotation) {
        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);

        // The content is always rendered in to the largest square that fits
        // inside the slot, aligned to the top of the slot.
        width = height = Math.min(width, height);
        if (rotation != 0) {
            canvas.translate(width / 2, height / 2);
            canvas.rotate(rotation, 0, 0, 1);
            canvas.translate(-width / 2, -height / 2);
        }

        // Fit the content into the box
        float scale = Math.min(
                (float) width / content.getWidth(),
                (float) height / content.getHeight());
        canvas.scale(scale, scale, 1);
        content.draw(canvas, 0, 0);

        canvas.restore();
    }

    protected void drawVideoOverlay(GLCanvas canvas, int width, int height) {
        // Scale the video overlay to the height of the thumbnail and put it
        // on the left side.
        ResourceTexture v = mVideoOverlay;
        float scale = (float) height / v.getHeight();
        int w = Math.round(scale * v.getWidth());
        int h = Math.round(scale * v.getHeight());
        v.draw(canvas, 0, 0, w, h);

        int s = Math.min(width, height) / 6;
        mVideoPlayIcon.draw(canvas, (width - s) / 2, (height - s) / 2, s, s);
    }

    protected void drawPanoramaIcon(GLCanvas canvas, int width, int height) {
        int iconSize = Math.min(width, height) / 6;
        mPanoramaIcon.draw(canvas, (width - iconSize) / 2, (height - iconSize) / 2,
                iconSize, iconSize);
    }

    protected boolean isPressedUpFrameFinished() {
        if (mFramePressedUp != null) {
            if (mFramePressedUp.isAnimating()) {
                return false;
            } else {
                mFramePressedUp = null;
            }
        }
        return true;
    }

    protected void drawPressedUpFrame(GLCanvas canvas, int width, int height) {
        if (mFramePressedUp == null) {
            mFramePressedUp = new FadeOutTexture(mFramePressed);
        }
        drawFrame(canvas, mFramePressed.getPaddings(), mFramePressedUp, 0, 0, width, height);
    }

    protected void drawPressedFrame(GLCanvas canvas, int width, int height) {
        drawFrame(canvas, mFramePressed.getPaddings(), mFramePressed, 0, 0, width, height);
    }

    protected void drawSelectedFrame(GLCanvas canvas, int width, int height) {
        drawFrame(canvas, mFrameSelected.getPaddings(), mFrameSelected, 0, 0, width, height);
    }

    protected static void drawFrame(GLCanvas canvas, Rect padding, Texture frame,
            int x, int y, int width, int height) {
        frame.draw(canvas, x - padding.left, y - padding.top, width + padding.left + padding.right,
                 height + padding.top + padding.bottom);
    }
}
