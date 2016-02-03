package com.asus.simplecamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tinghan_Chang on 2016/2/2.
 */
public class CamBaseV2 {

    private static String TAG = "SimpleCameraApp";
    private Activity mApp = null;
    private CameraDevice mCamera = null;
    private CameraManager mCameraManager = null;
    private CameraCharacteristics mCameraCharacteristics = null;
    private CameraCaptureSession mPreviewSession = null;
    private CaptureRequest.Builder mPreviewBuilder = null;
    private String[] mCameraId = null;
    private HandlerThread mCameraThread = null;
    private Handler mCameraHandler = null;
    private Surface mPreviewSurface = null;
    private boolean mIsPreviewing = false;
    private LinearLayout mRootView = null;
    private Size mPreviewSize = null;
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;

    public CamBaseV2(Activity app, LinearLayout rootView) {
        mApp = app;
        mRootView = rootView;
    }

    public void onActivityResume() {
        Log.e(TAG, "LifeCycle, onActivityResume");
        initCameraThread();
        openCamera();
    }

    private void initCameraThread() {
        Log.e(TAG, "init camera thread begin.");
        mCameraThread = new HandlerThread("Camera Handler Thread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        Log.e(TAG, "nit camera thread done");
    }

    public void onActivityPause() {
        Log.e(TAG, "LifeCycle, onActivityPause");
        releaseCamera();
        releaseCameraThread();
        Log.e(TAG, "LifeCycle, onActivityPause done");
    }

    private void releaseCamera() {
        // release camera
        if (mPreviewSession != null) {
            try {
                mPreviewSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mPreviewSession.close();
            mPreviewSession = null;
            mIsPreviewing = false;
        }
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mPreviewSurface != null) {
            mPreviewSurface = null;
        }
    }

    private void releaseCameraThread() {
        if (mCameraThread != null) {
            mCameraThread.interrupt();
            mCameraThread = null;
        }
        if (mCameraHandler != null) {
            mCameraHandler = null;
        }
    }

    private void openCamera() {
        mCameraManager = (CameraManager) mApp.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = mCameraManager.getCameraIdList();
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId[0]);
            mPreviewSize = getPreviewSize();
            mSurfaceView = new SurfaceView(mApp);
            mSurfaceView.setLayoutParams(new ViewGroup.LayoutParams(mPreviewSize.getWidth(), mPreviewSize.getHeight()));
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.addCallback(mSurfaceHolderCallback);
            mRootView.addView(mSurfaceView);
            Log.e(TAG, "camera open begin");
            mCameraManager.openCamera(mCameraId[0], mCameraDeviceStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private Size getPreviewSize(){
        Rect activeArea = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//        int width = activeArea.width();
//        int height = activeArea.height();
        int width = activeArea.height();
        int height = activeArea.width();
        Log.e(TAG, width + " AAAAAAAAAAAAAA " + height);
        Point deviceSize = new Point();
        mApp.getWindowManager().getDefaultDisplay().getSize(deviceSize);
        Log.e(TAG, deviceSize.x + " AAAAAAAAAAAAAA " + deviceSize.y);
        if(deviceSize.x > deviceSize.y){

        } else {
            height = 2392;
            width = 2392 * 3120 / 4208;
        }
        Size previewSize = new Size(width, height);
        return previewSize;
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {

        public void surfaceCreated(SurfaceHolder holder) {
            Log.e(TAG, "surface create done");
            mSurfaceHolder = holder;
            mPreviewSurface = mSurfaceHolder.getSurface();
            startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        }

    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "camera open done");
            mCamera = camera;
            startPreview();
        }

        @Override
        public void onClosed(CameraDevice camera) {
            mCamera = null;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    /**
     * Maybe need to sync Camera and SurfaceView.
     * Maybe need to create SurfaceView after get camera size.
     */
    private void startPreview() {
        Log.e(TAG, "Try start preview.");
        if (mCamera != null && mPreviewSurface != null && !mIsPreviewing) {
            mIsPreviewing = true;
            List<Surface> outputSurfaces = new ArrayList<Surface>(1);
            outputSurfaces.add(mPreviewSurface);
            try {
                Log.e(TAG, "createCaptureSession begin");
                mCamera.createCaptureSession(outputSurfaces, mPreviewSessionCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else if (mPreviewSurface == null) {
            Log.e(TAG, "mPreviewSurface is null");
        } else if (mCamera == null) {
            Log.e(TAG, "mCamera is null");
        } else if (mIsPreviewing) {
            Log.e(TAG, "mIsPreviewing");
        }
    }

    private CameraCaptureSession.StateCallback mPreviewSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.e(TAG, "createCaptureSession done");
            mPreviewSession = session;
            CaptureRequest.Builder previewBuilder = getPreviewBuilder();
            CaptureRequest request = previewBuilder.build();
            try {
                Log.e(TAG, "setRepeatingRequest begin");
                mPreviewSession.setRepeatingRequest(request, null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private CaptureRequest.Builder getPreviewBuilder() {
        try {
            mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(mPreviewSurface);
        return mPreviewBuilder;
    }
}
