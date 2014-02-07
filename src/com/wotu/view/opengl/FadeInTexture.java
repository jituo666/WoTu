package com.wotu.view.opengl;

// FadeInTexture is a texture which begins with a color, then gradually animates
// into a given texture.
public class FadeInTexture extends FadeTexture implements Texture {
    @SuppressWarnings("unused")
    private static final String TAG = "FadeInTexture";

    private final int mColor;
    private final TiledTexture mTexture;

    public FadeInTexture(int color, TiledTexture texture) {
        super(texture.getWidth(), texture.getHeight(), texture.isOpaque());
        mColor = color;
        mTexture = texture;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        if (isAnimating()) {
            mTexture.drawMixed(canvas, mColor, getRatio(), x, y, w, h);
        } else {
            mTexture.draw(canvas, x, y, w, h);
        }
    }
}
