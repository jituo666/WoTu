package com.wotu.common;

public interface FutureListener<T> {
    public void onFutureDone(Future<T> future);
}