package com.asus.simplecamera.cambase2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.asus.simplecamera.SimpleCameraApp;

import java.util.List;

/**
 * Created by Tinghan_Chang on 2016/3/9.
 */
public class CamSessionV2 {
    private static String TAG = SimpleCameraApp.TAG;

    // Constructor
    private CameraDevice mCamera = null;
    private Handler mCameraHandler = null;
    private CameraCharacteristics mCameraCharacteristics = null;


    private CameraCaptureSession mSession = null;
    private Surface mPreviewSurface = null;
    private ImageReader mSessionImageReader = null;
    private int mMaxRequestNumber = 1;
    private boolean mIsPreviewing = false;
    private boolean mIsNeedTakePicture = false;

    public CamSessionV2(CameraDevice camera, Handler handler, CameraCharacteristics cameraCharacteristics) {
        mCamera = camera;
        mCameraHandler = handler;
        mCameraCharacteristics = cameraCharacteristics;
    }

    /**
     * Release all resource. Not use again.
     */
    public void release() {
        if (mSession != null) {
            mSession.close();
        }
        mPreviewSurface = null;
        mIsPreviewing = false;
        mCamera = null;
        mCameraHandler = null;
        mCameraCharacteristics = null;
    }

    /**
     * Maybe need to sync Camera and SurfaceView.
     * Maybe need to create SurfaceView after get camera size.
     */
    public void startPreview(Surface previewSurface, PostProcess postProcess) {
        Log.e(TAG, "Try start preview.");
        if (previewSurface != null) {
            mPreviewSurface = previewSurface;
        }
        if (mCamera != null && mPreviewSurface != null) {
            List<Surface> outputSurfaces = postProcess.createOutputSurfaceList(mPreviewSurface, mCameraCharacteristics);
            mSessionImageReader = postProcess.getPictureImageReader();
            mMaxRequestNumber = postProcess.getMaxRequestNumber();
            try {
                Log.e(TAG, "createCaptureSession begin");
                mCamera.createCaptureSession(outputSurfaces, new CustomCaptureSessionCallback(postProcess), mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else if (mPreviewSurface == null) {
            Log.e(TAG, "mPreviewSurface is null");
        } else if (mCamera == null) {
            Log.e(TAG, "mCamera is null");
        }
    }

    /**
     * Try take picture.
     * If postProcess not match current session, take picture after create new session.
     * Maybe fail at ImageReader not enough. Depend on postProcess's mMaxRequestNumber.
     *
     * @param postProcess
     */
    public void takePicture(PostProcess postProcess) {
        if (mCamera == null) return;
        if (postProcess.isImageReaderMatch(mSessionImageReader)) {
            useCurrentSessionTakePicture(postProcess);
        } else {
            useNewSessionTakePicture(postProcess);
        }
    }

    /**
     * Tell session we can accept a new request from CamBaseV2.
     */
    public void finishOneRequest() {
        mMaxRequestNumber++;
    }

    private class CustomCaptureSessionCallback extends CameraCaptureSession.StateCallback {
        PostProcess postProcess;

        public CustomCaptureSessionCallback(PostProcess Process) {
            postProcess = Process;
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.i(TAG, "mSessionCallback, onConfigured done");
            mSession = session;
            useCurrentSessionStartPreview(postProcess);
            if (mIsNeedTakePicture) {
                mIsNeedTakePicture = false;
                useCurrentSessionTakePicture(postProcess);
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e(TAG, "mSessionCallback, onConfigureFailed done");
        }

        @Override
        public void onClosed(CameraCaptureSession session) {
            super.onClosed(session);
            mSession = null;
            Log.i(TAG, "mSessionCallback, onClosed done");
        }
    }


    private void useCurrentSessionStartPreview(PostProcess postProcess) {
        CaptureRequest.Builder previewBuilder = postProcess.getPreviewBuilder(mCamera);
        try {
            Log.e(TAG, "setRepeatingRequest begin");
            mSession.setRepeatingRequest(previewBuilder.build(), null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void useCurrentSessionTakePicture(PostProcess postProcess) {
        if (!isEnoughRequest()) return;
        CaptureRequest.Builder builder = postProcess.getCaptureBuilder(mCamera, mPreviewSurface, mSessionImageReader);
        builder.setTag(postProcess);
        if (builder != null) {
            try {
                mSession.capture(builder.build(), mCaptureCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(TAG, "takePicture, fail at capture.");
            }
        } else {
            Log.e(TAG, "takePicture, fail at builder is null.");
        }
    }

    /**
     * Try create a new session.
     * If current session can't use, we must create a new session before take picture.
     * @param postProcess
     */
    private void useNewSessionTakePicture(PostProcess postProcess) {
        // Some bug, preview will rotate 90 angle.
        releaseOldSession();
        mIsNeedTakePicture = true;
        startPreview(mPreviewSurface, postProcess);
    }

    private boolean isEnoughRequest() {
        if (mMaxRequestNumber <= 0) {
            Log.e(TAG, "No request can use.");
            return false;
        }
        mMaxRequestNumber--;
        return true;
    }

    private void releaseOldSession() {
        if (mSession != null) {
            try {
                mSession.abortCaptures();
                mSessionImageReader.close();
                mSessionImageReader = null;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            //Maybe can postProcess at here.
//            PostProcess p = (PostProcess)request.getTag();
//            Image image = null;
//            while(image == null) {
//                image = mSessionImageReader.acquireNextImage();
//            }
//            p.saveImage(image);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };
}
