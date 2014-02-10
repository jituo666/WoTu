package com.wotu.data.load;

public interface DataLoadListener {
    public void onLoadingStarted();
    public void onLoadingFinished(boolean loadFailed);
}
