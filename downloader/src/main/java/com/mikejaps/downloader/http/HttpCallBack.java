package com.mikejaps.downloader.http;

public interface HttpCallBack {
    void onSuccess(HttpResponse response);

    void onError(Exception e);
}
