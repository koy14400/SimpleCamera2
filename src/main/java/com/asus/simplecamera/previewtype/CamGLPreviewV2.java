package com.asus.simplecamera.previewtype;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.ViewGroup;

import com.asus.simplecamera.PreviewGLSurfaceView;

/**
 * Created by Tinghan_Chang on 2016/3/8.
 */
public class CamGLPreviewV2 extends CamPreviewV2 {
    public CamGLPreviewV2(Activity context, CamPreviewV2.PreviewViewType viewType, CamPreviewV2.OnSurfaceReadyListener listener){
        super(context, viewType, listener);
    }

    @Override
    protected void createSurfaceView() {
        Log.e(TAG, "Use GLSurfaceView implement preview. Start");
        ViewGroup.LayoutParams layoutParams = getPreviewLayoutParams();
        mPreviewSize = new Size(layoutParams.width, layoutParams.height);
        PreviewGLSurfaceView previewSurfaceView = new PreviewGLSurfaceView(mApp, mPreviewSize);
        previewSurfaceView.setLayoutParams(layoutParams);
        previewSurfaceView.setSurfaceTextureListener(new PreviewGLSurfaceView.SurfaceTextureListener() {
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture) {
                SurfaceTexture previewSurfaceTexture = surfaceTexture;
                Surface previewSurface = new Surface(previewSurfaceTexture);
                onSurfaceReadyListener.onSurfaceReady(previewSurface);
            }
        });
        mPreviewSurfaceView = previewSurfaceView;
        Log.e(TAG, "Use GLSurfaceView implement preview. end");
    }

    protected PreviewGLSurfaceView.SurfaceTextureListener mSurfaceTextureListener = new PreviewGLSurfaceView.SurfaceTextureListener() {
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture) {
            SurfaceTexture previewSurfaceTexture = surfaceTexture;
            Surface previewSurface = new Surface(previewSurfaceTexture);
            onSurfaceReadyListener.onSurfaceReady(previewSurface);
        }
    };
}
