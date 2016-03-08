package com.asus.simplecamera;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.asus.simplecamera2_v3.R;

public class SimpleCameraApp extends Activity {
    public static final String TAG = "SimpleCameraApp";
    public CamPreviewV2.PreviewViewType mViewType = CamPreviewV2.PreviewViewType.GLSurfaceView;

    //    private Camera mCamera = null;
    private CamBaseV2 mCamBase = null;
    private CamPreviewV2 mCamPreview = null;
    private LinearLayout mRootView = null;
    private Button mCaptureButton = null;

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
        mCaptureButton = (Button) findViewById(R.id.button);
        mCaptureButton.setOnClickListener(mCaptureButtonClickListener);
        mCamBase = new CamBaseV2(this, mRootView);
        mCamPreview = new CamPreviewV2(this, mViewType, mOnSurfaceReadyListener);
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "LifeCycle, onResume");
        super.onResume();
        mCamBase.onActivityResume();
        mRootView.addView(mCamPreview.getView());
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "LifeCycle, onPause");
        super.onPause();
        mCamBase.onActivityPause();
        mRootView.removeAllViews();
        // return from screen always on state
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    View.OnClickListener mCaptureButtonClickListener = new View.OnClickListener() {
        public void onClick(View v) {
//            mCamBase.takePicture();
            changeNextPreviewViewType();
        }
    };

    private CamPreviewV2.OnSurfaceReadyListener mOnSurfaceReadyListener = new CamPreviewV2.OnSurfaceReadyListener() {
        public void onSurfaceReady(Surface surface) {
            mCamBase.startPreview(surface);
        }
    };
    public void changeNextPreviewViewType(){
        int nextTypeOrdinal = mViewType.ordinal() + 1;
        if(nextTypeOrdinal >= CamPreviewV2.PreviewViewType.values().length)
            nextTypeOrdinal = 0;
        mViewType = CamPreviewV2.PreviewViewType.values()[nextTypeOrdinal];
        mCamBase.onActivityPause();
        mRootView.removeAllViews();
        mCamBase.onActivityResume();
        mCamPreview = new CamPreviewV2(this, mViewType, mOnSurfaceReadyListener);
        mRootView.addView(mCamPreview.getView());
    }
}