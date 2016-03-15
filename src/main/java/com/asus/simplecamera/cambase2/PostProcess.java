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

    public class OptInfo {
        public boolean isApplyBeauty = false;
        public boolean isApplyOptimal = false;
        public boolean isApplyLowLight = false;
        public boolean isApplyNighSense = false;
    }

    protected static String TAG = SimpleCameraApp.TAG;
    protected int mCaptureCount = 1;
    protected int mCaptureFormat = ImageFormat.JPEG;
    protected int mCaptureWidth = -1;
    protected int mBurstCount = 1;
    protected OptInfo mOptInfo = null;
    protected Surface mPreviewSurface = null;
    protected ImageReader mPictureImageReader = null;
    protected int mMaxRequestNumber = 3;

    public PostProcess() {

        mOptInfo = new OptInfo();
        initOptionInfo();

    }

    /**
     * For Child class override and implement.
     */
    protected abstract void initOptionInfo();

    /**
     * For Child class override and implement.
     */
    public abstract Image postProcess(Image[] image);

    /**
     * If this postProcess can't use current session.
     * Maybe we need to create a new session fit this postProcess.
     *
     * @param sessionImageReader
     * @return
     */
    public boolean isImageReaderMatch(ImageReader sessionImageReader) {
        if (sessionImageReader != null &&
                sessionImageReader.getImageFormat() == mCaptureFormat &&
                sessionImageReader.getMaxImages() >= mCaptureCount * mMaxRequestNumber &&
                (mCaptureWidth <= 0 || sessionImageReader.getWidth() == mCaptureWidth)) {
            mPictureImageReader = sessionImageReader;
            return true;
        }
        return false;
    }

    public int getBurstCount(){
        return mBurstCount;
    }

    public int getMaxRequestNumber() {
        return mMaxRequestNumber;
    }

    public ImageReader getPictureImageReader() {
        return mPictureImageReader;
    }

    /**
     * Initial session's total Preview and ImageReader surface.
     *
     * @param previewSurface
     * @param cameraCharacteristics
     * @return
     */
    public List<Surface> createOutputSurfaceList(Surface previewSurface, CameraCharacteristics cameraCharacteristics) {
        mPreviewSurface = previewSurface;
        List<Surface> outputSurface = new ArrayList<Surface>(mCaptureCount + 1);
        outputSurface.add(previewSurface);
        setCaptureImageReader(outputSurface, mCaptureFormat, mCaptureCount * mMaxRequestNumber, mCaptureWidth, cameraCharacteristics);
        return outputSurface;
    }

    /**
     * This is default preview builder.
     * Maybe child class need override.
     * Maybe Time rewind need this to implement.
     *
     * @param camera
     * @return
     */
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

    /**
     * @param camera
     * @param previewSurface
     * @param pictureImageReader
     * @return
     */
    public List<CaptureRequest> getCaptureBuilder(CameraDevice camera, Surface previewSurface, ImageReader pictureImageReader) {
        // TODO: return Builder[] to set HDR
        try {
            List<CaptureRequest> list = new ArrayList<CaptureRequest>(mCaptureCount*mBurstCount);
            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(previewSurface);
            builder.addTarget(pictureImageReader.getSurface());
            for (int i = 0; i < mBurstCount; i++) {
                for (int j = 0; j < mCaptureCount; j++) {
                    // TODO: HDR should implement 3 builder at child class.
                    list.add(builder.build());
                }
            }
            return list;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "Create CaptureBuilder fail. need check.");
        return null;
    }

    protected List<Surface> setCaptureImageReader(List<Surface> outputSurface, int targetFormat, int targetCount, int targetWidth, CameraCharacteristics cameraCharacteristics) {
        int picWidth = 0, picHeight = 0;
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            int[] colorFormat = map.getOutputFormats();
            for (int format : colorFormat) {
                Log.i(TAG, "Camera 0" + ": Supports color format " + formatToText(format));
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
        Log.i(TAG, "Camera 0, format:" + formatToText(targetFormat) + ": picture size W/H :" + picWidth + "/" + picHeight);
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
            case ImageFormat.RAW10:
                return "RAW10";
            case ImageFormat.RAW_SENSOR:
                return "RAW_SENSOR";
            case ImageFormat.JPEG:
                return "JPEG";
            case ImageFormat.NV16:
                return "NV16";
            case ImageFormat.NV21:
                return "NV21";
            case ImageFormat.YUV_420_888:
                return "YUV_420_888";
            case ImageFormat.YUY2:
                return "YUY2";
            case ImageFormat.YV12:
                return "YV12";
            case PixelFormat.RGBA_8888:
                return "RGBA_8888";
        }
        return "<unknown format>: " + Integer.toHexString(format);
    }
}
