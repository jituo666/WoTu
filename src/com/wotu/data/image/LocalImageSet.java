package com.wotu.data.image;

import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.VideoColumns;

import com.wotu.app.WoTuApp;
import com.wotu.common.WLog;
import com.wotu.data.DataManager;
import com.wotu.data.DataNotifier;
import com.wotu.data.MediaPath;
import com.wotu.data.MediaSetObject;
import com.wotu.util.UtilsBase;
import com.wotu.util.UtilsCom;

import java.util.ArrayList;

/**
 * ImageSet to manage images
 * 
 * @author jetoo
 * 
 */
public class LocalImageSet extends MediaSetObject {

    private static final String TAG = "LocalImageSet";

    public static final String KEY_BUCKET_ID = "bucketId";
    private static final String[] COUNT_PROJECTION = {
            "count(*)"
    };

    private static final int INVALID_COUNT = -1;
    private final String mWhereClause;
    private final String mOrderClause;
    private final Uri mBaseUri;
    private final String[] mProjection;

    private final WoTuApp mApplication;
    private final ContentResolver mResolver;
    private final int mBucketId;
    private final String mName;
    private final DataNotifier mNotifier;
    private int mCachedCount = INVALID_COUNT;

    public LocalImageSet(MediaPath path, WoTuApp application, int bucketId, String name) {
        super(path, nextVersionNumber());
        mApplication = application;
        mResolver = application.getContentResolver();
        mBucketId = bucketId;
        mName = getLocalizedName(application.getResources(), bucketId, name);

        mWhereClause = ImageColumns.BUCKET_ID + " = ?";
        mOrderClause = ImageColumns.DATE_TAKEN + " DESC, "
                + ImageColumns._ID + " DESC";
        mBaseUri = Images.Media.EXTERNAL_CONTENT_URI;
        mProjection = LocalImage.PROJECTION;

        mNotifier = new DataNotifier(this, mBaseUri, application);
    }

    public LocalImageSet(MediaPath path, WoTuApp application, int bucketId) {
        this(path, application, bucketId,
                ImageSetList.getBucketName(application.getContentResolver(),
                        bucketId));
    }

    @Override
    public Uri getContentUri() {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon()
                .appendQueryParameter(KEY_BUCKET_ID,
                        String.valueOf(mBucketId)).build();
    }

