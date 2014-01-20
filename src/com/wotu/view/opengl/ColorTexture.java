package com.wotu.view.opengl;

import com.wotu.utils.UtilsBase;


// ColorTexture is a texture which fills the rectangle with the specified color.
public class ColorTexture implements Texture {

    private final int mColor;
    private int mWidth;
    private int mHeight;

    public ColorTexture(int color) {
        mColor = color;
        mWidth = 1;
        mHeight = 1;
    }

    public void draw(GLCanvas canvas, int x, int y) {
        draw(canvas, x, y, mWidth, mHeight);
    }

    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        canvas.fillRect(x, y, w, h, mColor);
    }

    public boolean isOpaque() {
        return UtilsBase.isOpaque(mColor);
    }

    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }
}
