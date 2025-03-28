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
        ASensorEventQueue_setEventRate(sensorEventQueue, lightSensor, 1); // 1 kHz refresh rate
        ASensorEventQueue_setEventRate(sensorEventQueue, proximitySensor, 1); // 1 kHz refresh rate
        ASensorEventQueue_setEventRate(sensorEventQueue, accelerometerSensor, 1); // 1 kHz refresh rate
        ASensorEventQueue_setEventRate(sensorEventQueue, gyroscopeSensor, 1); // 1 kHz refresh rate

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
                sensor.setaccelx(event.acceleration.x);
                sensor.setaccely(event.acceleration.y);
                sensor.setaccelz(event.acceleration.z);
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

    initializeSensor();

    //async - start sensor reading
    std::thread sensorRead(sensorWorker, env, thisObj);
    sensorRead.detach();


}

extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplicationndk_Foreground_readSensor(
        JNIEnv* env,
        jobject thisObj) {

    //geting the class
    jclass retStringClass = env ->FindClass("com/example/myapplicationndk/Foreground");
    //getting the method to call within the class
    jmethodID retStringMethodID = env ->GetMethodID(retStringClass,"setValues","(FFFFFFFF)V");

    //Calling setValues from Foreground and setting values from here
    env->CallVoidMethod(thisObj, retStringMethodID,
                        (jfloat)sensor.getlight(),(jfloat) sensor.getproxi(),
                        (jfloat)sensor.getaccelx(),(jfloat) sensor.getaccely(), (jfloat)sensor.getaccelz(),
                        (jfloat)sensor.getgyrox(), (jfloat) sensor.getgyroy(), (jfloat)sensor.getgyroz());
}

////start sensor data gather on a different thread
//extern "C" JNIEXPORT void JNICALL
//Java_com_example_myapplicationndk_MainActivity_startSensorThread(
//        JNIEnv* env,
//        jobject thisObj) {
//
//    initializeSensor();
//
//    //async - start sensor reading
//    std::thread sensorRead(sensorWorker, env, thisObj);
//    sensorRead.detach();
//
//
//}


extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplicationndk_MainActivity_readSensor(
        JNIEnv* env,
        jobject thisObj) {

    //geting the class
    jclass retStringClass = env ->FindClass("com/example/myapplicationndk/MainActivity");
    //getting the method to call within the class
    jmethodID retStringMethodID = env ->GetMethodID(retStringClass,"setValues","(FFFFFFFF)V");

    //Calling setValues from MainActivity and setting values from here
    env->CallVoidMethod(thisObj, retStringMethodID,
                        (jfloat)sensor.getlight(),(jfloat) sensor.getproxi(),
                        (jfloat)sensor.getaccelx(),(jfloat) sensor.getaccely(), (jfloat)sensor.getaccelz(),
                        (jfloat)sensor.getgyrox(), (jfloat) sensor.getgyroy(), (jfloat)sensor.getgyroz());
}


extern "C" JNIEXPORT void JNICALL
Java_com_example_myapplicationndk_ServiceHandler_readSensor(
        JNIEnv* env,
        jobject thisObj) {

    //geting the class
    jclass retStringClass = env ->FindClass("com/example/myapplicationndk/ServiceHandler");
    //getting the method to call within the class
    jmethodID retStringMethodID = env ->GetMethodID(retStringClass,"setValues","(FFFFFFFF)V");

    //Calling setValues from ServiceHandler and setting values from here
    env->CallVoidMethod(thisObj, retStringMethodID,
                        (jfloat)sensor.getlight(),(jfloat) sensor.getproxi(),
                        (jfloat)sensor.getaccelx(),(jfloat) sensor.getaccely(), (jfloat)sensor.getaccelz(),
                        (jfloat)sensor.getgyrox(), (jfloat) sensor.getgyroy(), (jfloat)sensor.getgyroz());
}



