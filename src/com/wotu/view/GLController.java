package com.wotu.view;

import android.graphics.Matrix;

import com.wotu.activity.OrientationSource;
import com.wotu.anim.CanvasAnim;
import com.wotu.view.opengl.GLCanvas;


public interface GLController {

    // Listener will be called when GL is idle AND before each frame.
    // Mainly used for uploading textures.
    public static interface OnGLIdleListener {
        public boolean onGLIdle(GLCanvas canvas, boolean renderRequested);
    }

    public void addOnGLIdleListener(OnGLIdleListener listener);

    public void registerLaunchedAnimation(CanvasAnim animation);

    public void requestRender();

    public void requestLayoutContentPane();

    public void lockRenderThread();

    public void unlockRenderThread();

    public void setContentPane(GLView content);

    public void setOrientationSource(OrientationSource source);

    public int getDisplayRotation();

    public int getCompensation();

    public Matrix getCompensationMatrix();

    public void freeze();

    public void unfreeze();

    public void setLightsOutMode(boolean enabled);
}