package com.asus.simplecamera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.media.Image;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Tinghan_Chang on 2016/3/3.
 */
public class ImageSaver implements Runnable {

    private static String TAG = SimpleCameraApp.TAG;
    private Image mImage;
    private final File mPicPath;

    /**
     * The CaptureResult for this image capture.
     */
    private final CaptureResult mCaptureResult;

    /**
     * The CameraCharacteristics for this camera device.
     */
    private final CameraCharacteristics mCharacteristics;
    private Context mContext;

    public ImageSaver(Image image, File picPath, CaptureResult result, CameraCharacteristics characteristics, Context context) {
        mImage = image;
        mPicPath = picPath;
        mCaptureResult = result;
        mCharacteristics = characteristics;
        mContext = context;
    }

    public void setImage(Image image) {
        mImage = image;
    }

    public void run() {
        if (mImage == null) {
            Log.e(TAG, "ImageSaver, image is null. Store fail.");
            return;
        }
        int format = mImage.getFormat();
        boolean success = false;

        switch (format) {
            case ImageFormat.JPEG: {
                Log.i(TAG, "ImageSaver, store image start. Format:JPEG.");
                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes, 0, buffer.capacity());

//                byte[] bytes = new byte[buffer.remaining()];
//                buffer.get(bytes);
                FileOutputStream output = null;

//                Bitmap pictureTaken = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                Matrix matrix = new Matrix();
//                matrix.preRotate(90);
//                pictureTaken = Bitmap.createBitmap(pictureTaken, 0, 0, pictureTaken.getWidth(), pictureTaken.getHeight(), matrix, true);

                try {
                    output = new FileOutputStream(mPicPath.getPath());
//                    pictureTaken.compress(Bitmap.CompressFormat.JPEG, 50, output);
//                    pictureTaken.recycle();
                    output.write(bytes);
                    output.close();
                    success = true;
                    Log.e(SimpleCameraApp.TAG, "Save image, Success");
                } catch (IOException e) {
                    Log.e(SimpleCameraApp.TAG, "Save image, exception");
                    e.printStackTrace();
                } finally {
                    mImage.close();
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            }
            case ImageFormat.RAW_SENSOR: {
                Log.i(TAG, "ImageSaver, store image start. Format:RAW_SENSOR.");
                DngCreator dngCreator = new DngCreator(mCharacteristics, mCaptureResult);
                FileOutputStream output = null;
                try {
                    output = new FileOutputStream(mPicPath.getPath());
                    dngCreator.writeImage(output, mImage);
                    success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mImage.close();
                    closeOutput(output);
                }
                break;
            }
            default: {
                Log.e(SimpleCameraApp.TAG, "Cannot save image, unexpected image format:" + format);
                mImage.close();
                break;
            }

        }
        if (success) {
            galleryAddPic(mPicPath);
        }

    }

    private static void closeOutput(OutputStream outputStream) {
        if (null != outputStream) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void galleryAddPic(File photoPath) {
        Log.i(TAG, "ImageSaver, MediaScan start.");
        Uri contentUri = Uri.fromFile(photoPath);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
        mContext.sendBroadcast(mediaScanIntent);
        Log.i(TAG, "ImageSaver, MediaScan done.");
    }
}
