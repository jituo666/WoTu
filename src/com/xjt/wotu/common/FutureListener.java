package com.xjt.wotu.common;

public interface FutureListener<T> {
    public void onFutureDone(Future<T> future);
}