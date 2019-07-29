package com.mikejaps.downloader.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.List;


public class DownLoadDaoHelper {

    private static DownLoadDaoHelper helper;

    // 持有外部数据库的引用
    private SQLiteDatabase mSqLiteDatabase;
    private IDaoSupport downloadDao;

    private DownLoadDaoHelper() {
    }

    public static DownLoadDaoHelper getDownLoadDaoHelper() {
        if (helper == null) {
            synchronized (DownLoadDaoHelper.class) {
                if (helper == null) {
                    helper = new DownLoadDaoHelper();
                }
            }
        }
        return helper;
    }

    public void init(Context context) {
        // 把数据库放到内存卡里面  判断是否有存储卡 6.0要动态申请权限
        File dbRoot = new File(context.getCacheDir() + File.separator + "database");
        if (!dbRoot.exists()) {
            dbRoot.mkdirs();
        }
        File dbFile = new File(dbRoot, "download.db");

        // 打开或者创建一个数据库
        mSqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        String createTableSql = "create table if not exists DownloadEntity(id integer primary key autoincrement, contentLength long, mEnd long, mStart long, progress long, threadId integer, url text)";//sb.toString();

        // 创建表
        mSqLiteDatabase.execSQL(createTableSql);
        downloadDao = new DownloadDao();
        downloadDao.init(mSqLiteDatabase);
    }

    public void addEntity(DownloadEntity entity) {
        long delete = downloadDao.delete("url = ? and threadId = ?", entity.getUrl(), entity.getThreadId() + "");
        long size = downloadDao.insert(entity);
    }

    public List<DownloadEntity> queryAll(String url) {
        return downloadDao.query("url = ?", url);
    }

    public void remove(String url) {
        downloadDao.delete("url = ?", url);
    }
}
