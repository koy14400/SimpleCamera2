package com.asus.simplecamera.cambase2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.asus.simplecamera.ImageSaver;

import java.util.List;

/**
 * Created by Tinghan_Chang on 2016/3/10.
 */
public class SingleCaptureNormalProcess extends PostProcess {
    public SingleCaptureNormalProcess(ImageSaver imageSaver, Handler saverHandler) {
        super(imageSaver, saverHandler);
    }

    @Override
    public CaptureRequest.Builder getPreviewBuilder(CameraDevice camera, Surface previewSurface) {
        CaptureRequest.Builder previewBuilder = null;
        try {
            previewBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(previewSurface);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getPreviewBuilder, preview builder create fail.");
            e.printStackTrace();
        }
        return previewBuilder;
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
        mOptInfo.isApplyBeauty = false;
        mOptInfo.isApplyNighSence = false;
        mOptInfo.isApplyOptimal = false;
        mOptInfo.isApplyLowLight = false;
    }

    @Override
    protected Image postProcess(OptInfo optInfo, Image image) {
        return image;
    }
}
