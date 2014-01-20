package com.wotu.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Process;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.wotu.anim.AnimTimer;
import com.wotu.anim.CanvasAnimation;
import com.wotu.common.WLog;
import com.wotu.util.UtilsBase;
import com.wotu.view.opengl.BasicTexture;
import com.wotu.view.opengl.GLCanvas;
import com.wotu.view.opengl.GLCanvasImpl;
import com.wotu.view.opengl.GLRoot;
import com.wotu.view.opengl.UploadedTexture;
import com.wotu.view.opengl.WoTuEGLConfigChooser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class GLRootView extends GLSurfaceView implements Renderer, GLRoot {

    private static final String TAG = "GLRootView";

    private static final int FLAG_INITIALIZED = 1;
    private static final int FLAG_NEED_LAYOUT = 2;
    private final WoTuEGLConfigChooser mEglConfigChooser = new WoTuEGLConfigChooser();
    private GL11 mGL;
    private final ReentrantLock mRenderLock = new ReentrantLock();
    private final Condition mFreezeCondition = mRenderLock.newCondition();
    private boolean mFreeze;
    private int mFlags = FLAG_NEED_LAYOUT;
    private volatile boolean mRenderRequested = false;
    private OrientationSource mOrientationSource;
    // mCompensation is the difference between the UI orientation on GLCanvas
    // and the framework orientation. See OrientationManager for details.
    private int mCompensation;
    // mCompensationMatrix maps the coordinates of touch events. It is kept sync
    // with mCompensation.
    private Matrix mCompensationMatrix = new Matrix();
    private int mDisplayRotation;
    //
    private GLCanvas mCanvas;
    private GLView mContentView;
    private boolean mInDownState = false;

    private final ArrayList<CanvasAnimation> mAnimations = new ArrayList<CanvasAnimation>();
    private final ArrayDeque<OnGLIdleListener> mIdleListeners = new ArrayDeque<OnGLIdleListener>();
    private final IdleRunner mIdleRunner = new IdleRunner();

    public GLRootView(Context context) {
        this(context, null);
    }

    public GLRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFlags |= FLAG_INITIALIZED;
        setEGLConfigChooser(mEglConfigChooser);
        setRenderer(this);
        getHolder().setFormat(PixelFormat.RGB_565);

    }

    //----------------------------------------Open gl rendering----------------------------------------
    @Override
    public void onSurfaceCreated(GL10 gl1, EGLConfig config) {
        GL11 gl = (GL11) gl1;
        if (mGL != null) {
            WLog.i(TAG, "GLObject has changed from " + mGL + " to " + gl);// The GL Object has changed
        }
        mRenderLock.lock();
        try {
            mGL = gl;
            mCanvas = new GLCanvasImpl(mGL);
            BasicTexture.invalidateAllTextures();
        } finally {
            mRenderLock.unlock();
        }
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onSurfaceChanged(GL10 gl1, int width, int height) {
        WLog.i(TAG, "onSurfaceChanged: " + width + "x" + height + ", gl10: " + gl1.toString());
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
        GL11 gl = (GL11) gl1;
        UtilsBase.assertTrue(mGL == gl);
        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);// 在此可以设置背景色
        mCanvas.setSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        AnimTimer.update();
        mRenderLock.lock();
        while (mFreeze) {
            mFreezeCondition.awaitUninterruptibly();
        }
        try {
            onDrawFrameLocked(gl);
        } finally {
            mRenderLock.unlock();
        }
    }

    private void onDrawFrameLocked(GL10 gl) {
        //// release the unbound textures and deleted buffers.
        mCanvas.deleteRecycledResources();

        // reset texture upload limit
        UploadedTexture.resetUploadLimit();
        mRenderRequested = false;
        if ((mFlags & FLAG_NEED_LAYOUT) != 0)
            layoutContentPane(); //重新布局各个子views

        mCanvas.save(GLCanvas.SAVE_FLAG_ALL);
        rotateCanvas(-mCompensation);
        if (mContentView != null) {
            mContentView.render(mCanvas); //绘制各个子views
        }
        mCanvas.restore();

        if (!mAnimations.isEmpty()) {
            long now = AnimTimer.get();
            for (int i = 0, n = mAnimations.size(); i < n; i++) {
                mAnimations.get(i).setStartTime(now);
            }
            mAnimations.clear();
        }

        if (UploadedTexture.uploadLimitReached()) {
            requestRender();
        }

        synchronized (mIdleListeners) {
            if (!mIdleListeners.isEmpty())
                mIdleRunner.enable();
        }
    }

    //----------------------------------------end Open gl rendering----------------------------------------

    @Override
    public void requestRender() {
        if (mRenderRequested)
            return;
        mRenderRequested = true;
        super.requestRender();
    }

    @Override
    public void requestLayoutContentPane() {
        mRenderLock.lock();
        try {
            if (mContentView == null || (mFlags & FLAG_NEED_LAYOUT) != 0)
                return;
            // "View" system will invoke onLayout() for initialization(bug ?), we
            // have to ignore it since the GLThread is not ready yet.
            if ((mFlags & FLAG_INITIALIZED) == 0)
                return;

            mFlags |= FLAG_NEED_LAYOUT; // 设置标志位，请求一次layout
            requestRender();
        } finally {
            mRenderLock.unlock();
        }
    }

    private void layoutContentPane() {
        mFlags &= ~FLAG_NEED_LAYOUT;

        int w = getWidth();
        int h = getHeight();
        int displayRotation = 0;
        int compensation = 0;

        // Get the new orientation values
        if (mOrientationSource != null) {
            displayRotation = mOrientationSource.getDisplayRotation();
            compensation = mOrientationSource.getCompensation();
        } else {
            displayRotation = 0;
            compensation = 0;
        }

        if (mCompensation != compensation) {
            mCompensation = compensation;
            if (mCompensation % 180 != 0) {
                mCompensationMatrix.setRotate(mCompensation);
                // move center to origin before rotation
                mCompensationMatrix.preTranslate(-w / 2, -h / 2);
                // align with the new origin after rotation
                mCompensationMatrix.postTranslate(h / 2, w / 2);
            } else {
                mCompensationMatrix.setRotate(mCompensation, w / 2, h / 2);
            }
        }
        mDisplayRotation = displayRotation;

        // Do the actual layout.
        if (mCompensation % 180 != 0) {
            int tmp = w;
            w = h;
            h = tmp;
        }
        WLog.i(TAG, "layout content pane " + w + "x" + h + " (compensation " + mCompensation + ")");
        if (mContentView != null && w != 0 && h != 0) {
            mContentView.layout(0, 0, w, h); // 触发每个glview的layout
        }
    }

    private void rotateCanvas(int degrees) {
        if (degrees == 0)
            return;
        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;
        mCanvas.translate(cx, cy);
        mCanvas.rotate(degrees, 0, 0, 1);
        if (degrees % 180 != 0) {
            mCanvas.translate(-cy, -cx);
        } else {
            mCanvas.translate(-cx, -cy);
        }
    }

    @Override
    public void lockRenderThread() {
        mRenderLock.lock();
    }

    @Override
    public void unlockRenderThread() {
        mRenderLock.unlock();
    }

    @Override
    public void onPause() {
        unfreeze();
        super.onPause();
    }

    @Override
    public void setOrientationSource(OrientationSource source) {
        mOrientationSource = source;
    }

    @Override
    public int getDisplayRotation() {
        return mDisplayRotation;
    }

    @Override
    public int getCompensation() {
        return mCompensation;
    }

    @Override
    public Matrix getCompensationMatrix() {
        return mCompensationMatrix;
    }

    @Override
    public void freeze() {
        mRenderLock.lock();
        mFreeze = true;
        mRenderLock.unlock();
    }

    @Override
    public void unfreeze() {
        mRenderLock.lock();
        mFreeze = false;
        mFreezeCondition.signalAll();
        mRenderLock.unlock();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) { // 分发事件给各个子views
        if (!isEnabled())
            return false;

        int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP) {
            mInDownState = false;
        } else if (!mInDownState && action != MotionEvent.ACTION_DOWN) {
            return false;
        }

        if (mCompensation != 0) {
            event.transform(mCompensationMatrix);
        }

        mRenderLock.lock();
        try {
            // If this has been detached from root, we don't need to handle event
            boolean handled = mContentView != null
                    && mContentView.dispatchTouchEvent(event);
            if (action == MotionEvent.ACTION_DOWN && handled) {
                mInDownState = true;
            }
            return handled;
        } finally {
            mRenderLock.unlock();
        }
    }

    @Override
    public void addOnGLIdleListener(OnGLIdleListener listener) {
        synchronized (mIdleListeners) {
            mIdleListeners.addLast(listener);
            mIdleRunner.enable();
        }
    }

    @Override
    public void registerLaunchedAnimation(CanvasAnimation animation) {
        mAnimations.add(animation);
    }

    @Override
    public void setContentPane(GLView content) {
        if (mContentView == content) return;
        if (mContentView != null) {
            if (mInDownState) {
                long now = SystemClock.uptimeMillis();
                MotionEvent cancelEvent = MotionEvent.obtain(
                        now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                mContentView.dispatchTouchEvent(cancelEvent);
                cancelEvent.recycle();
                mInDownState = false;
            }
            mContentView.detachFromRoot();
            BasicTexture.yieldAllTextures();
        }
        mContentView = content;
        if (content != null) {
            content.attachToRoot(this);
            requestLayoutContentPane();
        }
    }

    @Override
    public void setLightsOutMode(boolean enabled) { //控制系统UI组件(status bar ,action bar等)的显示与否
        int flags = enabled
                ? SYSTEM_UI_FLAG_LOW_PROFILE
                | SYSTEM_UI_FLAG_FULLSCREEN
                | SYSTEM_UI_FLAG_LAYOUT_STABLE
                : 0;
        setSystemUiVisibility(flags);

    }

    // We need to unfreeze in the following methods and in onPause().
    // These methods will wait on GLThread. If we have freezed the GLRootView,
    // the GLThread will wait on main thread to call unfreeze and cause dead
    // lock.
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        unfreeze();
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        unfreeze();
        super.surfaceCreated(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        unfreeze();
        super.surfaceDestroyed(holder);
    }

    @Override
    protected void onDetachedFromWindow() {
        unfreeze();
        super.onDetachedFromWindow();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            unfreeze();
        } finally {
            super.finalize();
        }
    }

    private class IdleRunner implements Runnable {
        // true if the idle runner is in the queue
        private boolean mActive = false;

        @Override
        public void run() {
            OnGLIdleListener listener;
            synchronized (mIdleListeners) {
                mActive = false;
                if (mIdleListeners.isEmpty())
                    return;
                listener = mIdleListeners.removeFirst();
            }
            mRenderLock.lock();
            try {
                if (!listener.onGLIdle(mCanvas, mRenderRequested)) // Mainly used for uploading textures
                    return;
            } finally {
                mRenderLock.unlock();
            }
            synchronized (mIdleListeners) {
                mIdleListeners.addLast(listener);
                if (!mRenderRequested)
                    enable();
            }
        }

        public void enable() {
            // Who gets the flag can add it to the queue
            if (mActive)
                return;
            mActive = true;
            queueEvent(this);
        }
    }

}
