package com.wotu.view;

import com.wotu.anim.AnimTimer;
import com.wotu.data.utils.WoTuBitmapPool;
import com.wotu.utils.UtilsBase;
import com.wotu.view.opengl.GLCanvas;
import com.wotu.view.opengl.ScreenNail;
import com.wotu.view.opengl.TiledTexture;

import android.graphics.Bitmap;
import android.graphics.RectF;

// This is a ScreenNail wraps a Bitmap. There are some extra functions:
//
// - If we need to draw before the bitmap is available, we draw a rectange of
// placeholder color (gray).
//
// - When the the bitmap is available, and we have drawn the placeholder color
// before, we will do a fade-in animation.
public class TiledScreenNail implements ScreenNail {
    @SuppressWarnings("unused")
    private static final String TAG = "TiledScreenNail";

    // The duration of the fading animation in milliseconds
    private static final int DURATION = 180;

    private static int sMaxSide = 640;

    // These are special values for mAnimationStartTime
    private static final long ANIMATION_NOT_NEEDED = -1;
    private static final long ANIMATION_NEEDED = -2;
    private static final long ANIMATION_DONE = -3;

    private int mWidth;
    private int mHeight;
    private long mAnimationStartTime = ANIMATION_NOT_NEEDED;

    private Bitmap mBitmap;
    private TiledTexture mTexture;

    public TiledScreenNail(Bitmap bitmap) {
        mWidth = bitmap.getWidth();
        mHeight = bitmap.getHeight();
        mBitmap = bitmap;
        mTexture = new TiledTexture(bitmap);
    }

    public TiledScreenNail(int width, int height) {
        setSize(width, height);
    }

    // This gets overridden by bitmap_screennail_placeholder
    // in GalleryUtils.initialize
    private static int mPlaceholderColor = 0xFF222222;
    private static boolean mDrawPlaceholder = true;

    public static void setPlaceholderColor(int color) {
        mPlaceholderColor = color;
    }

    private void setSize(int width, int height) {
        if (width == 0 || height == 0) {
            width = sMaxSide;
            height = sMaxSide * 3 / 4;
        }
        float scale = Math.min(1, (float) sMaxSide / Math.max(width, height));
        mWidth = Math.round(scale * width);
        mHeight = Math.round(scale * height);
    }

    // Combines the two ScreenNails.
    // Returns the used one and recycle the unused one.
    public ScreenNail combine(ScreenNail other) {
        if (other == null) {
            return this;
        }

        if (!(other instanceof TiledScreenNail)) {
            recycle();
            return other;
        }

        // Now both are TiledScreenNail. Move over the information about width,
        // height, and Bitmap, then recycle the other.
        TiledScreenNail newer = (TiledScreenNail) other;
        mWidth = newer.mWidth;
        mHeight = newer.mHeight;
        if (newer.mTexture != null) {
            if (mBitmap != null) WoTuBitmapPool.getInstance().put(mBitmap);
            if (mTexture != null) mTexture.recycle();
            mBitmap = newer.mBitmap;
            mTexture = newer.mTexture;
            newer.mBitmap = null;
            newer.mTexture = null;
        }
        newer.recycle();
        return this;
    }

    public void updatePlaceholderSize(int width, int height) {
        if (mBitmap != null) return;
        if (width == 0 || height == 0) return;
        setSize(width, height);
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public void noDraw() {
    }

    @Override
    public void recycle() {
        if (mTexture != null) {
            mTexture.recycle();
            mTexture = null;
        }
        if (mBitmap != null) {
            WoTuBitmapPool.getInstance().put(mBitmap);
            mBitmap = null;
        }
    }

    public static void disableDrawPlaceholder() {
        mDrawPlaceholder = false;
    }

    public static void enableDrawPlaceholder() {
        mDrawPlaceholder = true;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        if (mTexture == null || !mTexture.isReady()) {
            if (mAnimationStartTime == ANIMATION_NOT_NEEDED) {
                mAnimationStartTime = ANIMATION_NEEDED;
            }
            if(mDrawPlaceholder) {
                canvas.fillRect(x, y, width, height, mPlaceholderColor);
            }
            return;
        }

        if (mAnimationStartTime == ANIMATION_NEEDED) {
            mAnimationStartTime = AnimTimer.get();
        }

        if (isAnimating()) {
            mTexture.drawMixed(canvas, mPlaceholderColor, getRatio(), x, y,
                    width, height);
        } else {
            mTexture.draw(canvas, x, y, width, height);
        }
    }

    @Override
    public void draw(GLCanvas canvas, RectF source, RectF dest) {
        if (mTexture == null || !mTexture.isReady()) {
            canvas.fillRect(dest.left, dest.top, dest.width(), dest.height(),
                    mPlaceholderColor);
            return;
        }

        mTexture.draw(canvas, source, dest);
    }

    public boolean isAnimating() {
        // The TiledTexture may not be uploaded completely yet.
        // In that case, we count it as animating state and we will draw
        // the placeholder in TileImageView.
        if (mTexture == null || !mTexture.isReady()) return true;
        if (mAnimationStartTime < 0) return false;
        if (AnimTimer.get() - mAnimationStartTime >= DURATION) {
            mAnimationStartTime = ANIMATION_DONE;
            return false;
        }
        return true;
    }

    private float getRatio() {
        float r = (float) (AnimTimer.get() - mAnimationStartTime) / DURATION;
        return UtilsBase.clamp(1.0f - r, 0.0f, 1.0f);
    }

    public boolean isShowingPlaceholder() {
        return (mBitmap == null) || isAnimating();
    }

    public TiledTexture getTexture() {
        return mTexture;
    }

    public static void setMaxSide(int size) {
        sMaxSide = size;
    }
}
