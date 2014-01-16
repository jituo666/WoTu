package com.wotu.data.image;

import java.util.ArrayList;

public interface ImageSet {
    public ArrayList<Image> getMediaItem(int start, int count);
    public Image getCoverMediaItem();
    public int getMediaItemCount();
    public String getName();
    public long reload();
    public int getSupportedOperations();
    public void delete();
}
