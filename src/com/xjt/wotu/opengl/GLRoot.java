package com.xjt.wotu.opengl;

import android.graphics.Matrix;

import com.xjt.wotu.animation.CanvasAnimation;
import com.xjt.wotu.ui.GLView;
import com.xjt.wotu.ui.OrientationSource;


public interface GLRoot {

    // Listener will be called when GL is idle AND before each frame.
    // Mainly used for uploading textures.
    public static interface OnGLIdleListener {
        public boolean onGLIdle(GLCanvas canvas, boolean renderRequested);
    }

    public void addOnGLIdleListener(OnGLIdleListener listener);

    public void registerLaunchedAnimation(CanvasAnimation animation);

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