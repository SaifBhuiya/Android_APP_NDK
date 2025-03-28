package com.example.myapplicationndk;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplicationndk.databinding.ActivityMainBinding;

import org.w3c.dom.Text;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {


    // Used to load the 'myapplicationndk' library on application startup.
    static {
        System.loadLibrary("myapplicationndk");
    }

    private ActivityMainBinding binding;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    //public native String LightSensorValue();
//    public native boolean initializeLightSensor();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkNotificationPermission();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        Intent serviceIntent = new Intent(this, Foreground.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        foregroundServiceRunning();



      //  TextView tv = binding.sampleText;
       // tv.setText(callRetString());
        // Start the sensor thread when the activity starts
      //  startSensorThread();

    }
    // JNI method to call the sensor thread
//    private native void startSensorThread();



    public boolean foregroundServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService((Context.ACTIVITY_SERVICE));
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (Foreground.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }



    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // Shows the permission request dialog
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE
                );
            }
        }
    }



}

