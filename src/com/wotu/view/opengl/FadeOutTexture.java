package com.wotu.view.opengl;


// FadeOutTexture is a texture which begins with a given texture, then gradually animates
// into fading out totally.
public class FadeOutTexture extends FadeTexture {
    @SuppressWarnings("unused")
    private static final String TAG = "FadeOutTexture";

    private final BasicTexture mTexture;

    public FadeOutTexture(BasicTexture texture) {
        super(texture.getWidth(), texture.getHeight(), texture.isOpaque());
        mTexture = texture;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        if (isAnimating()) {
            canvas.save(GLCanvas.SAVE_FLAG_ALPHA);
            canvas.setAlpha(getRatio());
            mTexture.draw(canvas, x, y, w, h);
            canvas.restore();
        }
    }
}
