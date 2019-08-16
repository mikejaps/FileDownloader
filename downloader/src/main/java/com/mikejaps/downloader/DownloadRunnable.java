package com.mikejaps.downloader;


import com.mikejaps.downloader.db.DownLoadDaoHelper;
import com.mikejaps.downloader.db.DownloadEntity;
import com.mikejaps.downloader.http.HttpManager;
import com.mikejaps.downloader.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.Response;


/**
 * Description:
 * Data：4/19/2018-1:45 PM
 *
 * @author: mikejaps
 */
public class DownloadRunnable implements Runnable {
    private static final String TAG = "DownloadRunnable";
    private static final int STATUS_DOWNLOADING = 1;
    private static final int STATUS_STOP = 2;
    //线程的状态
    private int mStatus = STATUS_DOWNLOADING;
    //文件下载的url
    private String url;
    private String dir;
    //文件的名称
    private String name;
    //线程id
    private int threadId;
    //每个线程下载开始的位置
    private long start;
    //每个线程下载结束的位置
    private long end;
    //每个线程的下载进度
    private long mProgress;
    //文件的总大小 content-length
    private long mCurrentLength;
    private DownloadCallback downloadCallback;

    public int reTryCount = 3;

    public interface DownloadCallback {
        /**
         * 下载成功
         *
         * @param file
         */
        void onSuccess(File file);

        /**
         * 下载失败
         *
         * @param e
         */
        void onFailure(Exception e, long mProgress);

        /**
         * 下载进度
         *
         * @param progress
         */
        void onProgress(long progress, int threadId);

        /**
         * 暂停
         *
         * @param progress
         * @param threadId
         */
        void onPause(long progress, int threadId);
    }

    public DownloadRunnable(String name, String dir, String url, long currentLength, int threadId, long start, long end,
                            long progress,  DownloadCallback downloadCallback) {
        this.name = name;
        this.url = url;
        this.dir = dir;
        this.mCurrentLength = currentLength;
        this.threadId = threadId;
        this.start = start;
        this.end = end;
        this.mProgress = progress;
        this.downloadCallback = downloadCallback;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getStart() {
        return start;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;
        try {
            Response response = HttpManager.getInstance().syncResponse(url, start, end);
            inputStream = response.body().byteStream();
            //保存文件的路径
            File file = new File(dir, name);
            randomAccessFile = new RandomAccessFile(file, "rwd");
            //seek从哪里开始
            randomAccessFile.seek(start);
            int length;
            byte[] bytes = new byte[10 * 1024];
            while ((length = inputStream.read(bytes)) != -1) {
                if (mStatus == STATUS_STOP) {
                    downloadCallback.onPause(length, threadId);
                    break;
                }
                //写入
                randomAccessFile.write(bytes, 0, length);
                this.mProgress = this.mProgress + length;
                // Log.d(TAG, "run: mProgress=" + mProgress);
                //实时去更新下进度条
                downloadCallback.onProgress(mProgress, threadId);
            }
            if (mStatus != STATUS_STOP) {
                downloadCallback.onSuccess(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
            downloadCallback.onFailure(e,mProgress);
        } finally {
            Utils.close(inputStream);
            Utils.close(randomAccessFile);
            //保存到数据库
            saveToDb();

        }
    }

    public void stop() {
        mStatus = STATUS_STOP;
    }

    private void saveToDb() {
        DownloadEntity mDownloadEntity= new DownloadEntity();
        mDownloadEntity.setThreadId(threadId);
        mDownloadEntity.setUrl(url);
        mDownloadEntity.setStart(start);
        mDownloadEntity.setEnd(end);
        mDownloadEntity.setProgress(mProgress);
        mDownloadEntity.setContentLength(mCurrentLength);

        //保存到数据库
        DownLoadDaoHelper.getDownLoadDaoHelper().addEntity(mDownloadEntity);
    }

}
