package com.sonnhe.voicecommand.phonehelper.call;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    // 数据库文件名
    public static final String DB_NAME = "CallService.db";
    // 数据库表名
    public static final String TABLE_NAME = "call";
    // 数据库版本号
    public static final int DB_VERSION = 1;

    public static final String NAME = "name";
    public static final String PHONETIC = "phonetic";


    public static final String CREATE_CALL = "create table call ("

            + "id integer primary key autoincrement, "

            + "phonetic text, "

            + "name text)";


    private Context mContext;


    public MyDatabaseHelper(Context context) {

        super(context, DB_NAME, null, DB_VERSION);

        mContext = context;

    }


    @Override

    public void onCreate(SQLiteDatabase db) {

        db.execSQL(CREATE_CALL);

//        Toast.makeText(mContext, "Create succeeded", Toast.LENGTH_SHORT).show();

    }


    @Override

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    /**
     * 删除数据库
     * @return
     */
    public boolean deleteDatabase() {
        return mContext.deleteDatabase("CallService.db");
    }
}


