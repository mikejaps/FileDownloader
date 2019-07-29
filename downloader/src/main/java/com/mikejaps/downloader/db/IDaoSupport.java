package com.mikejaps.downloader.db;

import android.database.sqlite.SQLiteDatabase;

import java.util.List;


public interface IDaoSupport<T> {

    void init(SQLiteDatabase sqLiteDatabase);

    long insert(T t);

    void insert(List<T> datas);

    List<T> query(String selection, String... args);

    int delete(String whereClause, String... whereArgs);

    int update(T obj, String whereClause, String... whereArgs);
}
