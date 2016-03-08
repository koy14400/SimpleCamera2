package com.asus.simplecamera;

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
    private ImageReader mPictureImageReader;
    private final static int PICTURE_FORMAT = ImageFormat.JPEG;

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
        releaseSurfaceView();
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

    private void releaseSurfaceView() {
        if (mPreviewSurface != null) {
            mPreviewSurface = null;
        }
    }

    private void openCamera() {
        mCameraManager = (CameraManager) mApp.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = mCameraManager.getCameraIdList();
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId[0]);

            // Because camera2.0 only can control view size.
            // So we need to dynamic create view to fit sensor size.
//            createSurfaceView(mRootView);
            Log.e(TAG, "camera open begin");
            mCameraManager.openCamera(mCameraId[0], mCameraDeviceStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "camera open done");
            mCamera = camera;
            startPreview(null);
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
    public void startPreview(Surface previewSurface) {
        Log.e(TAG, "Try start preview.");
        if (previewSurface!= null) {
            mPreviewSurface = previewSurface;
        }
        if (mCamera != null && mPreviewSurface != null && !mIsPreviewing) {
            mIsPreviewing = true;
            List<Surface> outputSurfaces = new ArrayList<Surface>(1);
            outputSurfaces.add(mPreviewSurface);
            outputSurfaces = setCaptureImageReader(outputSurfaces);
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

    private List<Surface> setCaptureImageReader(List<Surface> outputSurface) {
        int picWidth = 0, picHeigh = 0;
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            int[] colorFormat = map.getOutputFormats();
            for (int format : colorFormat) {
                Log.d(TAG, "Camera 0" + ": Supports color format " + formatToText(format));
                android.util.Size[] mColorSizes = map.getOutputSizes(format);
                for (android.util.Size s : mColorSizes)
                    Log.d(TAG, "Camera 0" + ": color size W/H:" + s.getWidth() + "/" + s.getHeight());
                if (format == PICTURE_FORMAT) {
                    Size size = mColorSizes[0];
                    picWidth = size.getWidth();
                    picHeigh = size.getHeight();
                }
            }
        }
        Log.d(TAG, "Camera 0, format:" + PICTURE_FORMAT + ": picture size W/H :" + picWidth + "/" + picHeigh);
        if (picWidth <= 0 || picHeigh <= 0) {
            Log.e(TAG, "Camera 0" + ": picture size have some problem, need check!!!");
            return outputSurface;
        }
        mPictureImageReader = ImageReader.newInstance(picWidth, picHeigh, PICTURE_FORMAT, 2);
        mPictureImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);
        outputSurface.add(mPictureImageReader.getSurface());
        return outputSurface;
    }

    public static String formatToText(int format) {
        switch (format) {
            case ImageFormat.YUY2:
                return "YUY2";
            case ImageFormat.JPEG:
                return "JPEG";
            case ImageFormat.NV21:
                return "NV21";
            case ImageFormat.YV12:
                return "YV12";
            case PixelFormat.RGBA_8888:
                return "RGBA_8888";
        }
        return "<unknown format>: " + Integer.toHexString(format);
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        public void onImageAvailable(ImageReader reader) {
            Log.i(TAG, "takePicture, process ImageReader start.");
            Image image = reader.acquireLatestImage();
            File outputPath = getOutputMediaFile();
            ImageSaver imageSaver = new ImageSaver(image, outputPath, null, mCameraCharacteristics, mApp);
            mCameraHandler.post(imageSaver);
            Log.i(TAG, "takePicture, process ImageReader Done.");
        }
    };

    private File getOutputMediaFile() {

        File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "SimpleCamera2");
        path.mkdir();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String photoPath = null;

        switch (PICTURE_FORMAT) {
            case ImageFormat.JPEG:
                photoPath = path.getPath() + File.separator + "IMG_" + timeStamp + ".jpg";
                break;
            case ImageFormat.RAW_SENSOR:
                photoPath = path.getPath() + File.separator + "RAW_" + timeStamp + ".dng";
                break;
        }

        Log.i(TAG, photoPath);
        File photo = new File(photoPath);

        return photo;
    }

    public void takePicture() {
        Log.i(TAG, "takePicture, Start.");
        CaptureRequest.Builder builder = getCaptureBuilder();
        if (builder != null) {
            try {
                mPreviewSession.capture(builder.build(), mCaptureCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(TAG, "takePicture, fail at capture.");
            }
        } else {
            Log.e(TAG, "takePicture, fail at builder is null.");
        }
        Log.i(TAG, "takePicture, Done.");
    }

    private CaptureRequest.Builder getCaptureBuilder() {
        try {
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(mPictureImageReader.getSurface());
            return builder;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "Create CaptureBuilder fail. need check.");
        return null;
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
}
