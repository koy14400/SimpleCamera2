package com.asus.simplecamera.cambase2;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.os.Handler;

/**
 * Created by Tinghan_Chang on 2016/3/10.
 */
public class SingleCaptureBeautyProcess extends PostProcess {

    public SingleCaptureBeautyProcess(TakePictureCallback takePictureCallback, Handler imageAvailableHandler) {
        super(takePictureCallback, imageAvailableHandler);
    }

    @Override
    public CaptureRequest.Builder getPreviewBuilder(CameraDevice camera) {
        return null;
    }

    @Override
    protected void initOptionInfo() {
        mOptInfo.isApplyBeauty = true;
    }

    @Override
    protected Image postProcess(OptInfo optInfo, Image image) {
        // Use JNI process image
        return image;
    }
}
