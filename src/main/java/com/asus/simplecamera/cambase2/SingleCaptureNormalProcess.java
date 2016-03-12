package com.asus.simplecamera.cambase2;

import android.media.Image;
import android.os.Handler;

/**
 * Created by Tinghan_Chang on 2016/3/10.
 */
public class SingleCaptureNormalProcess extends PostProcess {

    public SingleCaptureNormalProcess(TakePictureCallback takePictureCallback, Handler imageAvailableHandler) {
        super(takePictureCallback, imageAvailableHandler);
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
