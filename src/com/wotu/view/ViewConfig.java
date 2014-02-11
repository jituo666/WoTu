package com.wotu.view;

import android.content.Context;
import android.content.res.Resources;

import com.wotu.R;

public final class ViewConfig {
    public static class AlbumSetPage {
        private static AlbumSetPage sInstance;

        public SlotView.Spec slotViewSpec;
        public SlotView.LabelSpec labelSpec;

        public static synchronized AlbumSetPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumSetPage(context);
            }
            return sInstance;
        }

        private AlbumSetPage(Context context) {
            Resources r = context.getResources();

            slotViewSpec = new SlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.albumset_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.albumset_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.albumset_slot_gap);

            labelSpec = new SlotView.LabelSpec();
            labelSpec.labelBackgroundHeight = r.getDimensionPixelSize(
                    R.dimen.albumset_label_background_height);
            labelSpec.titleOffset = r.getDimensionPixelSize(
                    R.dimen.albumset_title_offset);
            labelSpec.countOffset = r.getDimensionPixelSize(
                    R.dimen.albumset_count_offset);
            labelSpec.titleFontSize = r.getDimensionPixelSize(
                    R.dimen.albumset_title_font_size);
            labelSpec.countFontSize = r.getDimensionPixelSize(
                    R.dimen.albumset_count_font_size);
            labelSpec.leftMargin = r.getDimensionPixelSize(
                    R.dimen.albumset_left_margin);
            labelSpec.iconSize = r.getDimensionPixelSize(
                    R.dimen.albumset_icon_size);
        }
    }

    public static class AlbumPage {
        private static AlbumPage sInstance;

        public SlotView.Spec slotViewSpec;

        public static synchronized AlbumPage get(Context context) {
            if (sInstance == null) {
                sInstance = new AlbumPage(context);
            }
            return sInstance;
        }

        private AlbumPage(Context context) {
            Resources r = context.getResources();

            slotViewSpec = new SlotView.Spec();
            slotViewSpec.rowsLand = r.getInteger(R.integer.album_rows_land);
            slotViewSpec.rowsPort = r.getInteger(R.integer.album_rows_port);
            slotViewSpec.slotGap = r.getDimensionPixelSize(R.dimen.album_slot_gap);
        }
    }

}

