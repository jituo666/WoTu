
package com.wotu.common;

import android.os.Handler;
import android.os.Message;
import com.wotu.utils.UtilsBase;
import com.wotu.view.GLController;

public class SynchronizedHandler extends Handler {

    private final GLController mRoot;

    public SynchronizedHandler(GLController root) {
        mRoot = UtilsBase.checkNotNull(root);
    }

    @Override
    public void dispatchMessage(Message message) {
        mRoot.lockRenderThread();
        try {
            super.dispatchMessage(message);
        } finally {
            mRoot.unlockRenderThread();
        }
    }
}
