package com.wotu.data.image;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;

import com.wotu.common.ThreadPool.Job;

public interface Image {

    public String getMimeType();

    public String getFilePath();

    public int getWidth();

    public int getHeight();

    public int getFullImageRotation();

    public int getRotation();

    public long getSize();

    public long getDateInMs();

    public void getLatLong(double[] latLong);

    public Job<Bitmap> requestThumnail(int type);

    public Job<BitmapRegionDecoder> requestImage();

    public int getSupportedOperations();

    public void rotate(int degrees);

    public ImageDetails getDetails();

    public void delete();
}
