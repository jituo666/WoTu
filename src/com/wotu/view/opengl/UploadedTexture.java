package com.wotu.view.opengl;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLUtils;

import com.wotu.util.UtilsBase;

import java.util.HashMap;

import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11Ext;

public abstract class UploadedTexture extends BasicTexture {
    private static final String TAG = "UploadedTexture";
    // To prevent keeping allocation the borders, we store those used borders here.
    // Since the length will be power of two, it won't use too much memory.
    private static HashMap<BorderKey, Bitmap> sBorderLines =
            new HashMap<BorderKey, Bitmap>();
    private static BorderKey sBorderKey = new BorderKey();
    private static final int UPLOAD_LIMIT = 100;
    private static int sUploadedCount;
    protected static int[] sTextureId = new int[1];
    protected static float[] sCropRect = new float[4];

    protected Bitmap mBitmap;
    private boolean mIsUploading = false;
    private boolean mOpaque = true;
    private boolean mContentValid = true;
    private boolean mThrottled = false;
    private int mBorder;

    protected UploadedTexture() {
        this(false);
    }

    protected UploadedTexture(boolean hasBorder) {
        super(null, 0, STATE_UNLOADED);
        if (hasBorder) {
            setBorder(true);
            mBorder = 1;
        }
    }

    public void setOpaque(boolean isOpaque) {
        mOpaque = isOpaque;
    }

    public boolean isOpaque() {
        return mOpaque;
    }

    protected void setIsUploading(boolean uploading) {
        mIsUploading = uploading;
    }

    public boolean isUploading() {
        return mIsUploading;
    }

    public static void resetUploadLimit() {
        sUploadedCount = 0;
    }

    public static boolean uploadLimitReached() {
        return sUploadedCount > UPLOAD_LIMIT;
    }

    protected void setThrottled(boolean throttled) {
        mThrottled = throttled;
    }

    protected abstract Bitmap onGetBitmap();

    protected abstract void onFreeBitmap(Bitmap bitmap);

    private Bitmap getBitmap() {
        if (mBitmap == null) {
            mBitmap = onGetBitmap();
            int w = mBitmap.getWidth() + mBorder * 2;
            int h = mBitmap.getHeight() + mBorder * 2;
            if (mWidth == UNSPECIFIED) {
                setSize(w, h);
            }
        }
        return mBitmap;
    }

    private void freeBitmap() {
        UtilsBase.assertTrue(mBitmap != null);
        onFreeBitmap(mBitmap);
        mBitmap = null;
    }

    protected void invalidateContent() {
        if (mBitmap != null)
            freeBitmap();
        mContentValid = false;
        mWidth = UNSPECIFIED;
        mHeight = UNSPECIFIED;
    }

    /**
     * Whether the content on GPU is valid.
     */
    public boolean isContentValid() {
        return isLoaded() && mContentValid;
    }

    /**
     * Updates the content on GPU's memory.
     * 
     * @param canvas
     */
    public void updateContent(GLCanvas canvas) {
        if (!isLoaded()) {
            if (mThrottled && ++sUploadedCount > UPLOAD_LIMIT) {
                return;
            }
            uploadToCanvas(canvas);
        } else if (!mContentValid) {
            Bitmap bitmap = getBitmap();
            int format = GLUtils.getInternalFormat(bitmap);
            int type = GLUtils.getType(bitmap);
            canvas.getGLInstance().glBindTexture(GL11.GL_TEXTURE_2D, mId);
            GLUtils.texSubImage2D(GL11.GL_TEXTURE_2D, 0, mBorder, mBorder, bitmap, format, type);
            freeBitmap();
            mContentValid = true;
        }
    }

