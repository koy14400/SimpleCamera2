package com.asus.simplecamera.cambase2;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

/**
 * Created by Tinghan_Chang on 2016/3/14.
 */
public class SingleCaptureLowLightProcess extends PostProcess {

    @Override
    protected void initOptionInfo() {
        mOptInfo.isApplyLowLight = true;
        mMaxRequestNumber = 3;
        mCaptureCount = 4;
        mCaptureFormat = ImageFormat.JPEG;
        mCaptureWidth = -1; // -1 mean not care picture size.
    }

    @Override
    public Image postProcess(Image[] image) {
        Log.i(TAG, "Do Low light post process");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 1; i < image.length; i++) {
            image[i].close();
        }
        return image[0];
    }

}
