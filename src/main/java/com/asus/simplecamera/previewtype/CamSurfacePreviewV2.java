package com.asus.simplecamera.previewtype;

import android.app.Activity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

/**
 * Created by Tinghan_Chang on 2016/3/8.
 */
public class CamSurfacePreviewV2 extends CamPreviewV2 {
    public CamSurfacePreviewV2(Activity context, PreviewViewType viewType, OnSurfaceReadyListener listener){
        super(context, viewType, listener);
    }

    protected void createSurfaceView() {
        Log.e(TAG, "Use SurfaceView implement preview. Start");
        SurfaceView surfaceView = new SurfaceView(mApp);
        ViewGroup.LayoutParams layoutParams = getPreviewLayoutParams();
        surfaceView.setLayoutParams(layoutParams);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                Log.e(TAG, "surface create done");
                SurfaceHolder surfaceHolder = holder;
                Surface surface = surfaceHolder.getSurface();
                onSurfaceReadyListener.onSurfaceReady(surface);
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        mPreviewSurfaceView = surfaceView;
        Log.e(TAG, "Use SurfaceView implement preview. End");
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        public void surfaceCreated(SurfaceHolder holder) {
            Log.e(TAG, "surface create done");
            SurfaceHolder surfaceHolder = holder;
            Surface surface = surfaceHolder.getSurface();
            onSurfaceReadyListener.onSurfaceReady(surface);
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        }

    };

}
