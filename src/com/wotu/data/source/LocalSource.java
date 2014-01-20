
package com.wotu.data.source;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.net.Uri;
import android.provider.MediaStore;

import com.wotu.app.WoTuApp;
import com.wotu.common.WLog;
import com.wotu.data.MediaObject;
import com.wotu.data.MediaSource;
import com.wotu.data.Path;
import com.wotu.data.PathMatcher;
import com.wotu.data.image.LocalImage;
import com.wotu.data.video.LocalVideo;

public class LocalSource extends MediaSource {

    public static final String KEY_BUCKET_ID = "bucketId";

    private WoTuApp mApplication;
    private PathMatcher mMatcher;
    private static final int NO_MATCH = -1;
    private final UriMatcher mUriMatcher = new UriMatcher(NO_MATCH);
    private static final int LOCAL_IMAGE_ALBUMSET = 0;
    private static final int LOCAL_VIDEO_ALBUMSET = 1;
    private static final int LOCAL_IMAGE_ALBUM = 2;
    private static final int LOCAL_VIDEO_ALBUM = 3;
    private static final int LOCAL_IMAGE_ITEM = 4;
    private static final int LOCAL_VIDEO_ITEM = 5;
    private static final int LOCAL_ALL_ALBUMSET = 6;
    private static final int LOCAL_ALL_ALBUM = 7;

    private static final String TAG = "LocalSource";

    private ContentProviderClient mClient;

    public LocalSource(WoTuApp context) {
        super("local");
        mApplication = context;
        mMatcher = new PathMatcher();
        mMatcher.add("/local/image", LOCAL_IMAGE_ALBUMSET);
        mMatcher.add("/local/video", LOCAL_VIDEO_ALBUMSET);
        mMatcher.add("/local/all", LOCAL_ALL_ALBUMSET);

        mMatcher.add("/local/image/*", LOCAL_IMAGE_ALBUM);
        mMatcher.add("/local/video/*", LOCAL_VIDEO_ALBUM);
        mMatcher.add("/local/all/*", LOCAL_ALL_ALBUM);
        mMatcher.add("/local/image/item/*", LOCAL_IMAGE_ITEM);
        mMatcher.add("/local/video/item/*", LOCAL_VIDEO_ITEM);

        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/images/media/#", LOCAL_IMAGE_ITEM);
        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/video/media/#", LOCAL_VIDEO_ITEM);
        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/images/media", LOCAL_IMAGE_ALBUM);
        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/video/media", LOCAL_VIDEO_ALBUM);
        mUriMatcher.addURI(MediaStore.AUTHORITY,
                "external/file", LOCAL_ALL_ALBUM);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        WoTuApp app = mApplication;
        switch (mMatcher.match(path.getPrefix())) {
            case LOCAL_ALL_ALBUMSET:
            case LOCAL_IMAGE_ALBUMSET:
            case LOCAL_VIDEO_ALBUMSET:
                return new LocalAlbumSet(path, mApplication);
            case LOCAL_IMAGE_ALBUM:
                return new LocalAlbum(path, app, path.getIdentity(), true);
            case LOCAL_VIDEO_ALBUM:
                return new LocalAlbum(path, app, path.getIdentity(), false);
            case LOCAL_IMAGE_ITEM:
                return new LocalImage(path, mApplication, path.getIdentity());
            case LOCAL_VIDEO_ITEM:
                return new LocalVideo(path, mApplication, path.getIdentity());
            default:
                throw new RuntimeException("bad path: " + path);
        }
    }

    private static int getMediaType(String type, int defaultType) {
        if (type == null)
            return defaultType;
        try {
            int value = Integer.parseInt(type);
            if ((value & (MediaObject.MEDIA_TYPE_IMAGE
            | MediaObject.MEDIA_TYPE_VIDEO)) != 0)
                return value;
        } catch (NumberFormatException e) {
            WLog.w(TAG, "invalid type: " + type, e);
        }
        return defaultType;
    }

    // The media type bit passed by the intent
    public static final String KEY_MEDIA_TYPES = "mediaTypes";

    private Path getAlbumPath(Uri uri, int defaultType) {
        int mediaType = getMediaType(uri.getQueryParameter(KEY_MEDIA_TYPES), defaultType);
        String bucketId = uri.getQueryParameter(KEY_BUCKET_ID);
        int id = 0;
        try {
            id = Integer.parseInt(bucketId);
        } catch (NumberFormatException e) {
            WLog.w(TAG, "invalid bucket id: " + bucketId, e);
            return null;
        }
        switch (mediaType) {
            case MediaObject.MEDIA_TYPE_IMAGE:
                return new Path("/local/image", id);
            case MediaObject.MEDIA_TYPE_VIDEO:
                return new Path("/local/video", id);
            default:
                return new Path("/local/all", id);
        }
    }

    @Override
    public Path findPathByUri(Uri uri, String type) {
        try {
            switch (mUriMatcher.match(uri)) {
                case LOCAL_IMAGE_ITEM: {
                    long id = ContentUris.parseId(uri);
                    return id >= 0 ? new Path(LocalImage.ITEM_PATH,id) : null;
                }
                case LOCAL_VIDEO_ITEM: {
                    long id = ContentUris.parseId(uri);
                    return id >= 0 ? new Path(LocalVideo.ITEM_PATH,id) : null;
                }
                case LOCAL_IMAGE_ALBUM: {
                    return getAlbumPath(uri, MediaObject.MEDIA_TYPE_IMAGE);
                }
                case LOCAL_VIDEO_ALBUM: {
                    return getAlbumPath(uri, MediaObject.MEDIA_TYPE_VIDEO);
                }
                case LOCAL_ALL_ALBUM: {
                    return getAlbumPath(uri, MediaObject.MEDIA_TYPE_ALL);
                }
            }
        } catch (NumberFormatException e) {
            WLog.w(TAG, "uri: " + uri.toString(), e);
        }
        return null;
    }

    @Override
    public void resume() {
        mClient = mApplication.getContentResolver()
                .acquireContentProviderClient(MediaStore.AUTHORITY);
    }

    @Override
    public void pause() {
        mClient.release();
        mClient = null;
    }
}
