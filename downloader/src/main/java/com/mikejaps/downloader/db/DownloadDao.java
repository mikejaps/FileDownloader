package com.mikejaps.downloader.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;


public class DownloadDao implements IDaoSupport<DownloadEntity> {
    private static final String TAG = "DownloadDao";
    // SQLiteDatabase
    private SQLiteDatabase mSqLiteDatabase;
    private static final String TABLE = DaoUtil.getTableName(DownloadEntity.class);

    public void init(SQLiteDatabase sqLiteDatabase) {
        this.mSqLiteDatabase = sqLiteDatabase;
    }

    @Override
    public long insert(DownloadEntity obj) {
        ContentValues values = contentValuesByObj(obj);
        return mSqLiteDatabase.insert(TABLE, null, values);
    }

    @Override
    public void insert(List<DownloadEntity> datas) {
        mSqLiteDatabase.beginTransaction();
        for (DownloadEntity data : datas) {
            insert(data);
        }
        mSqLiteDatabase.setTransactionSuccessful();
        mSqLiteDatabase.endTransaction();
    }


    public List<DownloadEntity> query(String selection, String... args) {
        Cursor cursor = mSqLiteDatabase.query(TABLE, null, selection,
                args, null, null, null, null);
        return cursorToList(cursor);
    }

    private List<DownloadEntity> cursorToList(Cursor cursor) {
        List<DownloadEntity> list = new ArrayList<>();
        Log.i(TAG, "cursorToList: " + cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                DownloadEntity entity = new DownloadEntity();
                long contentLength = cursor.getLong(cursor.getColumnIndex("contentLength"));
                long mEnd = cursor.getLong(cursor.getColumnIndex("mEnd"));
                long mStart = cursor.getLong(cursor.getColumnIndex("mStart"));
                long progress = cursor.getLong(cursor.getColumnIndex("progress"));
                int threadId = cursor.getInt(cursor.getColumnIndex("threadId"));
                String url = cursor.getString(cursor.getColumnIndex("url"));

                entity.setContentLength(contentLength);
                entity.setEnd(mEnd);
                entity.setMStart(mStart);
                entity.setProgress(progress);
                entity.setThreadId(threadId);
                entity.setUrl(url);

                list.add(entity);

            } while (cursor.moveToNext());
        }
        cursor.close();

        return list;
    }

    private ContentValues contentValuesByObj(DownloadEntity obj) {
        ContentValues values = new ContentValues();

        values.put("mStart", obj.getMStart());
        values.put("mEnd", obj.getEnd());
        values.put("contentLength", obj.getContentLength());
        values.put("progress", obj.getProgress());
        values.put("threadId", obj.getThreadId());
        values.put("url", obj.getUrl());

        return values;
    }

    /**
     * 删除
     */
    public int delete(String whereClause, String... whereArgs) {
        return mSqLiteDatabase.delete(TABLE, whereClause, whereArgs);
    }

    /**
     * 更新
     */
    public int update(DownloadEntity obj, String whereClause, String... whereArgs) {
        ContentValues values = contentValuesByObj(obj);
        return mSqLiteDatabase.update(TABLE,
                values, whereClause, whereArgs);
    }
}
