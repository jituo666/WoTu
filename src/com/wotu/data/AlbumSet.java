package com.wotu.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Files.FileColumns;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video;

import com.wotu.app.WoTuApp;
import com.wotu.common.Future;
import com.wotu.common.FutureListener;
import com.wotu.common.ThreadPool;
import com.wotu.common.WLog;
import com.wotu.common.ThreadPool.JobContext;
import com.wotu.util.UtilsBase;
import com.wotu.util.UtilsMedia;
import com.wotu.R;

import java.util.ArrayList;
import java.util.Comparator;

public class AlbumSet extends MediaSet implements
        FutureListener<ArrayList<MediaSet>> {
    public static final MediaPath PATH_ALL = new MediaPath("/local/all");
    public static final MediaPath PATH_IMAGE = new MediaPath("/local/image");
    public static final MediaPath PATH_VIDEO = new MediaPath("/local/video");

    private static final String TAG = "LocalAlbumSet";
    private static final String EXTERNAL_MEDIA = "external";

    // The indices should match the following projections.
    private static final int INDEX_BUCKET_ID = 0;
    private static final int INDEX_MEDIA_TYPE = 1;
    private static final int INDEX_BUCKET_NAME = 2;

    private static final Uri mBaseUri = Files.getContentUri(EXTERNAL_MEDIA);
    private static final Uri mWatchUriImage = Images.Media.EXTERNAL_CONTENT_URI;
    private static final Uri mWatchUriVideo = Video.Media.EXTERNAL_CONTENT_URI;

    // BUCKET_DISPLAY_NAME is a string like "Camera" which is the directory
    // name of where an image or video is in. BUCKET_ID is a hash of the path
    // name of that directory (see computeBucketValues() in MediaProvider for
    // details). MEDIA_TYPE is video, image, audio, etc.
    //
    // The "albums" are not explicitly recorded in the database, but each image
    // or video has the two columns (BUCKET_ID, MEDIA_TYPE). We define an
    // "album" to be the collection of images/videos which have the same value
    // for the two columns.
    //
    // The goal of the query (used in loadSubMediaSets()) is to find all albums,
    // that is, all unique values for (BUCKET_ID, MEDIA_TYPE). In the meantime
    // sort them by the timestamp of the latest image/video in each of the
    // album.
    //
    // The order of columns below is important: it must match to the index in
    // MediaStore.
    private static final String[] PROJECTION_BUCKET = {
            ImageColumns.BUCKET_ID,
            FileColumns.MEDIA_TYPE, ImageColumns.BUCKET_DISPLAY_NAME
    };

    // We want to order the albums by reverse chronological order. We abuse the
    // "WHERE" parameter to insert a "GROUP BY" clause into the SQL statement.
    // The template for "WHERE" parameter is like:
    // SELECT ... FROM ... WHERE (%s)
    // and we make it look like:
    // SELECT ... FROM ... WHERE (1) GROUP BY 1,(2)
    // The "(1)" means true. The "1,(2)" means the first two columns specified
    // after SELECT. Note that because there is a ")" in the template, we use
    // "(2" to match it.
    private static final String BUCKET_GROUP_BY = "1) GROUP BY 1,(2";
    private static final String BUCKET_ORDER_BY = "MAX(datetaken) DESC";

    private final WoTuApp mApp;
    private ArrayList<MediaSet> mAlbums = new ArrayList<MediaSet>();
    private final DataNotifier mNotifierImage;
    private final DataNotifier mNotifierVideo;
    private final String mName;
    private final Handler mHandler;
    private boolean mIsLoading;

    private Future<ArrayList<MediaSet>> mLoadTask;
    private ArrayList<MediaSet> mLoadBuffer;

    public AlbumSet(MediaPath path, WoTuApp application) {
        super(path, nextVersionNumber());
        mApp = application;
        mHandler = new Handler(application.getMainLooper());
        mNotifierImage = new DataNotifier(this, mWatchUriImage, application);
        mNotifierVideo = new DataNotifier(this, mWatchUriVideo, application);
        mName = application.getResources().getString(
                R.string.set_label_albums);
    }

    @Override
    public MediaSet getSubMediaSet(int index) {
        return mAlbums.get(index);
    }

    @Override
    public int getSubMediaSetCount() {
        return mAlbums.size();
    }

    @Override
    public String getName() {
        return mName;
    }

    private BucketEntry[] loadBucketEntries(JobContext jc) {
        Uri uri = mBaseUri;

        WLog.v("DebugLoadingTime", "start quering media provider");
        Cursor cursor = mApp.getContentResolver().query(uri,
                PROJECTION_BUCKET, BUCKET_GROUP_BY, null, BUCKET_ORDER_BY);
        if (cursor == null) {
            WLog.w(TAG, "cannot open local database: " + uri);
            return new BucketEntry[0];
        }
        ArrayList<BucketEntry> buffer = new ArrayList<BucketEntry>();

        try {
            while (cursor.moveToNext()) {
                    BucketEntry entry = new BucketEntry(
                            cursor.getInt(INDEX_BUCKET_ID),
                            cursor.getString(INDEX_BUCKET_NAME));
                    if (!buffer.contains(entry)) {
                        buffer.add(entry);
                    }
                if (jc.isCancelled())
                    return null;
            }
            WLog.v("DebugLoadingTime", "got " + buffer.size() + " buckets");
        } finally {
            cursor.close();
        }
        return buffer.toArray(new BucketEntry[buffer.size()]);
    }

    private static int findBucket(BucketEntry entries[], int bucketId) {
        for (int i = 0, n = entries.length; i < n; ++i) {
            if (entries[i].bucketId == bucketId)
                return i;
        }
        return -1;
    }

    private class AlbumsLoader implements ThreadPool.Job<ArrayList<MediaSet>> {

        @Override
        @SuppressWarnings("unchecked")
        public ArrayList<MediaSet> run(JobContext jc) {
            // Note: it will be faster if we only select media_type and
            // bucket_id.
            // need to test the performance if that is worth
            BucketEntry[] entries = loadBucketEntries(jc);

            if (jc.isCancelled())
                return null;

            int offset = 0;
            // Move camera and download bucket to the front, while keeping the
            // order of others.
            int index = findBucket(entries, UtilsMedia.CAMERA_BUCKET_ID);
            if (index != -1) {
                circularShiftRight(entries, offset++, index);
            }
            index = findBucket(entries, UtilsMedia.DOWNLOAD_BUCKET_ID);
            if (index != -1) {
                circularShiftRight(entries, offset++, index);
            }

            ArrayList<MediaSet> albums = new ArrayList<MediaSet>();
            DataManager dataManager = mApp.getDataManager();
            for (BucketEntry entry : entries) {
                MediaSet album = getLocalAlbum(dataManager, mType, mPath,
                        entry.bucketId, entry.bucketName);
                albums.add(album);
            }
            return albums;
        }
    }

    private MediaSet getLocalAlbum(DataManager manager, int type, MediaPath parent,
            int id, String name) {
        synchronized (DataManager.LOCK) {
            MediaPath path = parent.getChild(id);
            MediaObject object = manager.peekMediaObject(path);
            if (object != null)
                return (MediaSet) object;
            switch (type) {
                case MEDIA_TYPE_IMAGE:
                    return new Album(path, mApp, id, true, name);
                case MEDIA_TYPE_VIDEO:
                    return new Album(path, mApp, id, false, name);
                case MEDIA_TYPE_ALL:
                    Comparator<MediaItem> comp = DataManager.sDateTakenComparator;
                    return new LocalMergeAlbum(path, comp, new MediaSet[] {
                            getLocalAlbum(manager, MEDIA_TYPE_IMAGE, PATH_IMAGE,
                                    id, name),
                            getLocalAlbum(manager, MEDIA_TYPE_VIDEO, PATH_VIDEO,
                                    id, name)
                    }, id);
            }
            throw new IllegalArgumentException(String.valueOf(type));
        }
    }

    public static String getBucketName(ContentResolver resolver, int bucketId) {
        Uri uri = mBaseUri.buildUpon().appendQueryParameter("limit", "1")
                .build();

        Cursor cursor = resolver.query(uri, PROJECTION_BUCKET, "bucket_id = ?",
                new String[] {
                    String.valueOf(bucketId)
                }, null);

        if (cursor == null) {
            WLog.w(TAG, "query fail: " + uri);
            return "";
        }
        try {
            return cursor.moveToNext() ? cursor.getString(INDEX_BUCKET_NAME)
                    : "";
        } finally {
            cursor.close();
        }
    }

    @Override
    public synchronized boolean isLoading() {
        return mIsLoading;
    }

    public static synchronized long nextVersionNumber() {
        return ++MediaObject.sVersionSerial;
    }

    @Override
    // synchronized on this function for
    // 1. Prevent calling reload() concurrently.
    // 2. Prevent calling onFutureDone() and reload() concurrently
    public synchronized long reload() {
        // "|" is used instead of "||" because we want to clear both flags.
        if (mNotifierImage.isDirty() | mNotifierVideo.isDirty()) {
            if (mLoadTask != null)
                mLoadTask.cancel();
            mIsLoading = true;
            mLoadTask = mApp.getThreadPool().submit(new AlbumsLoader(),
                    this);
        }
        if (mLoadBuffer != null) {
            mAlbums = mLoadBuffer;
            mLoadBuffer = null;
            for (MediaSet album : mAlbums) {
                album.reload();
            }
            WLog.i("test-r", "enter reload()-2:" + mDataVersion);
            mDataVersion = nextVersionNumber();
        }
        return mDataVersion;
    }

    @Override
    public synchronized void onFutureDone(Future<ArrayList<MediaSet>> future) {
        if (mLoadTask != future)
            return; // ignore, wait for the latest task
        mLoadBuffer = future.get();
        mIsLoading = false;
        if (mLoadBuffer == null)
            mLoadBuffer = new ArrayList<MediaSet>();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyContentChanged();
            }
        });
    }

    // For debug only. Fake there is a ContentObserver.onChange() event.
    void fakeChange() {
        mNotifierImage.fakeChange();
        mNotifierVideo.fakeChange();
    }

    private static class BucketEntry {
        public String bucketName;
        public int bucketId;

        public BucketEntry(int id, String name) {
            bucketId = id;
            bucketName = UtilsBase.ensureNotNull(name);
        }

        @Override
        public int hashCode() {
            return bucketId;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof BucketEntry))
                return false;
            BucketEntry entry = (BucketEntry) object;
            return bucketId == entry.bucketId;
        }
    }

    // Circular shift the array range from a[i] to a[j] (inclusive). That is,
    // a[i] -> a[i+1] -> a[i+2] -> ... -> a[j], and a[j] -> a[i]
    private static <T> void circularShiftRight(T[] array, int i, int j) {
        T temp = array[j];
        for (int k = j; k > i; k--) {
            array[k] = array[k - 1];
        }
        array[i] = temp;
    }
}