    /*
     * Returns the media items in the range [start, start + count). The number
     * of media items returned may be less than the specified count if there are
     * not enough media items available. The number of media items available may
     * not be consistent with the return value of getMediaItemCount() because
     * the contents of database may have already changed.
     */
    public ArrayList<Image> getMediaItem(int start, int count) {
        DataManager dataManager = mApplication.getDataManager();
        Uri uri = mBaseUri.buildUpon()
                .appendQueryParameter("limit", start + "," + count).build();
        ArrayList<Image> list = new ArrayList<Image>();
        UtilsCom.assertNotInRenderThread();
        Cursor cursor = mResolver.query(
                uri, mProjection, mWhereClause,
                new String[] {
                String.valueOf(mBucketId)
                },
                mOrderClause);
        if (cursor == null) {
            WLog.w(TAG, "query fail: " + uri);
            return list;
        }

        try {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0); // _id must be in the first column
                MediaPath childPath = mItemPath.getChild(id);
                Image item = loadOrUpdateItem(childPath, cursor,
                        dataManager, mApplication);
                list.add(item);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    private static Image loadOrUpdateItem(MediaPath path, Cursor cursor,
            DataManager dataManager, WoTuApp app) {
        Image item = (Image) dataManager.peekMediaObject(path);
        if (item == null) {
            item = new Image(path, app, cursor);

        } else {
            item.updateContent(cursor);
        }
        return item;
    }

    // The pids array are sorted by the (path) id.
    public static Image[] getMediaItemById(
            WoTuApp application, ArrayList<Integer> ids) {
        // get the lower and upper bound of (path) id
        Image[] result = new Image[ids.size()];
        if (ids.isEmpty())
            return result;
        int idLow = ids.get(0);
        int idHigh = ids.get(ids.size() - 1);

        // prepare the query parameters
        Uri baseUri;
        String[] projection;
        MediaPath itemPath;

        baseUri = Images.Media.EXTERNAL_CONTENT_URI;
        projection = Image.PROJECTION;

        ContentResolver resolver = application.getContentResolver();
        DataManager dataManager = application.getDataManager();
        Cursor cursor = resolver.query(baseUri, projection, "_id BETWEEN ? AND ?",
                new String[] {
                        String.valueOf(idLow), String.valueOf(idHigh)
                },
                "_id");
        if (cursor == null) {
            WLog.w(TAG, "query fail" + baseUri);
            return result;
        }
        try {
            int n = ids.size();
            int i = 0;

            while (i < n && cursor.moveToNext()) {
                int id = cursor.getInt(0); // _id must be in the first column

                // Match id with the one on the ids list.
                if (ids.get(i) > id) {
                    continue;
                }

                while (ids.get(i) < id) {
                    if (++i >= n) {
                        return result;
                    }
                }

                MediaPath childPath = itemPath.getChild(id);
                Image item = loadOrUpdateItem(childPath, cursor, dataManager,
                        application);
                result[i] = item;
                ++i;
            }
            return result;
        } finally {
            cursor.close();
        }
    }

    public static interface ItemConsumer {
        void consume(int index, Image item);
    }

    // The default implementation uses getMediaItem() for enumerateMediaItems().
    // Subclasses may override this and use more efficient implementations.
    // Returns the number of items enumerated.
    protected int enumerateMediaItems(ItemConsumer consumer, int startIndex) {
        int total = getMediaItemCount();
        int start = 0;
        while (start < total) {
            int count = Math.min(MEDIAITEM_BATCH_FETCH_COUNT, total - start);
            ArrayList<Image> items = getMediaItem(start, count);
            for (int i = 0, n = items.size(); i < n; i++) {
                Image item = items.get(i);
                consumer.consume(startIndex + start + i, item);
            }
            start += count;
        }
        return total;
    }

    public static Cursor getItemCursor(ContentResolver resolver, Uri uri,
            String[] projection, int id) {
        return resolver.query(uri, projection, "_id=?",
                new String[] {
                String.valueOf(id)
                }, null);
    }

    @Override
    public int getMediaItemCount() {
        if (mCachedCount == INVALID_COUNT) {
            Cursor cursor = mResolver.query(
                    mBaseUri, COUNT_PROJECTION, mWhereClause,
                    new String[] {
                    String.valueOf(mBucketId)
                    }, null);
            if (cursor == null) {
                WLog.w(TAG, "query fail");
                return 0;
            }
            try {
                UtilsBase.assertTrue(cursor.moveToNext());
                mCachedCount = cursor.getInt(0);
            } finally {
                cursor.close();
            }
        }
        return mCachedCount;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public long reload() {
        if (mNotifier.isDirty()) {
            mDataVersion = nextVersionNumber();
            mCachedCount = INVALID_COUNT;
        }
        return mDataVersion;
    }

    @Override
    public int getSupportedOperations() {
        return SUPPORT_DELETE | SUPPORT_SHARE | SUPPORT_INFO;
    }

    @Override
    public void delete() {
        UtilsCom.assertNotInRenderThread();
        mResolver.delete(mBaseUri, mWhereClause,
                new String[] {
                String.valueOf(mBucketId)
                });
        mApplication.getDataManager().broadcastLocalDeletion();
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    private static String getLocalizedName(Resources res, int bucketId,
            String name) {
        //        if (bucketId == MediaSetUtils.CAMERA_BUCKET_ID) {
        //            return res.getString(R.string.folder_camera);
        //        } else if (bucketId == MediaSetUtils.DOWNLOAD_BUCKET_ID) {
        //            return res.getString(R.string.folder_download);
        //        } else if (bucketId == MediaSetUtils.IMPORTED_BUCKET_ID) {
        //            return res.getString(R.string.folder_imported);
        //        } else if (bucketId == MediaSetUtils.SNAPSHOT_BUCKET_ID) {
        //            return res.getString(R.string.folder_screenshot);
        //        } else {
        //            return name;
        //        }
        return "";
    }
}
