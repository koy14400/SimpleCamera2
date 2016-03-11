package com.asus.simplecamera.cambase2;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.asus.simplecamera.ImageSaver;
import com.asus.simplecamera.SimpleCameraApp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Tinghan_Chang on 2016/2/2.
 */
public class CamBaseV2 {

    private static String TAG = SimpleCameraApp.TAG;
    private static final int BACK_CAMERA_ID = 0;
    private Activity mApp = null;
    private CameraDevice mCamera = null;
    private CameraManager mCameraManager = null;
    private CameraCharacteristics mCameraCharacteristics = null;
    private HandlerThread mCameraThread = null;
    private Handler mCameraHandler = null;
    private CamSessionV2 mCamSession = null;
    private HandlerThread mImageSaverThread = null;
    private Handler mImageSaverHandler = null;

    public CamBaseV2(Activity app) {
        mApp = app;
    }

    /**
     * Initial thread.
     * Open Camera.
     */
    public void onActivityResume() {
        Log.e(TAG, "LifeCycle, onActivityResume");
        initCameraThread();
        openCamera();
    }

    /**
     * Release Camera.
     * Release Thread.
     */
    public void onActivityPause() {
        Log.e(TAG, "LifeCycle, onActivityPause");
        releaseCamera();
        releaseCameraThread();
        Log.e(TAG, "LifeCycle, onActivityPause done");
    }

    /**
     * Try startPreview with previewSurface.
     * Only success when previewSurface and camera not null.
     * So call this function by onCameraOpened and onSurfaceReady.
     * Init ImageReader by createPostProcess().
     * @param previewSurface
     */
    public void startPreview(Surface previewSurface) {
        Log.e(TAG, "Try start preview.");
        mCamSession.startPreview(previewSurface, createPostProcess());
    }

    /**
     * Try take picture by createPostProcess().
     * Maybe fail at ImageReader not enough. Depend on postProcess's mMaxRequestNumber.
     */
    public void takePicture() {
        mCamSession.takePicture(createPostProcess());
    }

    private void initCameraThread() {
        Log.e(TAG, "Init camera thread begin.");
        mCameraThread = new HandlerThread("Camera Handler Thread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());
        Log.e(TAG, "Init camera thread done");
        Log.e(TAG, "Init ImageSaver thread begin.");
        mImageSaverThread = new HandlerThread("ImageSaver Handler Thread");
        mImageSaverThread.start();
        mImageSaverHandler = new Handler(mCameraThread.getLooper());
        Log.e(TAG, "Init ImageSaver thread done");
    }

    private void releaseCamera() {
        // release camera
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
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
        if (mImageSaverThread != null) {
            mImageSaverThread.interrupt();
            mImageSaverThread = null;
        }
        if (mImageSaverHandler != null) {
            mImageSaverHandler = null;
        }
    }

    private void openCamera() {
        String[] mCameraId;
        mCameraManager = (CameraManager) mApp.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = mCameraManager.getCameraIdList();
            String targetID = mCameraId[BACK_CAMERA_ID];
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(targetID);

            Log.e(TAG, "camera open begin");
            mCameraManager.openCamera(targetID, mCameraDeviceStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "CameraDevice ID:" + camera.getId() + " is onOpened.");
            mCamera = camera;
            mCamSession = new CamSessionV2(mCamera, mCameraHandler, mCameraCharacteristics);
            startPreview(null);
        }

        @Override
        public void onClosed(CameraDevice camera) {
            mCamSession.release();
            mCamSession = null;
            mCamera = null;
            Log.e(TAG, "CameraDevice ID:" + camera.getId() + " is onClosed.");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e(TAG, "CameraDevice ID:" + camera.getId() + " is onDisconnected.");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice ID:" + camera.getId() + " is onError.");
        }
    };

    /**
     * Override by child class to choose PostProcess.
     * @return
     */
    protected PostProcess createPostProcess() {
        PostProcess postProcess = new SingleCaptureNormalProcess(mTakePictureCallback, mImageSaverHandler);
        return postProcess;
    }

    private PostProcess.TakePictureCallback mTakePictureCallback = new PostProcess.TakePictureCallback() {
        public void onImageReady(Image image) {
            File outputPath = getOutputMediaFile(image.getFormat());
            ImageSaver imageSaver = new ImageSaver(image, outputPath, null, mCameraCharacteristics, mApp.getBaseContext());
            mImageSaverHandler.post(imageSaver);
            mCamSession.finishOneRequest();
        }
    };

    private File getOutputMediaFile(int pictureFormat) {

        File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "SimpleCamera2");
        path.mkdir();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String photoPath;

        switch (pictureFormat) {
            case ImageFormat.JPEG:
                photoPath = path.getPath() + File.separator + "IMG_" + timeStamp + ".jpg";
                break;
            case ImageFormat.RAW_SENSOR:
                photoPath = path.getPath() + File.separator + "RAW_" + timeStamp + ".dng";
                break;
            default:
                photoPath = path.getPath() + File.separator + "IMG_" + timeStamp + ".jpg";
                break;
        }

        Log.i(TAG, photoPath);
        File photo = new File(photoPath);

        return photo;
    }


}