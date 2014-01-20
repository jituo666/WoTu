package com.wotu.activity;

import com.wotu.view.GLRootView;
import com.wotu.view.OrientationManager;
import com.wotu.view.TransitionStore;

import android.app.Activity;
import android.os.Bundle;

public class WoTuActivity extends Activity {
    private GLRootView mGLRootView;
    private TransitionStore mTransitionStore = new TransitionStore();
    private OrientationManager mOrientationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mOrientationManager = new OrientationManager(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

}
