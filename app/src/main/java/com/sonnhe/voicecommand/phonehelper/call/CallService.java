package com.sonnhe.voicecommand.phonehelper.call;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

public class CallService {

    private Context context;
    private MyDatabaseHelper dbHelper;
//    private CharacterParser characterParser;
    private SQLiteDatabase mDatabase;
    private Cn2Spell mSpell;
    public CallService(Context context){
        this.context = context;
        dbHelper = new MyDatabaseHelper(context);
//        characterParser = new CharacterParser();
        mSpell = new Cn2Spell();
        //初始化数据库
        initDataBase();
    }

    private void initDataBase(){
        if(dbHelper.deleteDatabase()){
//            Toast.makeText(context, "删除数据库成功", Toast.LENGTH_SHORT).show();
            Log.e("CallService", "删除数据库成功");
        }else{
//            Toast.makeText(context, "删除数据库失败", Toast.LENGTH_SHORT).show();
            Log.e("CallService", "删除数据库失败");
        }
        mDatabase = dbHelper.getWritableDatabase();

        new Thread(){
            @Override
            public void run() {
                super.run();
                queryContactPhoneNumber();
            }
        }.start();


    }

    /**
     * 遍历通讯录姓名并存入数据库
     */
    private void queryContactPhoneNumber(){
        String[] cols = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                cols, null, null, null);
        assert cursor != null;
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            // 取得联系人名字
            int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            String name = cursor.getString(nameFieldColumnIndex);
//            String phonetic = characterParser.getSelling(name);
            String phonetic = mSpell.getPinYin(name);
            Log.e("phonetic", phonetic);
            ContentValues values = new ContentValues();
            values.put(MyDatabaseHelper.NAME, name);
            values.put(MyDatabaseHelper.PHONETIC, phonetic);
            mDatabase.insert(MyDatabaseHelper.TABLE_NAME, null, values);
        }
        cursor.close();
    }

    /**
     * 根据回传结果查找姓名
     * @param result 回传结果
     */
    public String queryPhonetic(String result){
//        String phonetic = characterParser.getSelling(result);
        String phonetic = mSpell.getPinYin(result);
        Cursor cursor = mDatabase.query(MyDatabaseHelper.TABLE_NAME,
                null,
                MyDatabaseHelper.PHONETIC + " like ?",
                new String[]{"%"+phonetic+"%"},
                null,
                null,
                null);

        int nameIndex = cursor.getColumnIndex((MyDatabaseHelper.NAME));
        String name = null;
        while (cursor.moveToNext()) {
            name = cursor.getString(nameIndex);
        }
        cursor.close();
        if (name == null){
            return null;
        }else{
//            nameNumberCall(name);
            return name;
        }
    }

    /**
     *根据名字拨打电话
     */
    public void nameNumberCall(String name) {
        Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        assert cursor != null;
        while (cursor.moveToNext()) {
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            Log.e("contactId", contactId);
            String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            Log.e("contactName", contactName);

            if (name.equals(contactName)) {
                Log.e("nameNumberCall", "识别成功");
                Cursor phone = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
                assert phone != null;
                if (phone.moveToNext()) {
                    Log.e("nameNumberCall", "moveToNext");
                    String phoneNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    Intent intentPhone = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                    phone.close();
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "权限认证失败", Toast.LENGTH_SHORT).show();
                        cursor.close();
                        return;
                    }else{
                        intentPhone.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intentPhone);
                        cursor.close();
                        return;
                    }
                }
            }
        }
        cursor.close();
        Toast.makeText(context, "没找到"+name, Toast.LENGTH_SHORT).show();
    }


//
//    private void queryContactPhoneNumber() {
//        String[] cols = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
//        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                cols, null, null, null);
//        for (int i = 0; i < cursor.getCount(); i++) {
//            Log.e("queryContactPhoneNumber", String.valueOf(cursor.getCount()));
//            cursor.moveToPosition(i);
//            // 取得联系人名字
//            int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
//            int numberFieldColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
//            String name = cursor.getString(nameFieldColumnIndex);
//            String number = cursor.getString(numberFieldColumnIndex);
//            Log.e("queryContactPhoneNumber", name + " " + number);
//        }
//    }
}
