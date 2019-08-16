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
    private int mThreadCount;
    //线程下载成功的个数，AtomicInteger
    private AtomicInteger mSuccessNumber;
    //总进度=每个线程的进度的和
    private long mTotalProgress;
    //正在执行下载任务的runnable
    private List<DownloadRunnable> mDownloadRunnables;
    private DownloadCallback mDownloadCallback;

    public DownloadTask(String name, String dir, String url, int threadSize, long contentLength, DownloadCallback callBack) {
        this.name = name;
        this.url = url;
        this.dir = dir;
        this.mThreadCount = threadSize;
        this.mContentLength = contentLength;
        this.mDownloadRunnables = new ArrayList<>();
        this.mDownloadCallback = callBack;
    }

    public void run() {
        mSuccessNumber = new AtomicInteger();
        File file = new File(dir, name);
        //每个线程的下载的大小threadSize
        long threadSize = mContentLength / mThreadCount;
        List<DownloadEntity> entities = DownLoadDaoHelper.getDownLoadDaoHelper().queryAll(url);
        boolean newStart = true;
        boolean dbExit = false;
        if (entities != null && entities.size() > 0)
            dbExit = true;

        if (file.exists()) {
            if (dbExit)
                newStart = false;
            else {
                for (int i = 0; i < 10; i++) {
                    Log.d(TAG, "init: file.exists db not exit new file");
                    file = new File(dir, name + "(" + i + ")");
                    if (!file.exists()) {
                        name = name + "(" + i + ")";
                        break;
                    }
                }
            }
        } else {
            if (dbExit) {
                Log.d(TAG, "init: file not exists, db  exit remove(url)");
                DownLoadDaoHelper.getDownLoadDaoHelper().remove(url);
            }
        }

        long[] pro = new long[mThreadCount];
        for (int i = 0; i < mThreadCount; i++) {
            //初始化的时候，需要读取数据库
            //开始下载的位置
            final int j = i;
            long start = i * threadSize;
            //结束下载的位置
            long end = start + threadSize - 1;
            if (i == mThreadCount - 1) {
                end = mContentLength;
            }
            DownloadEntity downloadEntity;
            if (newStart) {
                downloadEntity = new DownloadEntity(start, end, url, i, 0, end - start);
                Log.d(TAG, "init: 文件不存在从头下载");
            } else {
                downloadEntity = getEntity(i, entities);
                if (downloadEntity == null)
                    downloadEntity = new DownloadEntity(start, end, url, i, 0, end - start);
                start = start + downloadEntity.getProgress();
                mTotalProgress += downloadEntity.getProgress();
                pro[i] = downloadEntity.getProgress();
                if (threadSize <= downloadEntity.getProgress()) {
                    Log.d(TAG, "init: 文件存在，线程" + i + "已经下载完毕");
                    mSuccessNumber.incrementAndGet();
                    if (mSuccessNumber.get() == mThreadCount) {
                        Log.d(TAG, "init: 文件存在，所有线程已经下载完毕，直接返回");
                        mDownloadCallback.onSuccess(file);
                        DownloadDispatcher.getInstance().recyclerTask(url);
                        return;
                    }
                    continue;
                }
            }
            Log.d(TAG, "init: 上次保存的进度mTotalProgress=" + mTotalProgress);
            final DownloadRunnable downloadRunnable = new DownloadRunnable(name, dir, url, mContentLength, j, start, end,
                    downloadEntity.getProgress(), new DownloadRunnable.DownloadCallback() {
                @Override
                public void onFailure(Exception e, long mProgress) {
                    retryOrFail(j, e, mProgress);
                    DownloadDispatcher.getInstance().recyclerTask(url);
                }

                @Override
                public void onSuccess(File file) {
                    mSuccessNumber.incrementAndGet();
                    if (mSuccessNumber.get() == mThreadCount) {
                        mDownloadCallback.onSuccess(file);
                        DownloadDispatcher.getInstance().recyclerTask(url);
                        Log.d(TAG, "onSuccess............");
                    }
                }

                @Override
                public void onProgress(long progress, int threadId) {
                    pro[threadId] = progress;
                    synchronized (DownloadTask.this) {
                        long sum = 0;
                        for (int k = 0; k < mThreadCount; k++)
                            sum += pro[k];
                        mTotalProgress = sum;
                        Log.d(TAG, "onProgress: mTotalProgress = " + mTotalProgress + "; progress= " + progress + "; mContentLength = " + mContentLength);
                        mDownloadCallback.onProgress(mTotalProgress, mContentLength);
                    }
                }

                @Override
                public void onPause(long progress, int threadId) {
                    mDownloadCallback.onPause(mTotalProgress, threadId);
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
            DownloadDispatcher.getInstance().recyclerTask(url);
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
