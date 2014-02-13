package com.wotu.view.opengl;


import java.util.ArrayDeque;

import com.wotu.view.GLController;
import com.wotu.view.GLController.OnGLIdleListener;

public class TextureUploader implements OnGLIdleListener {
    private static final int INIT_CAPACITY = 64;
    private static final int QUOTA_PER_FRAME = 1;

    private final ArrayDeque<UploadedTexture> mFgTextures = new ArrayDeque<UploadedTexture>(INIT_CAPACITY);
    private final ArrayDeque<UploadedTexture> mBgTextures = new ArrayDeque<UploadedTexture>(INIT_CAPACITY);
    private final GLController mGLController;
    private volatile boolean mIsQueued = false;

    public TextureUploader(GLController controller) {
        mGLController = controller;
    }

    public synchronized void clear() {
        while (!mFgTextures.isEmpty()) {
            mFgTextures.pop().setIsUploading(false);
        }
        while (!mBgTextures.isEmpty()) {
            mBgTextures.pop().setIsUploading(false);
        }
    }

    // caller should hold synchronized on "this"
    private void queueSelfIfNeed() {
        if (mIsQueued) return;
        mIsQueued = true;
        mGLController.addOnGLIdleListener(this);
    }

    public synchronized void addBgTexture(UploadedTexture t) {
        if (t.isContentValid()) return;
        mBgTextures.addLast(t);
        t.setIsUploading(true);
        queueSelfIfNeed();
    }

    public synchronized void addFgTexture(UploadedTexture t) {
        if (t.isContentValid()) return;
        mFgTextures.addLast(t);
        t.setIsUploading(true);
        queueSelfIfNeed();
    }

    private int upload(GLCanvas canvas, ArrayDeque<UploadedTexture> deque,
            int uploadQuota, boolean isBackground) {
        while (uploadQuota > 0) {
            UploadedTexture t;
            synchronized (this) {
                if (deque.isEmpty()) break;
                t = deque.removeFirst();
                t.setIsUploading(false);
                if (t.isContentValid()) continue;

                // this has to be protected by the synchronized block
                // to prevent the inner bitmap get recycled
                t.updateContent(canvas);
            }

            // It will took some more time for a texture to be drawn for
            // the first time.
            // Thus, when scrolling, if a new column appears on screen,
            // it may cause a UI jank even these textures are uploaded.
            if (isBackground) t.draw(canvas, 0, 0);
            --uploadQuota;
        }
        return uploadQuota;
    }

    @Override
    public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
        int uploadQuota = QUOTA_PER_FRAME;
        uploadQuota = upload(canvas, mFgTextures, uploadQuota, false);
        if (uploadQuota < QUOTA_PER_FRAME) mGLController.requestRender();
        upload(canvas, mBgTextures, uploadQuota, true);
        synchronized (this) {
            mIsQueued = !mFgTextures.isEmpty() || !mBgTextures.isEmpty();
            return mIsQueued;
        }
    }
}
