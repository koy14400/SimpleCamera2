package com.asus.simplecamera.cambase2;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.asus.simplecamera.ImageSaver;
import com.asus.simplecamera.SimpleCameraApp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Tinghan_Chang on 2016/3/9.
 */
public class CamSessionV2 {
    private static String TAG = SimpleCameraApp.TAG;
    private static final int PICTURE_FORMAT = ImageFormat.JPEG;

    // Constructor
    private CameraDevice mCamera = null;
    private Handler mCameraHandler = null;
    private CameraCharacteristics mCameraCharacteristics = null;
    private Context mContext = null;


    private Surface mPreviewSurface = null;
    private CameraCaptureSession mSession = null;
    private ImageReader mPictureImageReader = null;
    private boolean mIsPreviewing = false;
    private int mImageReaderNumber = 0;
    private int mImageReaderFormat = ImageFormat.UNKNOWN;

    public CamSessionV2(Context context, CameraDevice camera, Handler handler, CameraCharacteristics cameraCharacteristics) {
        mContext = context;
        mCamera = camera;
        mCameraHandler = handler;
        mCameraCharacteristics = cameraCharacteristics;
    }

    public void release() {
        if (mSession != null) {
            mSession.close();
        }
        mPreviewSurface = null;
        mIsPreviewing = false;
        mContext = null;
        mCamera = null;
        mCameraHandler = null;
        mCameraCharacteristics = null;
    }

    /**
     * Maybe need to sync Camera and SurfaceView.
     * Maybe need to create SurfaceView after get camera size.
     */
    public void startPreview(Surface previewSurface, int outputCount, int targetFormat) {
        Log.e(TAG, "Try start preview.");
        if (previewSurface != null) {
            mPreviewSurface = previewSurface;
        }
        if (mCamera != null && mPreviewSurface != null) {
            int totalOutputNumber = 1 + outputCount;
            List<Surface> outputSurfaces = new ArrayList<Surface>(totalOutputNumber);
            outputSurfaces.add(mPreviewSurface);
            for (int i = 0; i < outputCount; i++) {
                mImageReaderNumber = outputCount;
                mImageReaderFormat = targetFormat;
                outputSurfaces = setCaptureImageReader(outputSurfaces, targetFormat);
            }
            try {
                Log.e(TAG, "createCaptureSession begin");
                mCamera.createCaptureSession(outputSurfaces, mSessionCallback, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else if (mPreviewSurface == null) {
            Log.e(TAG, "mPreviewSurface is null");
        } else if (mCamera == null) {
            Log.e(TAG, "mCamera is null");
        }
    }

    private CameraCaptureSession.StateCallback mSessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.i(TAG, "mSessionCallback, onConfigured done");
            mSession = session;
            CaptureRequest.Builder previewBuilder = getPreviewBuilder();
            CaptureRequest request = previewBuilder.build();
            try {
                Log.e(TAG, "setRepeatingRequest begin");
                mSession.setRepeatingRequest(request, null, mCameraHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
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
    };

    private CaptureRequest.Builder getPreviewBuilder() {
        CaptureRequest.Builder previewBuilder = null;
        try {
            previewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(mPreviewSurface);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getPreviewBuilder, preview builder create fail.");
            e.printStackTrace();
        }
        return previewBuilder;
    }

    private List<Surface> setCaptureImageReader(List<Surface> outputSurface, int targetFormat) {
        int picWidth = 0, picHeight = 0;
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            int[] colorFormat = map.getOutputFormats();
            for (int format : colorFormat) {
                Log.d(TAG, "Camera 0" + ": Supports color format " + formatToText(format));
                android.util.Size[] mColorSizes = map.getOutputSizes(format);
                for (android.util.Size s : mColorSizes)
                    Log.d(TAG, "Camera 0" + ": color size W/H:" + s.getWidth() + "/" + s.getHeight());
                if (format == targetFormat) {
                    Size size = mColorSizes[0];
                    picWidth = size.getWidth();
                    picHeight = size.getHeight();
                }
            }
        }
        Log.d(TAG, "Camera 0, format:" + targetFormat + ": picture size W/H :" + picWidth + "/" + picHeight);
        if (picWidth <= 0 || picHeight <= 0) {
            Log.e(TAG, "Camera 0" + ": picture size have some problem, need check!!!");
            return outputSurface;
        }
        mPictureImageReader = ImageReader.newInstance(picWidth, picHeight, targetFormat, 5);
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
            ImageSaver imageSaver = new ImageSaver(image, outputPath, null, mCameraCharacteristics, mContext);
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

    public void takePicture(PostProcess postProcess) {
        if (mCamera == null) return;
        int captureCount = postProcess.getmCaptureCount();
        int captureFormat = postProcess.getCaptureFormat();
        if (isNeedReCreateSession(captureCount, captureFormat)) {
            startPreview(mPreviewSurface, captureCount, captureFormat);
        } else {
            CaptureRequest.Builder builder = getCaptureBuilder();
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
    }

    public boolean isNeedReCreateSession(int captureCount, int captureFormat) {
        if (mSession != null && mImageReaderNumber == captureCount && mImageReaderFormat == captureFormat) {
            return false;
        }
        return true;
    }

    public void takePicture() {
        if (mCamera != null && mSession != null) {
            Log.i(TAG, "takePicture, Start.");
            CaptureRequest.Builder builder = getCaptureBuilder();
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
            Log.i(TAG, "takePicture, Done.");
        } else {
            Log.w(TAG, "takePicture, Capture fail::preview not ready.");
        }
    }

    private CaptureRequest.Builder getCaptureBuilder() {
        try {
            CaptureRequest.Builder builder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(mPreviewSurface);
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
