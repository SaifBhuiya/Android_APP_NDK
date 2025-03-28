#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/sensor.h>
#include <thread>



#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "MyApp", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "MyApp", __VA_ARGS__))
ASensorManager* sensorManager = nullptr;
ASensorEventQueue* sensorEventQueue = nullptr;
const ASensor* accelerometerSensor = nullptr;
const ASensor* lightSensor = nullptr;
const ASensor* proximitySensor = nullptr;
const ASensor* gyroscopeSensor = nullptr;


class Sensor{
private:
    float light;
    float proxi;
    float accelx;
    float accely;
    float accelz;
    float gyrox;
    float gyroy;
    float gyroz;

public:
    Sensor(): light(0.0f),proxi(0.0f),accelx(0.0f), accely(0.0f), accelz(0.0f), gyrox(0.0f),gyroy(0.0f),gyroz(0.0f) {}

    //getters
    float getlight(){
        return light;
    }
    float getproxi(){
        return proxi;
    }
    float getaccelx(){
        return accelx;
    }
    float getaccely(){
        return accely;
    }
    float getaccelz(){
        return accelz;
    }
    float getgyrox(){
        return gyrox;
    }
    float getgyroy(){
        return gyroy;
    }
    float getgyroz(){
        return gyroz;
    }

    //setters
    void setlight(float Val){
        light=Val;
    }
    void setproxi(float Val){
        proxi=Val;
    }
    void setaccelx(float Val){
        accelx=Val;
    }
    void setaccely(float Val){
        accely=Val;
    }
    void setaccelz(float Val){
        accelz=Val;
    }
    void setgyrox(float Val){
        gyrox=Val;
    }
    void setgyroy(float Val){
        gyroy=Val;
    }
    void setgyroz(float Val){
        gyroz=Val;
    }
};
Sensor sensor;



// Function to initialize sensor
bool initializeSensor() {
    //sensor manager
    sensorManager = ASensorManager_getInstance();
    if (!sensorManager) {
        LOGE("Sensor Manager not available");
        return false;
    }

    //get sensor
    accelerometerSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_ACCELEROMETER);
    lightSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_LIGHT);
    proximitySensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_PROXIMITY);
    gyroscopeSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_GYROSCOPE);

    if (!lightSensor) {
            LOGI("light sensor not found!");
            return false;
        }

    if (!proximitySensor) {
            LOGI("proxi sensor not found!");
            return false;
        }

    if (!accelerometerSensor) {
            LOGI("Accelerometer sensor not found!");
            return false;
        }

    if (!gyroscopeSensor) {
            LOGI("Gyro sensor not found!");
            return false;
        }

    //eventquee to store data
    ALooper* looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
    sensorEventQueue = ASensorManager_createEventQueue(sensorManager, looper, 0, NULL, NULL);
    if (!sensorEventQueue) {
        LOGE("Failed to create sensor event queue");
        return false;
    }

//     Enable the sensors (register?)
        ASensorEventQueue_enableSensor(sensorEventQueue, lightSensor);
        ASensorEventQueue_enableSensor(sensorEventQueue, proximitySensor);
        ASensorEventQueue_enableSensor(sensorEventQueue, accelerometerSensor);
        ASensorEventQueue_enableSensor(sensorEventQueue, gyroscopeSensor);

    //set refresh rate
        ASensorEventQueue_setEventRate(sensorEventQueue, lightSensor, 100); // 1 kHz refresh rate
        ASensorEventQueue_setEventRate(sensorEventQueue, proximitySensor, 100); // 1 kHz refresh rate
        ASensorEventQueue_setEventRate(sensorEventQueue, accelerometerSensor, 100); // 1 kHz refresh rate
        ASensorEventQueue_setEventRate(sensorEventQueue, gyroscopeSensor, 100); // 1 kHz refresh rate

    return true;
}


