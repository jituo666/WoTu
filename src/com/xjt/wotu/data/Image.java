
package com.xjt.wotu.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

import com.xjt.wotu.app.WoTuApp;
import com.xjt.wotu.common.ThreadPool.Job;
import com.xjt.wotu.data.bitmap.BitmapUtils;
import com.xjt.wotu.util.UpdateHelper;
import com.xjt.wotu.util.UtilsCom;

public class Image extends ImageItem {

    private final WoTuApp mApplication;

    // database fields
    public int id;
    public String caption;
    public String mimeType;
    public long fileSize;
    public double latitude = INVALID_LATLNG;
    public double longitude = INVALID_LATLNG;
    public long dateTakenInMs;
    public long dateAddedInSec;
    public long dateModifiedInSec;
    public String filePath;
    public int bucketId;
    public int rotation;
    public int width;
    public int height;

    private static final int INDEX_ID = 0;
    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_MIME_TYPE = 2;
    private static final int INDEX_LATITUDE = 3;
    private static final int INDEX_LONGITUDE = 4;
    private static final int INDEX_DATE_TAKEN = 5;
    private static final int INDEX_DATE_ADDED = 6;
    private static final int INDEX_DATE_MODIFIED = 7;
    private static final int INDEX_DATA = 8;
    private static final int INDEX_ORIENTATION = 9;
    private static final int INDEX_BUCKET_ID = 10;
    private static final int INDEX_SIZE = 11;
    private static final int INDEX_WIDTH = 12;
    private static final int INDEX_HEIGHT = 13;
    //
    static final String[] PROJECTION = {
            ImageColumns._ID,           // 0
            ImageColumns.TITLE,         // 1
            ImageColumns.MIME_TYPE,     // 2
            ImageColumns.LATITUDE,      // 3
            ImageColumns.LONGITUDE,     // 4
            ImageColumns.DATE_TAKEN,    // 5
            ImageColumns.DATE_ADDED,    // 6
            ImageColumns.DATE_MODIFIED, // 7
            ImageColumns.DATA,          // 8
            ImageColumns.ORIENTATION,   // 9
            ImageColumns.BUCKET_ID,     // 10
            ImageColumns.SIZE,          // 11
            ImageColumns.WIDTH,         // 12
            ImageColumns.HEIGHT
    // 13
    };

    public Image(MediaPath path, WoTuApp application, int id) {
        super(path, nextVersionNumber());
        mApplication = application;
        ContentResolver resolver = mApplication.getContentResolver();
        Uri uri = Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = resolver.query(uri, PROJECTION, "_id=?", new String[] {
                String.valueOf(id)
        }, null);
        if (cursor == null) {
            throw new RuntimeException("cannot get cursor for: " + path);
        }
        try {
            if (cursor.moveToNext()) {
                loadFromCursor(cursor);
            } else {
                throw new RuntimeException("cannot find data for: " + path);
            }
        } finally {
            cursor.close();
        }
    }

    public Image(MediaPath path, WoTuApp app, Cursor cursor) {
        super(path, nextVersionNumber());
        mApplication = app;
        loadFromCursor(cursor);
    }

    private void loadFromCursor(Cursor cursor) {
        id = cursor.getInt(INDEX_ID);
        caption = cursor.getString(INDEX_CAPTION);
        mimeType = cursor.getString(INDEX_MIME_TYPE);
        latitude = cursor.getDouble(INDEX_LATITUDE);
        longitude = cursor.getDouble(INDEX_LONGITUDE);
        dateTakenInMs = cursor.getLong(INDEX_DATE_TAKEN);
        dateAddedInSec = cursor.getLong(INDEX_DATE_ADDED);
        dateModifiedInSec = cursor.getLong(INDEX_DATE_MODIFIED);
        filePath = cursor.getString(INDEX_DATA);
        rotation = cursor.getInt(INDEX_ORIENTATION);
        bucketId = cursor.getInt(INDEX_BUCKET_ID);
        fileSize = cursor.getLong(INDEX_SIZE);
        width = cursor.getInt(INDEX_WIDTH);
        height = cursor.getInt(INDEX_HEIGHT);
    }

    @Override
    protected boolean updateFromCursor(Cursor cursor) {
        UpdateHelper uh = new UpdateHelper();
        id = uh.update(id, cursor.getInt(INDEX_ID));
        caption = uh.update(caption, cursor.getString(INDEX_CAPTION));
        mimeType = uh.update(mimeType, cursor.getString(INDEX_MIME_TYPE));
        latitude = uh.update(latitude, cursor.getDouble(INDEX_LATITUDE));
        longitude = uh.update(longitude, cursor.getDouble(INDEX_LONGITUDE));
        dateTakenInMs = uh.update(
                dateTakenInMs, cursor.getLong(INDEX_DATE_TAKEN));
        dateAddedInSec = uh.update(
                dateAddedInSec, cursor.getLong(INDEX_DATE_ADDED));
        dateModifiedInSec = uh.update(
                dateModifiedInSec, cursor.getLong(INDEX_DATE_MODIFIED));
        filePath = uh.update(filePath, cursor.getString(INDEX_DATA));
        rotation = uh.update(rotation, cursor.getInt(INDEX_ORIENTATION));
        bucketId = uh.update(bucketId, cursor.getInt(INDEX_BUCKET_ID));
        fileSize = uh.update(fileSize, cursor.getLong(INDEX_SIZE));
        width = uh.update(width, cursor.getInt(INDEX_WIDTH));
        height = uh.update(height, cursor.getInt(INDEX_HEIGHT));
        return uh.isUpdated();
    }

    @Override
    public Job<Bitmap> requestThumnail(int type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Job<BitmapRegionDecoder> requestImage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getSupportedOperations() {
        int operation = SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_CROP
                | SUPPORT_SETAS | SUPPORT_EDIT | SUPPORT_INFO;
        if (BitmapUtils.isSupportedByRegionDecoder(mimeType)) {
            operation |= SUPPORT_FULL_IMAGE;
        }

        if (BitmapUtils.isRotationSupported(mimeType)) {
            operation |= SUPPORT_ROTATE;
        }

        if (UtilsCom.isValidLocation(latitude, longitude)) {
            operation |= SUPPORT_SHOW_ON_MAP;
        }
        return operation;
    }
}
