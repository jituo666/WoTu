package com.wotu.data.image;

import java.util.ArrayList;

public interface ImageSet {
    public ArrayList<Image> getMediaItem(int start, int count);
    public int getImageCount();
    public String getSetName();
    public long reload();
    public void share();
    public void showOnMap();
    public void delete();
    public int getSupportedOperations();
}
