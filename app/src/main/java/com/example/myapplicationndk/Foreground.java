package com.example.myapplicationndk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


import java.util.Timer;
import java.util.TimerTask;

public class Foreground extends Service  {

    // Used to load the 'myapplicationndk' library on application startup.
    static {
        System.loadLibrary("myapplicationndk");
    }
    private SensorManager sensorManager;
    private Sensor gyroSensor, lightSensor, accelSensor, proxiSensor;
    private float current_light, current_proximity,current_accelerometer_x,current_accelerometer_y,current_accelerometer_z,current_gyroscope_x,current_gyroscope_y,current_gyroscope_z;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "Foreground Service";


    private native void startSensorThread();
    private native void readSensor();

    private Handler handler;
    private Runnable runnable;

    public void onCreate() {
        super.onCreate();

        // Create notification channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID,
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        } else {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        // Initialize the notification builder
        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("My Application");

        startSensorThread();

        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                // Your periodic task here
                Log.d("ForegroundService", "Task is running");
                readSensor();
                updateNotification();
                Log.d("ForegroundService", "Gap");


                // Re-run the task after 1 second (1000 ms)
                handler.postDelayed(this, 0);
            }
        };

        // Start the task immediately
        handler.post(runnable);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Log.d("TAG","Foreground service running");
                        readSensor();
                        updateNotification();

//                        try {
//                            Thread.sleep(2000);
//                        }
//                        catch(InterruptedException e){
//
//                            e.printStackTrace();
//                        }
                    }
                }
        ).start();
       // readSensor();

        // Update notification with initial values
        updateNotification();

        // Start as foreground service
        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        return super.onStartCommand(intent, flags, startId);
    }

    private void setValues(float light, float proxi,float accelx,float accely,float accelz,float gyrox,float gyroy,float gyroz){
        current_light = light;
        current_proximity = proxi;
        current_accelerometer_x = accelx;
        current_accelerometer_y = accely;
        current_accelerometer_z = accelz;
        current_gyroscope_x = gyrox;
        current_gyroscope_y = gyroy;
        current_gyroscope_z = gyroz;

    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Add this method to update notification content
    private void updateNotification() {
        Log.d("ForegroundService", "Update"+ current_light);
        String sensorText = "Light Sensor: " + current_light +
        "\nProximity sensor: " + current_proximity +
                "\nAccelerometer (x, y, z): " + current_accelerometer_x + ", " +
                current_accelerometer_y + ", " + current_accelerometer_z +
                "\nGyroscope (x, y, z): " + current_gyroscope_x + ", " +
                current_gyroscope_y + ", " + current_gyroscope_z;

        notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(sensorText))
                .setContentText(sensorText);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        Log.d("ForegroundService", "Update done");

    }


}


