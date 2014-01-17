package com.wotu.data.image;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;

import com.wotu.common.ThreadPool.Job;

public interface Image {

    public String getMimeType();

    public String getFilePath();

    public int getWidth();

    public int getHeight();

    public int getRotation();

    public int getFullImageRotation();

    public long getSize();

    public long getDateInMs();

    public void getLatLong(double[] latLong);

    public Job<Bitmap> loadThumnail(int type);

    public Job<BitmapRegionDecoder> loadImage();

    public void rotate(int degrees);

    public void delete();

    public void share();

    public void showOnMap();

    public ImageDetails getDetails();

    public int getSupportedOperations();

}
