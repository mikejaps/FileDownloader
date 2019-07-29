package com.mikejaps.downloader;

import android.content.Context;

import com.mikejaps.downloader.db.DownLoadDaoHelper;


public class DownloadManager {
    private static final DownloadManager downloadManager = new DownloadManager();

    private DownloadManager() {
    }

    public static DownloadManager getManager() {
        return downloadManager;
    }

    public void init(Context context) {
        DownLoadDaoHelper.getDownLoadDaoHelper().init(context);
    }

    public void startDownload(String url, String dir, String name, DownloadCallback callback) {
        DownloadDispatcher.getInstance().startDownload(url, dir, name, callback);
    }

    public void stopDownload(String url) {
        DownloadDispatcher.getInstance().stopDownLoad(url);
    }


    public void startDownload(String url) {
        //  DownloadDispatcher.getInstance().startDownload(url);
    }
}