    private void uploadToCanvas(GLCanvas canvas) {
        GL11 gl = canvas.getGLInstance();
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            try {
                int bWidth = bitmap.getWidth();
                int bHeight = bitmap.getHeight();
                int texWidth = getTextureWidth();
                int texHeight = getTextureHeight();

                UtilsBase.assertTrue(bWidth <= texWidth && bHeight <= texHeight);
                sCropRect[0] = mBorder;
                sCropRect[1] = mBorder + bHeight;
                sCropRect[2] = bWidth;
                sCropRect[3] = -bHeight;

                // Upload the bitmap to a new texture.
                GLId.glGenTextures(1, sTextureId, 0);
                gl.glBindTexture(GL11.GL_TEXTURE_2D, sTextureId[0]);
                gl.glTexParameterfv(GL11.GL_TEXTURE_2D, GL11Ext.GL_TEXTURE_CROP_RECT_OES, sCropRect, 0);
                gl.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP_TO_EDGE);
                gl.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP_TO_EDGE);
                gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                gl.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                if (bWidth == texWidth && bHeight == texHeight) {
                    GLUtils.texImage2D(GL11.GL_TEXTURE_2D, 0, bitmap, 0);
                } else {
                    int format = GLUtils.getInternalFormat(bitmap);
                    int type = GLUtils.getType(bitmap);
                    Config config = bitmap.getConfig();

                    gl.glTexImage2D(GL11.GL_TEXTURE_2D, 0, format,
                            texWidth, texHeight, 0, format, type, null);
                    GLUtils.texSubImage2D(GL11.GL_TEXTURE_2D, 0,
                            mBorder, mBorder, bitmap, format, type);

                    if (mBorder > 0) {
                        // Left border
                        Bitmap line = getBorderLine(true, config, texHeight);
                        GLUtils.texSubImage2D(GL11.GL_TEXTURE_2D, 0,
                                0, 0, line, format, type);

                        // Top border
                        line = getBorderLine(false, config, texWidth);
                        GLUtils.texSubImage2D(GL11.GL_TEXTURE_2D, 0,
                                0, 0, line, format, type);
                    }

                    // Right border
                    if (mBorder + bWidth < texWidth) {
                        Bitmap line = getBorderLine(true, config, texHeight);
                        GLUtils.texSubImage2D(GL11.GL_TEXTURE_2D, 0,
                                mBorder + bWidth, 0, line, format, type);
                    }

                    // Bottom border
                    if (mBorder + bHeight < texHeight) {
                        Bitmap line = getBorderLine(false, config, texWidth);
                        GLUtils.texSubImage2D(GL11.GL_TEXTURE_2D, 0,
                                0, mBorder + bHeight, line, format, type);
                    }
                }
            } finally {
                freeBitmap();
            }
        } else {
            mState = STATE_ERROR;
            throw new RuntimeException("Texture load fail, no bitmap");
        }
    }

    private static Bitmap getBorderLine(
            boolean vertical, Config config, int length) {
        BorderKey key = sBorderKey;
        key.vertical = vertical;
        key.config = config;
        key.length = length;
        Bitmap bitmap = sBorderLines.get(key);
        if (bitmap == null) {
            bitmap = vertical
                    ? Bitmap.createBitmap(1, length, config)
                    : Bitmap.createBitmap(length, 1, config);
            sBorderLines.put(key.clone(), bitmap);
        }
        return bitmap;
    }

    @Override
    protected boolean onBind(GLCanvas canvas) {
        updateContent(canvas);
        return isContentValid();
    }

    @Override
    protected int getTarget() {
        return GL11.GL_TEXTURE_2D;
    }

    @Override
    public void recycle() {
        super.recycle();
        if (mBitmap != null)
            freeBitmap();
    }

    private static class BorderKey implements Cloneable {
        public boolean vertical;
        public Config config;
        public int length;

        @Override
        public int hashCode() {
            int x = config.hashCode() ^ length;
            return vertical ? x : -x;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof BorderKey))
                return false;
            BorderKey o = (BorderKey) object;
            return vertical == o.vertical
                    && config == o.config && length == o.length;
        }

        @Override
        public BorderKey clone() {
            try {
                return (BorderKey) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }
}
