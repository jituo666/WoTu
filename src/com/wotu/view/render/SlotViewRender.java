package com.wotu.view.render;

import android.graphics.Color;

import com.wotu.activity.WoTuContext;
import com.wotu.app.MediaSelector;
import com.wotu.data.MediaObject;
import com.wotu.data.Path;
import com.wotu.data.load.AlbumDataLoader;
import com.wotu.view.SlotView;
import com.wotu.view.SlotView.SlotRenderer;
import com.wotu.view.adapter.AlbumDataWindow;
import com.wotu.view.opengl.ColorTexture;
import com.wotu.view.opengl.FadeInTexture;
import com.wotu.view.opengl.GLCanvas;
import com.wotu.view.opengl.Texture;
import com.wotu.view.opengl.TiledTexture;

public class SlotViewRender extends SlotViewRenderBase {

    private WoTuContext mContext;
    private AlbumDataWindow mDataWindow;
    private SlotView mSlotView;
    private Path mHighlightItemPath = null;
    private SlotFilter mSlotFilter;

    private static final int PLACE_HOLDER_COLOR = Color.BLACK;
    private static final int CACHE_SIZE = 96;

    private final ColorTexture mWaitLoadingTexture;
    private final int mPlaceholderColor = PLACE_HOLDER_COLOR;
    
    private int mPressedIndex = -1;
    private boolean mAnimatePressedUp;
    private MediaSelector mMediaSelector;
    private boolean mInSelectionMode;

    public interface SlotFilter {
        public boolean acceptSlot(int index);
    }

    private class MyDataListener implements AlbumDataWindow.DataListener {
        @Override
        public void onContentChanged() {
            mSlotView.invalidate();
        }

        @Override
        public void onSizeChanged(int size) {
            mSlotView.setSlotCount(size);
        }
    }

    public SlotViewRender(WoTuContext context, SlotView slotView, MediaSelector selector) {
        super(context.getAndroidContext());
        mContext = context;
        mMediaSelector = selector;
        mWaitLoadingTexture = new ColorTexture(mPlaceholderColor);
        mWaitLoadingTexture.setSize(1, 1);
    }

    public void setModel(AlbumDataLoader model) {
        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            mSlotView.setSlotCount(0);
            mDataWindow = null;
        }
        if (model != null) {
            mDataWindow = new AlbumDataWindow(mContext, model, CACHE_SIZE);
            mDataWindow.setListener(new MyDataListener());
            mSlotView.setSlotCount(model.size());
        }
    }

    public void setHighlightItemPath(Path path) {
        if (mHighlightItemPath == path)
            return;
        mHighlightItemPath = path;
        mSlotView.invalidate();
    }

    public void setSlotFilter(SlotFilter slotFilter) {
        mSlotFilter = slotFilter;
    }

    @Override
    public void prepareDrawing() {
        mInSelectionMode = mMediaSelector.inSelectionMode();
    }

    @Override
    public void onVisibleRangeChanged(int visibleStart, int visibleEnd) {
        if (mDataWindow != null) {
            mDataWindow.setActiveWindow(visibleStart, visibleEnd);
        }
    }

    @Override
    public void onSlotSizeChanged(int width, int height) {

    }

    private static Texture checkTexture(Texture texture) {
        return (texture instanceof TiledTexture)
                && !((TiledTexture) texture).isReady()
                ? null
                : texture;
    }
    
    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        if (mSlotFilter != null && !mSlotFilter.acceptSlot(index))
            return 0;

        AlbumDataWindow.AlbumEntry entry = mDataWindow.get(index);

        int renderRequestFlags = 0;

        Texture content = checkTexture(entry.content);
        if (content == null) {
            content = mWaitLoadingTexture;
            entry.isWaitDisplayed = true;
        } else if (entry.isWaitDisplayed) {
            entry.isWaitDisplayed = false;
            content = new FadeInTexture(mPlaceholderColor, entry.bitmapTexture);
            entry.content = content;
        }
        drawContent(canvas, content, width, height, entry.rotation);
        if ((content instanceof FadeInTexture) &&
                ((FadeInTexture) content).isAnimating()) {
            renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
        }

        renderRequestFlags |= renderOverlay(canvas, index, entry, width, height);

        return renderRequestFlags;
    }

    private int renderOverlay(GLCanvas canvas, int index,
            AlbumDataWindow.AlbumEntry entry, int width, int height) {
        int renderRequestFlags = 0;
        if (mPressedIndex == index) {
            if (mAnimatePressedUp) {
                drawPressedUpFrame(canvas, width, height);
                renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
                if (isPressedUpFrameFinished()) {
                    mAnimatePressedUp = false;
                    mPressedIndex = -1;
                }
            } else {
                drawPressedFrame(canvas, width, height);
            }
        } else if ((entry.path != null) && (mHighlightItemPath == entry.path)) {
            drawSelectedFrame(canvas, width, height);
        } else if (mInSelectionMode && mMediaSelector.isItemSelected(entry.path)) {
            drawSelectedFrame(canvas, width, height);
        }
        return renderRequestFlags;
    }

    //
    public void setPressedIndex(int index) {
        if (mPressedIndex == index)
            return;
        mPressedIndex = index;
        mSlotView.invalidate();
    }

    public void setPressedUp() {
        if (mPressedIndex == -1)
            return;
        mAnimatePressedUp = true;
        mSlotView.invalidate();
    }

    public void resume() {
        mDataWindow.resume();
    }

    public void pause() {
        mDataWindow.pause();
    }
}
