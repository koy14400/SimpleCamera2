package com.asus.simplecamera.cambase2;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.view.Surface;

import com.asus.simplecamera.ImageSaver;
import com.asus.simplecamera.SimpleCameraApp;

import java.util.List;


/**
 * Created by Tinghan_Chang on 2016/3/10.
 */
public abstract class PostProcess {
    public class OptInfo {
        public boolean isApplyBeauty = false;
        public boolean isApplyOptimal = false;
        public boolean isApplyLowLight = false;
        public boolean isApplyNighSence = false;
    }

    protected static String TAG = SimpleCameraApp.TAG;
    protected ImageSaver mImageSaver = null;
    protected Handler mSaverHandler = null;
    protected int mCaptureCount = 1;
    protected int mCaptureFormat = ImageFormat.JPEG;
    protected OptInfo mOptInfo = null;

    public PostProcess(ImageSaver imageSaver, Handler saverHandler) {
        mImageSaver = imageSaver;
        mSaverHandler = saverHandler;
        mOptInfo = new OptInfo();
        initOptionInfo();
        processOptInfo();

    }

    public int getmCaptureCount() {
        return mCaptureCount;
    }

    public int getCaptureFormat() {
        return mCaptureFormat;
    }

    public ImageReader.OnImageAvailableListener getImageAvailableListener() {
        return new CustomImageAvailableListener(mOptInfo);
    }

    public abstract CaptureRequest.Builder getPreviewBuilder(CameraDevice camera, Surface previewSurface);
    public abstract CaptureRequest.Builder getCaptureBuilder(CameraDevice camera);
    public abstract List<Surface> setCaptureImage(List<Surface> outputSurface, CameraCharacteristics cameraCharacteristics);
    public OptInfo getOptInfo() {
        return mOptInfo;
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

    protected class CustomImageAvailableListener implements ImageReader.OnImageAvailableListener {

        private OptInfo mOptInfo;

        public CustomImageAvailableListener(OptInfo optInfo) {
            mOptInfo = optInfo;
        }

        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            postProcess(mOptInfo, image);
            mSaverHandler.post(mImageSaver);
        }
    }

}
