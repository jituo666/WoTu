package com.wotu.view.render;

import com.wotu.activity.WoTuContext;
import com.wotu.data.load.AlbumDataLoader;
import com.wotu.view.SlotView;
import com.wotu.view.SlotView.SlotRenderer;
import com.wotu.view.adapter.AlbumDataWindow;
import com.wotu.view.opengl.GLCanvas;

public class SlotViewRender implements SlotRenderer {

    private WoTuContext mContext;
    private AlbumDataWindow mDataWindow;
    private SlotView mSlotView;

    private final int mPlaceholderColor;
    private static final int CACHE_SIZE = 96;

    private class MyDataModelListener implements AlbumDataWindow.Listener {
        @Override
        public void onContentChanged() {
            mSlotView.invalidate();
        }

        @Override
        public void onSizeChanged(int size) {
            mSlotView.setSlotCount(size);
        }
    }

    public SlotViewRender(WoTuContext context, SlotView slotView, int color) {
        mContext = context;
        mPlaceholderColor = color;
    }

    public void setModel(AlbumDataLoader model) {
        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            mSlotView.setSlotCount(0);
            mDataWindow = null;
        }
        if (model != null) {
            mDataWindow = new AlbumDataWindow(mContext, model, CACHE_SIZE);
            mDataWindow.setListener(new MyDataModelListener());
            mSlotView.setSlotCount(model.size());
        }
    }

    @Override
    public void prepareDrawing() {

    }

    @Override
    public void onVisibleRangeChanged(int visibleStart, int visibleEnd) {

    }

    @Override
    public void onSlotSizeChanged(int width, int height) {

    }

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        return 0;
    }

}
