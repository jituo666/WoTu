package com.wotu.data;

import android.media.ExifInterface;
import android.util.SparseIntArray;

import com.wotu.common.WLog;
import com.wotu.R;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

public class ImageDetails implements Iterable<Entry<Integer, Object>> {
    @SuppressWarnings("unused")
    private static final String TAG = "ImageDetails";

    private TreeMap<Integer, Object> mDetails = new TreeMap<Integer, Object>();
    private SparseIntArray mUnits = new SparseIntArray();

    public static final int INDEX_TITLE = 1;
    public static final int INDEX_DESCRIPTION = 2;
    public static final int INDEX_DATETIME = 3;
    public static final int INDEX_LOCATION = 4;
    public static final int INDEX_WIDTH = 5;
    public static final int INDEX_HEIGHT = 6;
    public static final int INDEX_ORIENTATION = 7;
    public static final int INDEX_DURATION = 8;
    public static final int INDEX_MIMETYPE = 9;
    public static final int INDEX_SIZE = 10;

    // for EXIF
    public static final int INDEX_MAKE = 100;
    public static final int INDEX_MODEL = 101;
    public static final int INDEX_FLASH = 102;
    public static final int INDEX_FOCAL_LENGTH = 103;
    public static final int INDEX_WHITE_BALANCE = 104;
    public static final int INDEX_APERTURE = 105;
    public static final int INDEX_SHUTTER_SPEED = 106;
    public static final int INDEX_EXPOSURE_TIME = 107;
    public static final int INDEX_ISO = 108;

    // Put this last because it may be long.
    public static final int INDEX_PATH = 200;

    public static class FlashState {
        private static int FLASH_FIRED_MASK = 1;
        private static int FLASH_RETURN_MASK = 2 | 4;
        private static int FLASH_MODE_MASK = 8 | 16;
        private static int FLASH_FUNCTION_MASK = 32;
        private static int FLASH_RED_EYE_MASK = 64;
        private int mState;

        public FlashState(int state) {
            mState = state;
        }

        public boolean isFlashFired() {
            return (mState & FLASH_FIRED_MASK) != 0;
        }
    }

    public void addDetail(int index, Object value) {
        mDetails.put(index, value);
    }

    public Object getDetail(int index) {
        return mDetails.get(index);
    }

    public int size() {
        return mDetails.size();
    }

    public Iterator<Entry<Integer, Object>> iterator() {
        return mDetails.entrySet().iterator();
    }

    public void setUnit(int index, int unit) {
        mUnits.put(index, unit);
    }

    public boolean hasUnit(int key) {
        return mUnits.get(key) != 0;
    }

    public int getUnit(int index) {
        return mUnits.get(index);
    }

    private static void setExifData(ImageDetails details, ExifInterface exif, String tag,
            int key) {
        String value = exif.getAttribute(tag);
        if (value != null) {
            if (key == ImageDetails.INDEX_FLASH) {
                ImageDetails.FlashState state = new ImageDetails.FlashState(
                        Integer.valueOf(value.toString()));
                details.addDetail(key, state);
            } else {
                details.addDetail(key, value);
            }
        }
    }

    public static void extractExifInfo(ImageDetails details, String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            setExifData(details, exif, ExifInterface.TAG_FLASH, ImageDetails.INDEX_FLASH);
            setExifData(details, exif, ExifInterface.TAG_IMAGE_WIDTH, ImageDetails.INDEX_WIDTH);
            setExifData(details, exif, ExifInterface.TAG_IMAGE_LENGTH,
                    ImageDetails.INDEX_HEIGHT);
            setExifData(details, exif, ExifInterface.TAG_MAKE, ImageDetails.INDEX_MAKE);
            setExifData(details, exif, ExifInterface.TAG_MODEL, ImageDetails.INDEX_MODEL);
            setExifData(details, exif, ExifInterface.TAG_APERTURE, ImageDetails.INDEX_APERTURE);
            setExifData(details, exif, ExifInterface.TAG_ISO, ImageDetails.INDEX_ISO);
            setExifData(details, exif, ExifInterface.TAG_WHITE_BALANCE,
                    ImageDetails.INDEX_WHITE_BALANCE);
            setExifData(details, exif, ExifInterface.TAG_EXPOSURE_TIME,
                    ImageDetails.INDEX_EXPOSURE_TIME);

            double data = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0);
            if (data != 0f) {
                details.addDetail(ImageDetails.INDEX_FOCAL_LENGTH, data);
                details.setUnit(ImageDetails.INDEX_FOCAL_LENGTH, R.string.unit_mm);
            }
        } catch (IOException ex) {
            // ignore it.
            WLog.w(TAG, "", ex);
        }
    }
}