// Worker function to continuously fetch sensor data
void sensorWorker(JNIEnv* env, jobject thisObj) {
    LOGI("Sensor Worker Called!");
    ASensorEvent event;
    while (true) {
        if (ASensorEventQueue_getEvents(sensorEventQueue, &event, 1) > 0) {
            if (event.type == ASENSOR_TYPE_LIGHT) {

                  sensor.setlight(event.light);
            }
            if (event.type == ASENSOR_TYPE_PROXIMITY) {

                    sensor.setproxi(event.distance);

            }
            if (event.type == ASENSOR_TYPE_ACCELEROMETER) {
                LOGI("Combuccha!");
                sensor.setaccelx(event.acceleration.x);
                sensor.setaccely(event.acceleration.y);
                sensor.setaccelz(event.acceleration.z);
                LOGI("innerx = %f", sensor.getaccelx());

            }
            if (event.type == ASENSOR_TYPE_GYROSCOPE) {
                sensor.setgyrox(event.gyro.x);
                sensor.setgyroy(event.gyro.y);
                sensor.setgyroz(event.gyro.z);
            }
        }
    }
}

//start sensor data gather on a different thread
extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplicationndk_Foreground_startSensorThread(
        JNIEnv* env,
        jobject thisObj) {
    LOGI("Sensor Worker Called!");

    initializeSensor();

    //async - start sensor reading
    std::thread sensorRead(sensorWorker, env, thisObj);
    sensorRead.detach();

    LOGI("x = %f", sensor.getaccelx());

}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplicationndk_Foreground_readSensor(
        JNIEnv* env,
        jobject thisObj) {

    //geting the class
    jclass retStringClass = env ->FindClass("com/example/myapplicationndk/Foreground");
    //getting the method to call within the class
    jmethodID retStringMethodID = env ->GetMethodID(retStringClass,"setValues","(FFFFFFFF)V");
    __android_log_print(ANDROID_LOG_ERROR, "SensorLog",
                        "thisObj: %p, retStringMethodID: %p, light: %f, proxi: %f, accelx: %f, accely: %f, accelz: %f, gyrox: %f, gyroy: %f, gyroz: %f",
                        thisObj, retStringMethodID,
                        (jfloat)sensor.getlight(),(jfloat) sensor.getproxi(),
                        (jfloat)sensor.getaccelx(),(jfloat) sensor.getaccely(), (jfloat)sensor.getaccelz(),
                        (jfloat)sensor.getgyrox(), (jfloat) sensor.getgyroy(), (jfloat)sensor.getgyroz());

    //making the call and returning
    env->CallVoidMethod(thisObj, retStringMethodID,
                        (jfloat)sensor.getlight(),(jfloat) sensor.getproxi(),
                        (jfloat)sensor.getaccelx(),(jfloat) sensor.getaccely(), (jfloat)sensor.getaccelz(),
                        (jfloat)sensor.getgyrox(), (jfloat) sensor.getgyroy(), (jfloat)sensor.getgyroz());
}

