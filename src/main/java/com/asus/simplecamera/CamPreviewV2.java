package com.asus.simplecamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Created by Tinghan_Chang on 2016/3/8.
 */
public class CamPreviewV2 {
    public interface OnSurfaceReadyListener {
        public void onSurfaceReady(Surface surface);
    }
    enum PreviewViewType {
        SurfaceView,
        TextureView,
        GLSurfaceView
    }

    private static final String TAG = SimpleCameraApp.TAG;
    private static final boolean mIsFullDeviceHeight = false;
    private PreviewViewType mPreviewViewType;
    private Size mPreviewSize = null;
    private View mPreviewSurfaceView = null;
    private Activity mApp = null;
//    private PreviewGLSurfaceView.SurfaceTextureListener mSurfaceTextureListener;
    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics;
    private String[] mCameraId;
    private OnSurfaceReadyListener onSurfaceReadyListener;

    public CamPreviewV2(Activity context, PreviewViewType viewType, OnSurfaceReadyListener listener) {
        mPreviewViewType = viewType;
        mApp = context;
        onSurfaceReadyListener = listener;

        mCameraManager = (CameraManager) mApp.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = mCameraManager.getCameraIdList();
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId[0]);
            switch (viewType) {
                case SurfaceView:
                    createSurfaceView();
                    break;
                case TextureView:
                    createTextureView();
                    break;
                case GLSurfaceView:
                    createGLSurfaceView();
                    break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public View getView() {
        return mPreviewSurfaceView;
    }

    private void createGLSurfaceView() {
        Log.e(TAG, "Use GLSurfaceView implement preview. Start");
        ViewGroup.LayoutParams layoutParams = getPreviewLayoutParams();
        mPreviewSize = new Size(layoutParams.width, layoutParams.height);
        PreviewGLSurfaceView previewSurfaceView = new PreviewGLSurfaceView(mApp, mPreviewSize);
        previewSurfaceView.setLayoutParams(layoutParams);
        previewSurfaceView.setSurfaceTextureListener(mSurfaceTextureListener);
        mPreviewSurfaceView = previewSurfaceView;
        Log.e(TAG, "Use GLSurfaceView implement preview. end");
    }

    private void createSurfaceView() {
        Log.e(TAG, "Use SurfaceView implement preview. Start");
        SurfaceView surfaceView = new SurfaceView(mApp);
        ViewGroup.LayoutParams layoutParams = getPreviewLayoutParams();
        surfaceView.setLayoutParams(layoutParams);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(mSurfaceHolderCallback);
        mPreviewSurfaceView = surfaceView;
        Log.e(TAG, "Use SurfaceView implement preview. End");
    }

    private void createTextureView() {
        Log.e(TAG, "Use TextureView implement preview. Start");
        TextureView surfaceView = new TextureView(mApp);
        ViewGroup.LayoutParams layoutParams = getPreviewLayoutParams();
        surfaceView.setLayoutParams(layoutParams);
        surfaceView.setSurfaceTextureListener(mSurfaceextureListener);
        mPreviewSurfaceView = surfaceView;
        Log.e(TAG, "Use TextureView implement preview. End");
    }

    private ViewGroup.LayoutParams getPreviewLayoutParams() {

        Point screenSize = new Point();
        mApp.getWindowManager().getDefaultDisplay().getSize(screenSize);
        Rect activeArea = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int sensorWidth, sensorHeight, previewWidth, previewHeight;
        // Make sensor's orientation same as screen.
        switch (sensorOrientation) {
            case 90:
            case 180:
                sensorWidth = activeArea.height();
                sensorHeight = activeArea.width();
                break;
            case 270:
            case 0:
            default:
                sensorWidth = activeArea.width();
                sensorHeight = activeArea.height();
                break;
        }
        Log.i(TAG, "Sensor Orientation angle:" + sensorOrientation);
        Log.i(TAG, "Sensor Width/Height : " + sensorWidth + "/" + sensorHeight);
        Log.i(TAG, "Screen Width/Height : " + screenSize.x + "/" + screenSize.y);
        // Preview's View size must same as sensor ratio.
        if (mIsFullDeviceHeight) {
            // full device height, maybe 16:9 at phone
            previewWidth = screenSize.y * sensorWidth / sensorHeight;
            previewHeight = screenSize.y;
        } else {
            // full device width, maybe 4:3 at phone
            previewWidth = screenSize.x;
            previewHeight = screenSize.x * sensorHeight / sensorWidth;
        }
        // Set margin to center at screen.
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(previewWidth, previewHeight);
        int widthMargin = (previewWidth - screenSize.x) / 2;
        int heightMargin = (previewHeight - screenSize.y) / 2;
        layoutParams.leftMargin = -widthMargin;
        layoutParams.topMargin = -heightMargin;
        Log.i(TAG, "LayoutMargin left/top : " + -widthMargin + "/" + -heightMargin);
        return layoutParams;
    }

    private PreviewGLSurfaceView.SurfaceTextureListener mSurfaceTextureListener = new PreviewGLSurfaceView.SurfaceTextureListener() {
        public void onSurfaceTextureAvailable(SurfaceTexture surface) {
            SurfaceTexture previewSurfaceTexture = surface;
            Surface previewSurface = new Surface(previewSurfaceTexture);
            onSurfaceReadyListener.onSurfaceReady(previewSurface);
        }
    };

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
