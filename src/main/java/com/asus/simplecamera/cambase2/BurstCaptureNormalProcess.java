package com.asus.simplecamera.cambase2;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

/**
 * Created by Tinghan_Chang on 2016/3/14.
 */
public class BurstCaptureNormalProcess extends PostProcess {
    @Override
    protected void initOptionInfo() {
        mOptInfo.isApplyBeauty = false;
        mOptInfo.isApplyNighSense = false;
        mOptInfo.isApplyOptimal = false;
        mOptInfo.isApplyLowLight = false;
        mMaxRequestNumber = 10;
        mBurstCount = 10;
        mCaptureCount = 1;
        mCaptureFormat = ImageFormat.JPEG;
        mCaptureWidth = -1; // -1 mean not care picture size.
    }

    @Override
    public Image postProcess(Image[] image) {
        Log.i(TAG, "Do Normal burst post process");
        return image[0];
    }
}
