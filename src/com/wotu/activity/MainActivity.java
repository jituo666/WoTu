
package com.wotu.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.Window;
import android.widget.Toast;

import com.wotu.R;
import com.wotu.common.WLog;
import com.wotu.data.DataManager;
import com.wotu.page.AlbumPage;
import com.wotu.utils.UtilsBase;

public class MainActivity extends WoTuActivity {

    private static final String TAG = "MainActivity";
    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    public static final String EXTRA_SLIDESHOW = "slideshow";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            WLog.i("TAG", "savedInstanceState");
            getPageManager().restoreFromPage(savedInstanceState);
        } else {
            WLog.i("TAG", "!savedInstanceState");
            initializeByIntent();
        }
    }

    private void initializeByIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_GET_CONTENT.equalsIgnoreCase(action)) {
            startGetContent(intent);
        } else if (Intent.ACTION_PICK.equalsIgnoreCase(action)) {
            // We do NOT really support the PICK intent. Handle it as
            // the GET_CONTENT. However, we need to translate the type
            // in the intent here.
            WLog.w(TAG, "action PICK is not supported");
            String type = UtilsBase.ensureNotNull(intent.getType());
            if (type.startsWith("vnd.android.cursor.dir/")) {
                if (type.endsWith("/image"))
                    intent.setType("image/*");
                if (type.endsWith("/video"))
                    intent.setType("video/*");
            }
            startGetContent(intent);
        } else if (Intent.ACTION_VIEW.equalsIgnoreCase(action)
                || ACTION_REVIEW.equalsIgnoreCase(action)) {
            startViewAction(intent);
        } else {
            startDefaultPage();
        }
    }

    public void startDefaultPage() {
        WLog.i("TAG", "startDefaultPage");
        Bundle data = new Bundle();
        data.putString(DataManager.KEY_MEDIA_PATH, getDataManager().getTopSetPath(DataManager.INCLUDE_IMAGE));
        getPageManager().startPage(AlbumPage.class, data);
    }

    private void startGetContent(Intent intent) {
        Bundle data = intent.getExtras() != null
                ? new Bundle(intent.getExtras())
                : new Bundle();
    }

    private String getContentType(Intent intent) {
        String type = intent.getType();
        if (type != null)
            return type;

        Uri uri = intent.getData();
        try {
            return getContentResolver().getType(uri);
        } catch (Throwable t) {
            WLog.w(TAG, "get type fail", t);
            return null;
        }
    }

    private void startViewAction(Intent intent) {
        Boolean slideshow = intent.getBooleanExtra(EXTRA_SLIDESHOW, false);
        if (slideshow) {

        } else {
            Bundle data = new Bundle();
            DataManager dm = getDataManager();
            Uri uri = intent.getData();
            String contentType = getContentType(intent);
            if (contentType == null) {
                Toast.makeText(this,
                        R.string.no_such_item, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

}
