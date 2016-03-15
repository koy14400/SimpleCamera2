package com.asus.simplecamera.cambase2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.asus.simplecamera.SimpleCameraApp;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Tinghan_Chang on 2016/3/9.
 */
public class CamSessionV2 {
    public interface TakePictureCallback {
        // Should be jpeg callback
        public void onImageReady(Image image);
    }

    private static String TAG = SimpleCameraApp.TAG;

    // Constructor
    private CameraDevice mCamera = null;
    private Handler mCameraHandler = null;
    private Handler mImageAvailableHandler = null;
    private TakePictureCallback mTakePictureCallback = null;
    private CameraCharacteristics mCameraCharacteristics = null;


    private CameraCaptureSession mSession = null;
    private Surface mPreviewSurface = null;
    private ImageReader mSessionImageReader = null;
    private int mMaxRequestNumber = 1;
    private boolean mIsPreviewing = false;
    private boolean mIsNeedTakePicture = false;

    private PostProcess mCurrentPostProcess;
    private Queue<PostProcess> mQueue;

    public CamSessionV2(CameraDevice camera, Handler cameraHandler, Handler imageHandler, CameraCharacteristics cameraCharacteristics, TakePictureCallback callback) {
        mCamera = camera;
        mCameraHandler = cameraHandler;
        mImageAvailableHandler = imageHandler;
        mTakePictureCallback = callback;
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
        Log.e(TAG, "Session, Try start preview.");
        if (previewSurface != null) {
            mPreviewSurface = previewSurface;
        }
        if (mCamera != null && mPreviewSurface != null) {
            List<Surface> outputSurfaces = postProcess.createOutputSurfaceList(mPreviewSurface, mCameraCharacteristics);
            mSessionImageReader = postProcess.getPictureImageReader();
            mSessionImageReader.setOnImageAvailableListener(new CustomImageAvailableListener(), mImageAvailableHandler);
            mMaxRequestNumber = postProcess.getMaxRequestNumber();
            mQueue = new ArrayBlockingQueue<PostProcess>(mMaxRequestNumber);
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
     * Tell session we can accept a new take picture request from CamBaseV2.
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
        List<CaptureRequest> list = postProcess.getCaptureBuilder(mCamera, mPreviewSurface, mSessionImageReader);
        try {
            if (list != null && list.size() > 1) {
                mSession.captureBurst(list, mCaptureCallback, mCameraHandler);
                mQueue.offer(postProcess);
                Log.e(TAG, "takePicture, use captureBurst. CaptureRequest Number:" + list.size());
            } else if (list != null && list.size() == 1) {
                mSession.capture(list.get(0), mCaptureCallback, mCameraHandler);
                mQueue.offer(postProcess);
                Log.e(TAG, "takePicture, use capture.");
            } else {
                Log.e(TAG, "takePicture, fail at builder is null.");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "takePicture, fail at capture.");
        }

    }

    /**
     * Try create a new session.
     * If current session can't use, we must create a new session before take picture.
     *
     * @param postProcess
     */
    private void useNewSessionTakePicture(PostProcess postProcess) {
        // Some bug, preview will rotate 90 angle at old device(legacy).
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
        Log.e(TAG, "Rest of Request count:" + mMaxRequestNumber);
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
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    protected class CustomImageAvailableListener implements ImageReader.OnImageAvailableListener {

        private int imageCount = 0;

        public CustomImageAvailableListener() {
        }

        public void onImageAvailable(ImageReader reader) {
            if (mCurrentPostProcess == null) {
                mCurrentPostProcess = mQueue.poll();
            }
            if (mCurrentPostProcess != null) {
                Log.d(TAG, "Capture, onImageAvailable.");
                if (!isGetEnoughImage()) return;
                // TODO: shutter callback
                Log.d(TAG, "Capture, onImageAvailable. Get total Images:" + imageCount);
                Image[] imageList = getImages(reader);
                // Do post process. Should close unused image.
                Image resultImage = mCurrentPostProcess.postProcess(imageList);
                // TODO: JpegCallback
                if (mTakePictureCallback != null) {
                    mTakePictureCallback.onImageReady(resultImage);
                }
                if (isBurstDone())
                    resetState();
            } else {
                Log.w(TAG, "Capture, onImageAvailable. CurrentPostProcess is null.");
            }
        }

        private boolean isBurstDone() {
            return (imageCount / mCurrentPostProcess.mCaptureCount) >= mCurrentPostProcess.getBurstCount();
        }

        private void resetState() {
            finishOneRequest();
            imageCount = 0;
            mCurrentPostProcess = null;
        }

        private Image[] getImages(ImageReader reader) {
            Image[] imageList = new Image[imageCount];
            for (int i = 0; i < imageCount; i++) {
                imageList[i] = reader.acquireNextImage();
            }
            return imageList;
        }

        private boolean isGetEnoughImage() {
            imageCount++;
            if (imageCount < mCurrentPostProcess.mCaptureCount) {
                Log.d(TAG, "Capture, onImageAvailable. Not enough Images. Current/Total:" + imageCount + "/" + mCurrentPostProcess.mCaptureCount);
                return false;
            }
            return true;
        }
    }
}
