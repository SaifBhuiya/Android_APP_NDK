package com.example.myapplicationndk;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ServiceHandler extends Worker {
    private SensorManager sensorManager;
    private Sensor gyroSensor, lightSensor, accelSensor, proxiSensor;
    private DbHelper dbHelper;
    private float current_light, current_proximity, current_accelerometer_x, current_accelerometer_y, current_accelerometer_z, current_gyroscope_x, current_gyroscope_y, current_gyroscope_z;

    private native void readSensor();

    private void setValues(float lightv, float proxi,float accelx,float accely,float accelz,float gyrox,float gyroy,float gyroz){
        current_light = lightv;
        current_proximity = proxi;
        current_accelerometer_x = accelx;
        current_accelerometer_y = accely;
        current_accelerometer_z = accelz;
        current_gyroscope_x = gyrox;
        current_gyroscope_y = gyroy;
        current_gyroscope_z = gyroz;

    }
    public ServiceHandler(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        dbHelper = new DbHelper(context);

    }

    @NonNull
    @Override
    public Result doWork() {

            readSensor();


        try {
            Thread.sleep(2000); // Allow time for sensors to collect data
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        // Store data in SQLite
        dbHelper.insertSensorData(
                current_light, current_proximity,
                current_accelerometer_x, current_accelerometer_y, current_accelerometer_z,
                current_gyroscope_x, current_gyroscope_y, current_gyroscope_z
        );

        return Result.success();
    }
}