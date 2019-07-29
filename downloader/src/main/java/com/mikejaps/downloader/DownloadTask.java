package com.mikejaps.downloader;


import android.util.Log;

import com.mikejaps.downloader.db.DownLoadDaoHelper;
import com.mikejaps.downloader.db.DownloadEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description:每个apk的下载，这个类需要复用的
 * Data：4/19/2018-1:45 PM
 *
 * @author: mikejaps
 */
public class DownloadTask {
    private static final String TAG = "DownloadTask";
    //文件下载的url
    private String url;
    private String dir;
    //文件的名称
    private String name;
    //文件的大小
    private long mContentLength;
    //下载文件的线程的个数
    private int mThreadSize;
    //线程下载成功的个数，AtomicInteger
    private AtomicInteger mSuccessNumber;
    //总进度=每个线程的进度的和
    private long mTotalProgress;
    //正在执行下载任务的runnable
    private List<DownloadRunnable> mDownloadRunnables;
    private DownloadCallback mDownloadCallback;
    private DownloadEntity downloadEntity;

    public DownloadTask(String name, String dir, String url, int threadSize, long contentLength, DownloadCallback callBack) {
        this.name = name;
        this.url = url;
        this.dir = dir;
        this.mThreadSize = threadSize;
        this.mContentLength = contentLength;
        this.mDownloadRunnables = new ArrayList<>();
        this.mDownloadCallback = callBack;
    }

    public void init() {
        mSuccessNumber = new AtomicInteger();
        mTotalProgress = 0;
        File file = new File(dir, name);
        //每个线程的下载的大小threadSize
        long threadSize = mContentLength / mThreadSize;
        List<DownloadEntity> entities = DownLoadDaoHelper.getDownLoadDaoHelper().queryAll(url);
        for (int i = 0; i < mThreadSize; i++) {
            //初始化的时候，需要读取数据库
            //开始下载的位置
            final int j = i;
            long start = i * threadSize;
            //结束下载的位置
            long end = start + threadSize - 1;
            if (i == mThreadSize - 1) {
                end = mContentLength;
            }

            if (!file.exists()) {
                DownLoadDaoHelper.getDownLoadDaoHelper().remove(url);
                downloadEntity = new DownloadEntity(start, end, url, i, 0, mContentLength);
                Log.d(TAG, "init: 文件不存在从头下载" + downloadEntity.toString());
            } else {
                downloadEntity = getEntity(i, entities);

                start = start + downloadEntity.getProgress();
                mTotalProgress += downloadEntity.getProgress();
                if (threadSize <= downloadEntity.getProgress()) {
                    Log.d(TAG, "init: 文件存在，线程" +i+"已经下载完毕");
                    mSuccessNumber.incrementAndGet();
                    Log.d(TAG, "mSuccessNumber.get()="+mSuccessNumber.get()+" ; mThreadSize ="+mThreadSize);
                    if (mSuccessNumber.get() == mThreadSize) {
                        Log.d(TAG, "init: 文件存在，所有线程" +"已经下载完毕，直接返回");
                        mDownloadCallback.onSuccess(file);
                        DownloadDispatcher.getInstance().recyclerTask(DownloadTask.this);
                        return;
                    }
                    continue;
                }
                Log.d(TAG, "init: 上次保存的进度mTotalProgress=" + mTotalProgress);
            }


            final DownloadRunnable downloadRunnable = new DownloadRunnable(name, dir, url, mContentLength, j, start, end,
                    downloadEntity.getProgress(), downloadEntity, new DownloadRunnable.DownloadCallback() {
                @Override
                public void onFailure(Exception e, long mProgress) {
                    retryOrFail(j, e, mProgress);
                    //Logger.d(TAG, "onFailure = " + e);
                }

                @Override
                public void onSuccess(File file) {
                    mSuccessNumber.incrementAndGet();
                    if (mSuccessNumber.get() == mThreadSize) {
                        mDownloadCallback.onSuccess(file);
                        DownloadDispatcher.getInstance().recyclerTask(DownloadTask.this);
                        //如果下载完毕，清除数据库
                        // DaoManagerHelper.getManager().remove(url);
                        Log.d(TAG, "onSuccess............");
                    }
                }

                @Override
                public void onProgress(long progress, long currentLength) {
                    //叠加下progress，实时去更新进度条
                    //这里需要synchronized下
                    synchronized (DownloadTask.this) {
                        mTotalProgress = mTotalProgress + progress;
                        Log.d(TAG, "onProgress: mTotalProgress = " + mTotalProgress + "; mContentLength = " + mContentLength);
                        mDownloadCallback.onProgress(mTotalProgress, currentLength);
                    }
                }

                @Override
                public void onPause(long progress, long currentLength) {
                    mDownloadCallback.onPause(mTotalProgress, currentLength);
                }
            });
            //通过线程池去执行
            DownloadDispatcher.getInstance().executorService().execute(downloadRunnable);
            mDownloadRunnables.add(downloadRunnable);
        }
    }

    void retryOrFail(int i, Exception e, long mProgress) {
        DownloadRunnable downloadRunnable = mDownloadRunnables.get(i);
        if (downloadRunnable.reTryCount > 0) {
            Log.d(TAG, "线程" + i + "失败续传....................");
            downloadRunnable.reTryCount--;
            downloadRunnable.setStart(downloadRunnable.getStart() + mProgress);
            DownloadDispatcher.getInstance().executorService().execute(mDownloadRunnables.get(i));
        } else {
            mDownloadCallback.onFailure(e);
            stopDownload();
        }
    }

    private DownloadEntity getEntity(int threadId, List<DownloadEntity> entities) {
        for (DownloadEntity entity : entities) {
            if (threadId == entity.getThreadId()) {
                return entity;
            }
        }
        return null;
    }

    /**
     * 停止下载
     */
    public void stopDownload() {
        for (DownloadRunnable runnable : mDownloadRunnables) {
            runnable.stop();
        }
    }

    public String getUrl() {
        return url;
    }
}
