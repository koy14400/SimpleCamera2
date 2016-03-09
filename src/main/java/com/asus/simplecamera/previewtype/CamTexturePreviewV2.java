package com.asus.simplecamera.previewtype;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;

/**
 * Created by Tinghan_Chang on 2016/3/8.
 */
public class CamTexturePreviewV2 extends CamPreviewV2 {
    public CamTexturePreviewV2(Activity context, CamPreviewV2.PreviewViewType viewType, CamPreviewV2.OnSurfaceReadyListener listener){
        super(context, viewType, listener);
    }

    protected void createSurfaceView() {
        Log.e(TAG, "Use TextureView implement preview. Start");
        TextureView surfaceView = new TextureView(mApp);
        ViewGroup.LayoutParams layoutParams = getPreviewLayoutParams();
        surfaceView.setLayoutParams(layoutParams);
        surfaceView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                SurfaceTexture surfaceTexture = surface;
                Surface previewSurface = new Surface(surfaceTexture);
                onSurfaceReadyListener.onSurfaceReady(previewSurface);
            }

            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
        mPreviewSurfaceView = surfaceView;
        Log.e(TAG, "Use TextureView implement preview. End");
    }

    private TextureView.SurfaceTextureListener mSurfaceextureListener = new TextureView.SurfaceTextureListener() {
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }

        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            SurfaceTexture surfaceTexture = surface;
            Surface previewSurface = new Surface(surfaceTexture);
            onSurfaceReadyListener.onSurfaceReady(previewSurface);
        }
    };
}
