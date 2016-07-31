package com.asus.simplecamera.previewtype;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.asus.simplecamera.SimpleCameraApp;

/**
 * Created by Tinghan_Chang on 2016/3/8.
 */
public abstract class CamPreviewV2 {
    public interface OnSurfaceReadyListener {
        public void onSurfaceReady(Surface surface);
    }
    public enum PreviewViewType {
        SurfaceView,
        TextureView,
        GLSurfaceView
    }

    protected static final String TAG = SimpleCameraApp.TAG;
    protected static final boolean mIsFullDeviceHeight = false;
    protected PreviewViewType mPreviewViewType;
    protected Size mPreviewSize = null;
    protected View mPreviewSurfaceView = null;
    protected Activity mApp = null;
    protected CameraManager mCameraManager;
    protected CameraCharacteristics mCameraCharacteristics;
    protected String[] mCameraId;
    protected OnSurfaceReadyListener onSurfaceReadyListener;

    public CamPreviewV2(Activity context, PreviewViewType viewType, OnSurfaceReadyListener listener) {
        mPreviewViewType = viewType;
        mApp = context;
        onSurfaceReadyListener = listener;

        mCameraManager = (CameraManager) mApp.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraId = mCameraManager.getCameraIdList();
            if (mCameraId.length > 0) {
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId[0]);
            } else {
                Log.e(TAG, "Device not support camera.");
                Toast.makeText(mApp, "Device not support camera.", Toast.LENGTH_SHORT).show();
                mApp.finish();
                return;
            }

            // Because camera2.0 only can control view size.
            // So we need to dynamic create view to fit sensor size.
            createSurfaceView();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Because camera2.0 only can control view size.
     * So we need to dynamic create view to fit sensor size.
     */
    protected abstract void createSurfaceView();
    public View getView() {
        return mPreviewSurfaceView;
    }

    protected ViewGroup.LayoutParams getPreviewLayoutParams() {

        Point screenSize = new Point();
        mApp.getWindowManager().getDefaultDisplay().getSize(screenSize);
        Rect activeArea = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        int sensorWidth, sensorHeight, previewWidth, previewHeight;
        // Make sensor's orientation same as screen.
        switch (sensorOrientation) {
            case 90:
            case 180:
                sensorWidth = activeArea.height();
                sensorHeight = activeArea.width();
                break;
            case 270:
            case 0:
            default:
                sensorWidth = activeArea.width();
                sensorHeight = activeArea.height();
                break;
        }
        Log.i(TAG, "Sensor Orientation angle:" + sensorOrientation);
        Log.i(TAG, "Sensor Width/Height : " + sensorWidth + "/" + sensorHeight);
        Log.i(TAG, "Screen Width/Height : " + screenSize.x + "/" + screenSize.y);
        // Preview's View size must same as sensor ratio.
        if (mIsFullDeviceHeight) {
            // full device height, maybe 16:9 at phone
            previewWidth = screenSize.y * sensorWidth / sensorHeight;
            previewHeight = screenSize.y;
        } else {
            // full device width, maybe 4:3 at phone
            previewWidth = screenSize.x;
            previewHeight = screenSize.x * sensorHeight / sensorWidth;
        }
        // Set margin to center at screen.
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(previewWidth, previewHeight);
        int widthMargin = (previewWidth - screenSize.x) / 2;
        int heightMargin = (previewHeight - screenSize.y) / 2;
        layoutParams.leftMargin = -widthMargin;
        layoutParams.topMargin = -heightMargin;
        Log.i(TAG, "LayoutMargin left/top : " + -widthMargin + "/" + -heightMargin);
        return layoutParams;
    }

}
