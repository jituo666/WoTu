
package com.wotu.data.video;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;

import com.wotu.app.WoTuApp;
import com.wotu.common.ThreadPool.Job;
import com.wotu.common.ThreadPool.JobContext;
import com.wotu.common.WLog;
import com.wotu.data.MediaDetails;
import com.wotu.data.MediaItem;
import com.wotu.data.Path;
import com.wotu.data.cache.ImageRequest;
import com.wotu.data.image.LocalMediaItem;
import com.wotu.data.source.LocalAlbum;
import com.wotu.data.utils.BitmapUtils;
import com.wotu.utils.UpdateHelper;
import com.wotu.utils.UtilsCom;

// LocalVideo represents a video in the local storage.
public class LocalVideo extends LocalMediaItem {
    private static final String TAG = "LocalVideo";

    // Must preserve order between these indices and the order of the terms in
    // the following PROJECTION array.
    private static final int INDEX_ID = 0;
    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_MIME_TYPE = 2;
    private static final int INDEX_LATITUDE = 3;
    private static final int INDEX_LONGITUDE = 4;
    private static final int INDEX_DATE_TAKEN = 5;
    private static final int INDEX_DATE_ADDED = 6;
    private static final int INDEX_DATE_MODIFIED = 7;
    private static final int INDEX_DATA = 8;
    private static final int INDEX_DURATION = 9;
    private static final int INDEX_BUCKET_ID = 10;
    private static final int INDEX_SIZE = 11;
    private static final int INDEX_RESOLUTION = 12;

    public static final String ITEM_PATH = "/local/video/item";

    public static final String[] PROJECTION = new String[] {
            VideoColumns._ID,
            VideoColumns.TITLE,
            VideoColumns.MIME_TYPE,
            VideoColumns.LATITUDE,
            VideoColumns.LONGITUDE,
            VideoColumns.DATE_TAKEN,
            VideoColumns.DATE_ADDED,
            VideoColumns.DATE_MODIFIED,
            VideoColumns.DATA,
            VideoColumns.DURATION,
            VideoColumns.BUCKET_ID,
            VideoColumns.SIZE,
            VideoColumns.RESOLUTION,
    };

    private final WoTuApp mApplication;

    public int durationInSec;

    public LocalVideo(Path path, WoTuApp application, Cursor cursor) {
        super(path, nextVersionNumber());
        mApplication = application;
        loadFromCursor(cursor);
    }

    public LocalVideo(Path path, WoTuApp context, long id) {
        super(path, nextVersionNumber());
        mApplication = context;
        ContentResolver resolver = mApplication.getContentResolver();
        Uri uri = Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = LocalAlbum.getItemCursor(resolver, uri, PROJECTION, id);
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

    private void loadFromCursor(Cursor cursor) {
        id = cursor.getInt(INDEX_ID);
        caption = cursor.getString(INDEX_CAPTION);
        mimeType = cursor.getString(INDEX_MIME_TYPE);
        latitude = cursor.getDouble(INDEX_LATITUDE);
        longitude = cursor.getDouble(INDEX_LONGITUDE);
        dateTakenInMs = cursor.getLong(INDEX_DATE_TAKEN);
        filePath = cursor.getString(INDEX_DATA);
        durationInSec = cursor.getInt(INDEX_DURATION) / 1000;
        bucketId = cursor.getInt(INDEX_BUCKET_ID);
        fileSize = cursor.getLong(INDEX_SIZE);
        parseResolution(cursor.getString(INDEX_RESOLUTION));
    }

    private void parseResolution(String resolution) {
        if (resolution == null)
            return;
        int m = resolution.indexOf('x');
        if (m == -1)
            return;
        try {
            int w = Integer.parseInt(resolution.substring(0, m));
            int h = Integer.parseInt(resolution.substring(m + 1));
            width = w;
            height = h;
        } catch (Throwable t) {
            WLog.w(TAG, t);
        }
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
        durationInSec = uh.update(
                durationInSec, cursor.getInt(INDEX_DURATION) / 1000);
        bucketId = uh.update(bucketId, cursor.getInt(INDEX_BUCKET_ID));
        fileSize = uh.update(fileSize, cursor.getLong(INDEX_SIZE));
        return uh.isUpdated();
    }

    @Override
    public Job<Bitmap> requestImage(int type) {
        return new LocalVideoRequest(mApplication, getPath(), type, filePath);
    }

    public static class LocalVideoRequest extends ImageRequest {
        private String mLocalFilePath;

        LocalVideoRequest(WoTuApp application, Path path, int type,
                String localFilePath) {
            super(application, path, type, MediaItem.getTargetSize(type));
            mLocalFilePath = localFilePath;
        }

        @Override
        public Bitmap onDecodeOriginal(JobContext jc, int type) {
            Bitmap bitmap = BitmapUtils.createVideoThumbnail(mLocalFilePath);
            if (bitmap == null || jc.isCancelled())
                return null;
            return bitmap;
        }
    }

    @Override
    public Job<BitmapRegionDecoder> requestLargeImage() {
        throw new UnsupportedOperationException("Cannot regquest a large image"
                + " to a local video!");
    }

    @Override
    public int getSupportedOperations() {
        return SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_PLAY | SUPPORT_INFO;
    }

    @Override
    public void delete() {
        UtilsCom.assertNotInRenderThread();
        Uri baseUri = Video.Media.EXTERNAL_CONTENT_URI;
        mApplication.getContentResolver().delete(baseUri, "_id=?",
                new String[] {
                    String.valueOf(id)
                });
        mApplication.getDataManager().broadcastLocalDeletion();
    }

    @Override
    public void rotate(int degrees) {
        // TODO
    }

    @Override
    public Uri getContentUri() {
        Uri baseUri = Video.Media.EXTERNAL_CONTENT_URI;
        return baseUri.buildUpon().appendPath(String.valueOf(id)).build();
    }

    @Override
    public Uri getPlayUri() {
        return getContentUri();
    }

    @Override
    public int getMediaType() {
        return MEDIA_TYPE_VIDEO;
    }

    @Override
    public MediaDetails getDetails() {
        MediaDetails details = super.getDetails();
        int s = durationInSec;
        if (s > 0) {
            details.addDetail(MediaDetails.INDEX_DURATION, UtilsCom.formatDuration(
                    mApplication.getAndroidContext(), durationInSec));
        }
        return details;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
