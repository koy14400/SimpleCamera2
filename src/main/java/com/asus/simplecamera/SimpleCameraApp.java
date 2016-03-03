package com.asus.simplecamera;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.asus.simplecamera2_v3.R;

public class SimpleCameraApp extends Activity {
    public static final String TAG = "SimpleCameraApp";

    //    private Camera mCamera = null;
    private CamBaseV2 mCamBase = null;
    private LinearLayout mRootView = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set full screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main_surfaceview);
        // make screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mRootView = (LinearLayout) findViewById(R.id.root_view);
        mCamBase = new CamBaseV2(this, mRootView);
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "LifeCycle, onResume");
        super.onResume();
        mCamBase.onActivityResume();
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "LifeCycle, onPause");
        super.onPause();
        mCamBase.onActivityPause();
        // return from screen always on state
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}