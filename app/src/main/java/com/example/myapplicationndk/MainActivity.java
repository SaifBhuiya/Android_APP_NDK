package com.example.myapplicationndk;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.example.myapplicationndk.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'myapplicationndk' library on application startup.
    static {
        System.loadLibrary("myapplicationndk");
    }

    private TextView testData, gyro, light, proximity, acceloro;
    private SensorManager sensorManager;
    private Sensor gyroSensor, lightSensor, accelSensor, proxiSensor;
    private DbHelper dbHelper;
    private float current_light, current_proximity,current_accelerometer_x,current_accelerometer_y,current_accelerometer_z,current_gyroscope_x,current_gyroscope_y,current_gyroscope_z;

    private Timer timer;
    private TimerTask timerTask;

    private LineChart lineChart;
    private List<String> xValues;

    String dataList,xdata,ydata,zdata = "";
    private Handler chartUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable chartUpdateRunnable;
    private String currentSensor = null;
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private ActivityMainBinding binding;
    private native void startSensorThread();
    private native void readSensor();
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        scheduleAlarm();

        //Bug Fix
        boolean firstLaunch = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getBoolean("first_launch", true);

        if (firstLaunch) {
            // Clear the pre-populated data
            SQLiteDatabase db = new DbHelper(this).getWritableDatabase();
            db.execSQL("DELETE FROM sensors");
            db.close();

            // Save that we've handled first launch
            getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_launch", false)
                    .apply();
        }

        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        boolean firstRun = prefs.getBoolean("firstRun", true);
        if (firstRun) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstRun", false);
            editor.apply();
        }


        //initiate foreground service
        Intent serviceIntent = new Intent(this, Foreground.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        foregroundServiceRunning();


//
//        binding = ActivityMainBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());

        //To hide Phone UI
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                // Your periodic task here
                readSensor();

                handler.postDelayed(this, 0);
            }
        };

        // Start the task immediately
        handler.post(runnable);

        //setting values to the variables declared earlier
        initialize();
        //to show notification
        checkNotificationPermission();
        //realtime chart updates
        startChartUpdates();

    }


    private void startChartUpdates() {
        // Create the runnable that will update the charts
        chartUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // Only update if a chart is currently displayed (currentSensor is set)
                if (currentSensor != null) {
                    switch (currentSensor) {
                        case "light":
                            //testData.setText(readSensorData("light"));

                            if(readSensorData("light") != ""){
                                LightChart();
                                testData.setText("");
                            }
                            else{
                                testData.setText("Fetching Sensor data...");
                            }
                            break;
                        case "proximity":
                            //testData.setText(readSensorData("proximity"));

                            if(readSensorData("proximity")!= ""){
                                ProximityChart();
                                testData.setText("");

                            }
                            else{
                                testData.setText("Fetching Sensor data...");
                            }
                            break;
                        case "accelerometer":
                            // testData.setText(readSensorData("accelerometer"));

                            // testData.setText(readSensorData("accelerometer"));
                            if(readSensorData("accelerometer")!= ""){
                                AccelerometerChart();
                                testData.setText("");

                            }else{
                                testData.setText("Fetching Sensor data...");
                            }
                            break;
                        case "gyroscope":
                            //testData.setText(readSensorData("gyroscope"));

                            if(readSensorData("gyroscope")!= ""){
                                GyroscopeChart();
                                testData.setText("");

                            }else{
                                testData.setText("Fetching Sensor data...");
                            }
                            break;
                    }
                }

                // Schedule the next update in 60 seconds
                chartUpdateHandler.postDelayed(this, 2000);
            }
        };

        // Start the updates immediately
        chartUpdateHandler.post(chartUpdateRunnable);
    }
    public boolean foregroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService((Context.ACTIVITY_SERVICE));
        for(ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)){
            if(Foreground.class.getName().equals(service.service.getClassName())){
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }//handle phone rotation

    public void initialize(){
        dbHelper = new DbHelper(this);

        // setting TextView variables
        gyro = findViewById(R.id.gyro_value);
        light = findViewById(R.id.light_value);
        proximity = findViewById(R.id.proxi_value);
        acceloro = findViewById(R.id.accel_value);

        //getting cards into variables
        CardView light_card = findViewById(R.id.light_sensor);
        CardView proxi_card = findViewById(R.id.proximity_sensor);
        CardView accel_card = findViewById(R.id.accelerometer);
        CardView gyro_card = findViewById(R.id.gyroscope);

        //setting onClick Listeners to Cards
        setClickListeners(light_card,0);
        setClickListeners(proxi_card,1);
        setClickListeners(accel_card,2);
        setClickListeners(gyro_card,3);


    }// set values to variables


    public void setClickListeners(CardView cardname, int sensornum) {
        cardname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.view_timeseries_graph);

                // Set the current sensor type based on sensornum
                if (sensornum == 0) {
                    currentSensor = "light";
                } else if (sensornum == 1) {
                    currentSensor = "proximity";
                } else if (sensornum == 2) {
                    currentSensor = "accelerometer";
                } else {
                    currentSensor = "gyroscope";
                }

                // Initialize the testData TextView
                testData = findViewById(R.id.light_data);

                // Update the chart immediately
                if (currentSensor.equals("light")) {
                    //testData.setText(readSensorData("light"));
                    if(readSensorData("light") != ""){
                        LightChart();
                        testData.setText("");
                    }
                    else{
                        testData.setText("Fetching Sensor data...");
                    }
                } else if (currentSensor.equals("proximity")) {
                    //testData.setText(readSensorData("proximity"));
                    if(readSensorData("proximity")!= ""){
                        ProximityChart();
                        testData.setText("");

                    }
                    else{
                        testData.setText("Fetching Sensor data...");
                    }

                } else if (currentSensor.equals("accelerometer")) {
                    // testData.setText(readSensorData("accelerometer"));
                    if(readSensorData("accelerometer")!= ""){
                        AccelerometerChart();
                        testData.setText("");

                    }else{
                        testData.setText("Fetching Sensor data...");
                    }

                } else {
                    //testData.setText(readSensorData("gyroscope"));
                    if(readSensorData("gyroscope")!= ""){
                        GyroscopeChart();
                        testData.setText("");

                    }else{
                        testData.setText("Fetching Sensor data...");
                    }
                }
            }
        });
    }//Detect which card has been clicked and adjust view accordingly


    private void scheduleAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE // FLAG_NO_CREATE prevents new PendingIntent creation
        );

        // Check if alarm is already scheduled
        if (pendingIntent != null) {
            // Alarm already exists, do nothing
            return;
        }

        // Create a new PendingIntent since none exists
        pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = System.currentTimeMillis() + 1000;

        if (alarmManager != null) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent
            );
        }
    }


    private void LightChart(){
        //https://www.youtube.com/watch?v=KIW4Vp8mjLo

        lineChart = findViewById(R.id.lineChart);
        Description description = new Description();
        description.setText("Sensor Value");
        description.setPosition(150f,15f);
        lineChart.setDescription(description);
        lineChart.getAxisRight().setDrawLabels(false);


        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(5f);
//        xValues = Arrays.asList("Nadun", "Kural", "Panther");
//
//        XAxis xAxis = lineChart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setValueFormatter(new IndexAxisValueFormatter(xValues));
//        xAxis.setLabelCount(3);
//        xAxis.setGranularity(1f);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        //limiting max scale to view clear graph
        yAxis.setAxisMaximum(500f);
        yAxis.setAxisLineWidth(2f);
        yAxis.setAxisLineColor(Color.BLACK);
        yAxis.setLabelCount(10);

        List<Entry> entries = new ArrayList<>();
        String[] items = dataList.split(",");
        int index = 0;
        for (String item : items) {
            entries.add(new Entry(index,Float.parseFloat(item)));

//        //update x axis value
//         List<String> xValues = new ArrayList<>(); // Create a dynamic list
//         xValues.add(index + " mins");
//        XAxis xAxis = lineChart.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setValueFormatter(new IndexAxisValueFormatter(xValues));
//        xAxis.setLabelCount(xValues.size(), true);
//        xAxis.setGranularity(1f);
//        lineChart.invalidate(); // Refresh chart


            //increment index for 5 mins
            index+=5;
        }


        LineDataSet dataSet1 = new LineDataSet(entries, "Light Sensor data");
        dataSet1.setColor(Color.BLUE);

        LineData lineData = new LineData(dataSet1);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }//Create chart if light card clicked

    private void ProximityChart(){

        lineChart = findViewById(R.id.lineChart);
        Description description = new Description();
        description.setText("Sensor Value");
        description.setPosition(150f,15f);
        lineChart.setDescription(description);
        lineChart.getAxisRight().setDrawLabels(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(5f);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(5f);
        yAxis.setAxisLineWidth(2f);
        yAxis.setAxisLineColor(Color.BLACK);
        yAxis.setLabelCount(10);

        List<Entry> entries = new ArrayList<>();
        String[] items = dataList.split(",");
        int index = 0;
        for (String item : items) {
            entries.add(new Entry(index,Float.parseFloat(item)));


            //increment index for 5 mins
            index+=5;
        }


        LineDataSet dataSet1 = new LineDataSet(entries, "Accelerometer Sensor data");
        dataSet1.setColor(Color.BLUE);

        LineData lineData = new LineData(dataSet1);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }//Create chart if proximity card clicked

    private void GyroscopeChart(){
        //https://www.youtube.com/watch?v=KIW4Vp8mjLo

        lineChart = findViewById(R.id.lineChart);
        Description description = new Description();
        description.setText("Sensor Value");
        description.setPosition(150f,15f);
        lineChart.setDescription(description);
        lineChart.getAxisRight().setDrawLabels(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(5f);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(-10f);
        //limiting max scale to view clear graph
        yAxis.setAxisMaximum(10f);
        yAxis.setAxisLineWidth(2f);
        yAxis.setAxisLineColor(Color.BLACK);
        yAxis.setLabelCount(10);

        List<Entry> entries = new ArrayList<>();
        String[] items = xdata.split(",");
        int index = 0;
        for (String item : items) {
            entries.add(new Entry(index,Float.parseFloat(item)));
            index+=5;
        }
        List<Entry> entries2 = new ArrayList<>();
        String[] items2 = ydata.split(",");
        index = 0;
        for (String item : items2) {
            entries2.add(new Entry(index,Float.parseFloat(item)));
            index+=5;
        }
        List<Entry> entries3 = new ArrayList<>();
        String[] items3 = zdata.split(",");
        index = 0;
        for (String item : items3) {

            entries3.add(new Entry(index,Float.parseFloat(item)));
            index+=5;

        }


        LineDataSet dataSet1 = new LineDataSet(entries, "Gyroscope X-axis data");
        dataSet1.setColor(Color.BLUE);

        LineDataSet dataSet2 = new LineDataSet(entries2, "Gyroscope Y-axis data");
        dataSet2.setColor(Color.RED);

        LineDataSet dataSet3 = new LineDataSet(entries3, "Gyroscope Z-axis data");
        dataSet3.setColor(Color.GREEN);

        LineData lineData = new LineData(dataSet1,dataSet2,dataSet3);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }//Create chart if gyroscope card clicked
    private void AccelerometerChart(){
        //https://www.youtube.com/watch?v=KIW4Vp8mjLo

        lineChart = findViewById(R.id.lineChart);
        Description description = new Description();
        description.setText("Sensor Value");
        description.setPosition(150f,15f);
        lineChart.setDescription(description);
        lineChart.getAxisRight().setDrawLabels(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(5f);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(-100f);
        //limiting max scale to view clear graph
        yAxis.setAxisMaximum(100f);
        yAxis.setAxisLineWidth(2f);
        yAxis.setAxisLineColor(Color.BLACK);
        yAxis.setLabelCount(10);

        List<Entry> entries = new ArrayList<>();
        String[] items = xdata.split(",");
        int index = 0;
        for (String item : items) {
            entries.add(new Entry(index,Float.parseFloat(item)));
            index+=5;
        }
        List<Entry> entries2 = new ArrayList<>();
        String[] items2 = ydata.split(",");
        index = 0;
        for (String item : items2) {
            entries2.add(new Entry(index,Float.parseFloat(item)));
            index+=5;
        }
        List<Entry> entries3 = new ArrayList<>();
        String[] items3 = zdata.split(",");
        index = 0;
        for (String item : items3) {

            entries3.add(new Entry(index,Float.parseFloat(item)));
            index+=5;

        }


        LineDataSet dataSet1 = new LineDataSet(entries, "Accelerometer X-axis data");
        dataSet1.setColor(Color.BLUE);

        LineDataSet dataSet2 = new LineDataSet(entries2, "Accelerometer Y-axis data");
        dataSet2.setColor(Color.RED);

        LineDataSet dataSet3 = new LineDataSet(entries3, "Accelerometer Z-axis data");
        dataSet3.setColor(Color.GREEN);

        LineData lineData = new LineData(dataSet1,dataSet2,dataSet3);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }//Create chart if accelerometer card clicked

    public void back_button_click(View v){
        currentSensor = null;

        setContentView(R.layout.activity_main);
        initialize();


    }//returns view to home page and reinitialized view



    public String readSensorData(String columnName){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        dataList = "";
        xdata = "";
        ydata = "";
        zdata = "";

        Cursor cursor;


        // If the column is gyroscope, query all three sub-columns
        if (columnName.equals("gyroscope")) {
            cursor = db.query("sensors", new String[]{"gyroscope_x", "gyroscope_y", "gyroscope_z"},
                    null, null, null, null, null);
        }
        else if(columnName.equals("accelerometer")) {
            cursor = db.query("sensors", new String[]{"accelerometer_x", "accelerometer_y", "accelerometer_z"},
                    null, null, null, null, null);
        }
        else {
            cursor = db.query("sensors", new String[]{columnName},
                    null, null, null, null, null);
        }

        if (cursor != null) {
            while (cursor.moveToNext()) {
                if (columnName.equals("light") || columnName.equals("proximity")) {
                    int columnIndex = cursor.getColumnIndex(columnName);
                    if (columnIndex != -1) {
                        float value = cursor.getFloat(columnIndex);
                        dataList += value + ",";

                    }
                }
                if (columnName.equals("gyroscope")) {
                    int xIndex = cursor.getColumnIndex("gyroscope_x");
                    int yIndex = cursor.getColumnIndex("gyroscope_y");
                    int zIndex = cursor.getColumnIndex("gyroscope_z");

                    if (xIndex != -1) {
                        xdata += cursor.getFloat(xIndex) + ",";
                        dataList  = xdata;
                    }
                    if (yIndex != -1) ydata += cursor.getFloat(yIndex) + ",";
                    if (zIndex != -1) zdata += cursor.getFloat(zIndex) + ",";

                    // Display xdata to test
//                   Toast.makeText(MainActivity.this, "X: " + zdata, Toast.LENGTH_SHORT).show();
                }
                if(columnName.equals("accelerometer")) {
                    int xIndex = cursor.getColumnIndex("accelerometer_x");
                    int yIndex = cursor.getColumnIndex("accelerometer_y");
                    int zIndex = cursor.getColumnIndex("accelerometer_z");

                    if (xIndex != -1) {
                        xdata += cursor.getFloat(xIndex) + ",";
                        dataList = xdata;
                    }
                    if (yIndex != -1) ydata += cursor.getFloat(yIndex) + ",";
                    if (zIndex != -1) zdata += cursor.getFloat(zIndex) + ",";

                    // Display xdata to test
                    //Toast.makeText(MainActivity.this, "X: " + xdata, Toast.LENGTH_SHORT).show();
                }
            }
            cursor.close();
        }
        db.close();

        return dataList;
    }//Read data from SQLite to use in Chart

    private void setValues(float lightv, float proxi,float accelx,float accely,float accelz,float gyrox,float gyroy,float gyroz){
//        long timestamp = System.currentTimeMillis();
//        Log.d("SENSOR_REFRESH", "Sensor: " + "gyro" +
//                " | Timestamp: " + timestamp +
//                " | Value: " + current_gyroscope_x);
        current_light = lightv;
        current_proximity = proxi;
        current_accelerometer_x = accelx;
        current_accelerometer_y = accely;
        current_accelerometer_z = accelz;
        current_gyroscope_x = gyrox;
        current_gyroscope_y = gyroy;
        current_gyroscope_z = gyroz;

        light.setText("Light Sensor\n" + current_light);
        proximity.setText("Proximity Sensor\n" + current_proximity);
        acceloro.setText("Accelerometer\n" + "X: " + current_accelerometer_x + "\n" + "Y: " + current_accelerometer_y + "\n" + "Z: " + current_accelerometer_z );
        gyro.setText("Gyroscope\n" + "X: " + current_gyroscope_x + "\n" + "Y: " + current_gyroscope_y + "\n" + "Z: " +current_gyroscope_z);

    }

    @Override
    protected void onResume() {
        super.onResume();
        currentSensor = null;
        setContentView(R.layout.activity_main);
        initialize();
        chartUpdateHandler.post(chartUpdateRunnable);
    }

    //
    @Override
    protected void onStop(){
        super.onStop();
        chartUpdateHandler.removeCallbacks(chartUpdateRunnable);

    }
}

