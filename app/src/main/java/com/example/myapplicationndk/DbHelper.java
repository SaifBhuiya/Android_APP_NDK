package com.example.myapplicationndk;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    public DbHelper(Context context) {
        super(context, "sensor.db", null, 1);
    }



    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE sensors (id INTEGER PRIMARY KEY AUTOINCREMENT, light REAL, proximity REAL, accelerometer_x REAL, accelerometer_y REAL, accelerometer_z REAL, gyroscope_x REAL,gyroscope_y REAL, gyroscope_z REAL)"));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    public void insertSensorData(float light, float proximity, float accelerometer_x, float accelerometer_y,float accelerometer_z,float gyroscope_x, float gyroscope_y, float gyroscope_z) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("light", light);
        values.put("proximity", proximity);
        values.put("accelerometer_x", accelerometer_x);
        values.put("accelerometer_y", accelerometer_y);
        values.put("accelerometer_z", accelerometer_z);
        values.put("gyroscope_x", gyroscope_x);
        values.put("gyroscope_y", gyroscope_y);
        values.put("gyroscope_z", gyroscope_z);



        db.insert("sensors", null, values);
        db.close();

    }// write data to database

}

