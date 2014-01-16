package com.wotu.data;

public interface MediaSet {
    public int getMediaItemCount();
    public long reload();
    public boolean isLoading();
    public String getName();
}