//
//class SensorHandler {
//private:
//    ASensorManager* sensorManager = nullptr;
//    ASensorEventQueue* sensorEventQueue = nullptr;
//    const ASensor* accelerometerSensor = nullptr;
//    const ASensor* lightSensor = nullptr;
//    const ASensor* proximitySensor = nullptr;
//    const ASensor* gyroscopeSensor = nullptr;
//    bool isInitialized = false;
//
//public:
//    // Initialize sensor manager and event queue
//    bool initialize() {
//        // Get the Sensor Manager
//        sensorManager = ASensorManager_getInstance();
//        if (!sensorManager) {
//            LOGI("Failed to get ASensorManager");
//            return false;
//        }
//
//        // Get the accelerometer sensor
//        lightSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_LIGHT);
//        proximitySensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_PROXIMITY);
//        accelerometerSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_ACCELEROMETER);
//        gyroscopeSensor = ASensorManager_getDefaultSensor(sensorManager, ASENSOR_TYPE_GYROSCOPE);
//
//        if (!lightSensor) {
//            LOGI("light sensor not found!");
//            return false;
//        }
//
//        if (!proximitySensor) {
//            LOGI("proxi sensor not found!");
//            return false;
//        }
//
//        if (!accelerometerSensor) {
//            LOGI("Accelerometer sensor not found!");
//            return false;
//        }
//
//        if (!gyroscopeSensor) {
//            LOGI("Gyro sensor not found!");
//            return false;
//        }
//
//        // Create a Looper to handle events
//        ALooper* looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
//        sensorEventQueue = ASensorManager_createEventQueue(sensorManager, looper, 0, NULL, NULL);
//
//        // Enable the accelerometer sensor
//        ASensorEventQueue_enableSensor(sensorEventQueue, lightSensor);
//        ASensorEventQueue_enableSensor(sensorEventQueue, proximitySensor);
//        ASensorEventQueue_enableSensor(sensorEventQueue, accelerometerSensor);
//        ASensorEventQueue_enableSensor(sensorEventQueue, gyroscopeSensor);
//
//
//        ASensorEventQueue_setEventRate(sensorEventQueue, lightSensor, 1000); // 1 kHz refresh rate
//        ASensorEventQueue_setEventRate(sensorEventQueue, proximitySensor, 1000); // 1 kHz refresh rate
//        ASensorEventQueue_setEventRate(sensorEventQueue, accelerometerSensor, 1000); // 1 kHz refresh rate
//        ASensorEventQueue_setEventRate(sensorEventQueue, gyroscopeSensor, 1000); // 1 kHz refresh rate
//
//        isInitialized = true;
//        LOGI("Sensor initialized successfully!");
//        return true;
//    }
//
//    // Continuously process sensor events
//    void processSensorEvents() {
//        if (!isInitialized) {
//            LOGI("Sensor not initialized");
//            return;
//        }
//
//        ASensorEvent event;
//
//        while (ASensorEventQueue_getEvents(sensorEventQueue, &event, 1) > 0) {
//            if (event.type == ASENSOR_TYPE_ACCELEROMETER) {
//                // Handle accelerometer data here
//                LOGI("Accelerometer Data - X: %f, Y: %f, Z: %f", event.acceleration.x, event.acceleration.y, event.acceleration.z);
//            }
//            if (event.type == ASENSOR_TYPE_GYROSCOPE) {
//                // Handle accelerometer data here
//                LOGI("Gyroscope Data - X: %f, Y: %f, Z: %f", event.gyro.x, event.gyro.y, event.gyro.z);
//            }
//            if (event.type == ASENSOR_TYPE_LIGHT) {
//                // Handle accelerometer data here
//                LOGI("Light Data - X: %f, Y: %f, Z: %f", event.light);
//            }
//            if (event.type == ASENSOR_TYPE_PROXIMITY) {
//                // Handle accelerometer data here
//                LOGI("Accelerometer Data - X: %f, Y: %f, Z: %f", event.distance);
//            }
//
//        }
//    }
//
//    // Destructor to clean up
//    ~SensorHandler() {
//        if (sensorEventQueue && sensorManager) {
//            ASensorManager_destroyEventQueue(sensorManager, sensorEventQueue);
//        }
//    }
//};


//extern "C" JNIEXPORT jstring JNICALL
//Java_com_example_myapplicationndk_MainActivity_callRetStringNative(
//        JNIEnv* env,
//        jobject thisObj/* this */,
//        jint numToPrint) {
//
//    //gettting the class
//    jclass retStringClass = env ->FindClass("com/example/myapplicationndk/MainActivity");
//    //getting the method to call within the class
//    jmethodID retStringMethodID = env ->GetMethodID(retStringClass,"retString","(I)Ljava/lang/String;");
//
//    //making the call and returning
//    return (jstring )env ->CallObjectMethod(thisObj,retStringMethodID,numToPrint);
//
//}

