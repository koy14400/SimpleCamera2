package com.asus.simplecamera.cambase2;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.asus.simplecamera.SimpleCameraApp;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Tinghan_Chang on 2016/3/10.
 */
public abstract class PostProcess {
    public interface TakePictureCallback {
        public void onImageReady(Image image);
    }

    public class OptInfo {
        public boolean isApplyBeauty = false;
        public boolean isApplyOptimal = false;
        public boolean isApplyLowLight = false;
        public boolean isApplyNighSence = false;
    }

    protected static String TAG = SimpleCameraApp.TAG;
    protected TakePictureCallback mTakePictureCallback = null;
    protected Handler mImageAvailableHandler = null;
    protected int mCaptureCount = 1;
    protected int mCaptureFormat = ImageFormat.JPEG;
    protected int mCaptureWidth = -1;
    protected OptInfo mOptInfo = null;
    protected Surface mPreviewSurface = null;
    protected ImageReader mPictureImageReader = null;
    protected int mMaxRequestNumber = 3;

    public PostProcess(TakePictureCallback takePictureCallback, Handler imageAvailableHandler) {
        mTakePictureCallback = takePictureCallback;
        mImageAvailableHandler = imageAvailableHandler;

        // init global variable.
        mMaxRequestNumber = 3;
        mCaptureCount = 1;
        mCaptureFormat = ImageFormat.JPEG;
        mCaptureWidth = -1; // -1 mean not care picture size.

        mOptInfo = new OptInfo();
        initOptionInfo();
        processOptInfo();

    }

    /**
     * If this postProcess can't use current session.
     * Maybe we need to create a new session fit this postProcess.
     * @param sessionImageReader
     * @return
     */
    public boolean isImageReaderMatch(ImageReader sessionImageReader) {
        if (sessionImageReader != null &&
                sessionImageReader.getImageFormat() == mCaptureFormat &&
                sessionImageReader.getMaxImages() == mCaptureCount * mMaxRequestNumber &&
                (mCaptureWidth <= 0 || sessionImageReader.getWidth() == mCaptureWidth)) {
            mPictureImageReader = sessionImageReader;
            return true;
        }
        return false;
    }

    public int getMaxRequestNumber() {
        return mMaxRequestNumber;
    }

    public ImageReader getPictureImageReader() {
        return mPictureImageReader;
    }

    public List<Surface> createOutputSurfaceList(Surface previewSurface, CameraCharacteristics cameraCharacteristics) {
        mPreviewSurface = previewSurface;
        List<Surface> outputSurface = new ArrayList<Surface>(mCaptureCount + 1);
        outputSurface.add(previewSurface);
        for (int i = 0; i < mCaptureCount; i++) {
            setCaptureImageReader(outputSurface, mCaptureFormat, mCaptureCount * mMaxRequestNumber, mCaptureWidth, cameraCharacteristics);
        }
        return outputSurface;
    }


    public CaptureRequest.Builder getPreviewBuilder(CameraDevice camera) {
        CaptureRequest.Builder previewBuilder = null;
        try {
            previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(mPreviewSurface);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getPreviewBuilder, preview builder create fail.");
            e.printStackTrace();
        }
        return previewBuilder;
    }

    public CaptureRequest.Builder getCaptureBuilder(CameraDevice camera, Surface previewSurface, ImageReader pictureImageReader) {
        try {
            pictureImageReader.setOnImageAvailableListener(getImageAvailableListener(), mImageAvailableHandler);
            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(previewSurface);
            builder.addTarget(pictureImageReader.getSurface());
            return builder;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "Create CaptureBuilder fail. need check.");
        return null;
    }

    /**
     * For Child class override and implement.
     */
    protected abstract void initOptionInfo();

    /**
     * For Child class override and implement.
     */
    protected abstract Image postProcess(OptInfo optInfo, Image image);

    protected void processOptInfo() {
        if (mOptInfo != null) {
            // Init Capture Count.
            if (mOptInfo.isApplyLowLight) {
                mCaptureCount = 4;
            } else {
                mCaptureCount = 1;
            }

            // Init Capture Format.
            if (mOptInfo.isApplyNighSence || mOptInfo.isApplyLowLight || mOptInfo.isApplyOptimal || mOptInfo.isApplyBeauty) {
                mCaptureFormat = ImageFormat.YUV_420_888;
            } else {
                mCaptureFormat = ImageFormat.JPEG;
            }
        }
    }


    protected ImageReader.OnImageAvailableListener getImageAvailableListener() {
        return new CustomImageAvailableListener(mOptInfo);
    }

    protected class CustomImageAvailableListener implements ImageReader.OnImageAvailableListener {

        private OptInfo mOptInfo;

        public CustomImageAvailableListener(OptInfo optInfo) {
            mOptInfo = optInfo;
        }

        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "Capture, onImageAvailable.");
            Image image = reader.acquireNextImage();
            postProcess(mOptInfo, image);
            if (mTakePictureCallback != null)
                mTakePictureCallback.onImageReady(image);
        }
    }

    public void saveImage(Image image) {
        postProcess(mOptInfo, image);
        if (mTakePictureCallback != null)
            mTakePictureCallback.onImageReady(image);
    }

    protected List<Surface> setCaptureImageReader(List<Surface> outputSurface, int targetFormat, int targetCount, int targetWidth, CameraCharacteristics cameraCharacteristics) {
        int picWidth = 0, picHeight = 0;
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            int[] colorFormat = map.getOutputFormats();
            for (int format : colorFormat) {
                Log.d(TAG, "Camera 0" + ": Supports color format " + formatToText(format));
                android.util.Size[] mColorSizes = map.getOutputSizes(format);
                for (android.util.Size s : mColorSizes)
                    Log.d(TAG, "Camera 0" + ": color size W/H:" + s.getWidth() + "/" + s.getHeight());
                if (format == targetFormat) {
                    for (Size size : mColorSizes) {
                        // Try choose CaptureWidth
                        if (size.getWidth() == targetWidth) {
                            picWidth = size.getWidth();
                            picHeight = size.getHeight();
                        }
                    }
                    if (picWidth <= 0 || picHeight <= 0) {
                        Size size = mColorSizes[0];
                        picWidth = size.getWidth();
                        picHeight = size.getHeight();
                    }
                }
            }
        }
        Log.d(TAG, "Camera 0, format:" + targetFormat + ": picture size W/H :" + picWidth + "/" + picHeight);
        if (picWidth <= 0 || picHeight <= 0) {
            Log.e(TAG, "Camera 0" + ": picture size have some problem, need check!!!");
            return outputSurface;
        }
        mPictureImageReader = ImageReader.newInstance(picWidth, picHeight, targetFormat, targetCount);
        outputSurface.add(mPictureImageReader.getSurface());
        return outputSurface;
    }

    protected static String formatToText(int format) {
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
}
