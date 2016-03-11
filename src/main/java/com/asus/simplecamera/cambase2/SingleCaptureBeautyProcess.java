package com.asus.simplecamera.cambase2;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.os.Handler;
import android.view.Surface;

import com.asus.simplecamera.ImageSaver;

import java.util.List;

/**
 * Created by Tinghan_Chang on 2016/3/10.
 */
public class SingleCaptureBeautyProcess extends PostProcess {
    public SingleCaptureBeautyProcess(ImageSaver imageSaver, Handler saverHandler) {
        super(imageSaver, saverHandler);
    }

    @Override
    public CaptureRequest.Builder getPreviewBuilder(CameraDevice camera) {
        return null;
    }

    @Override
    public CaptureRequest.Builder getCaptureBuilder(CameraDevice camera) {
        return null;
    }

    @Override
    public List<Surface> setCaptureImage(List<Surface> outputSurface, CameraCharacteristics cameraCharacteristics) {
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
