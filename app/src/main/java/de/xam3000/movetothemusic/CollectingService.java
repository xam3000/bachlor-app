package de.xam3000.movetothemusic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.opencsv.CSVWriter;

import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class CollectingService extends Service implements SensorEventListener {

    private final String LOG_TAG = "CollectingService";

    private ReentrantLock accelLock;
    private ReentrantLock gyroLock;

    private List<SensorData> accelData;
    private List<SensorData> gyroData;

    private Map<String, List<SensorData>> sensorEvents;
    private static Long start = null;
    private SensorManager sensorManager;

    private File folder;

    private File accelFile = null;

    private File gyroFile = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        start =  intent.getLongExtra("start",0);

        sensorEvents = new HashMap<>();

        folder = new File(getFilesDir(), LocalDateTime.now().toString().replace(":","_"));

        if (!folder.mkdir()){
            Log.e(LOG_TAG, "failed to create folder");
            return START_REDELIVER_INTENT;
        }

        accelFile = new File(folder, Sensor.STRING_TYPE_ACCELEROMETER + ".csv");
        gyroFile = new File(folder, Sensor.STRING_TYPE_GYROSCOPE + ".csv");

        accelLock = new ReentrantLock();
        gyroLock = new ReentrantLock();

        accelData = new ArrayList<>();
        gyroData = new ArrayList<>();

        String[] header ={"timestamp", "x", "y", "z"};

        try {
            CSVWriter writer = new CSVWriter(new FileWriter(accelFile));
            writer.writeNext(header);
            writer.close();

            writer = new CSVWriter(new FileWriter(gyroFile));
            writer.writeNext(header);
            writer.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "onStartCommand: " + e.getMessage(), e.getCause());
            e.printStackTrace();
        }


        sensorEvents.put(Sensor.STRING_TYPE_ACCELEROMETER, new ArrayList<>());
        sensorEvents.put(Sensor.STRING_TYPE_GYROSCOPE, new ArrayList<>());

        //startRecording();

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(accelFile,true));
            for (SensorData sensorData : accelData) {
                writer.writeNext(sensorData.toStringArray());
            }
            writer.close();

            writer = new CSVWriter(new FileWriter(gyroFile,true));
            for (SensorData sensorData : gyroData) {
                writer.writeNext(sensorData.toStringArray());
            }
            writer.close();
        }catch (IOException e){
            Log.e(LOG_TAG, "onDestroy: " + e.getMessage(), e.getCause());
            e.printStackTrace();
        }

        File zip = new File(folder.getPath() + ".zip");

        ZipUtil.pack(folder, zip);


        new SendThread(zip).start();
        //super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            accelLock.lock();
            handleSensorEventAccel(sensorEvent);
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            gyroLock.lock();
            handleSensorEventGyro(sensorEvent);
        }

        //Objects.requireNonNull(sensorEvents.get(sensorEvent.sensor.getStringType())).add(new SensorData(sensorEvent,start));
    }

    private void handleSensorEventAccel(SensorEvent sensorEvent) {
        accelData.add(new SensorData(sensorEvent,start));
        if (accelData.size() > 6000) {
             List<SensorData> temp = accelData;
             accelData = new ArrayList<>();
             accelLock.unlock();
             try {
                 CSVWriter writer = new CSVWriter(new FileWriter(accelFile,true));
                 for (SensorData sensorData : temp) {
                     writer.writeNext(sensorData.toStringArray());
                 }
                 writer.close();
                 Log.d(LOG_TAG, "handleSensorEventAccel: wrote " + temp.size() + " entries");
             }catch (IOException e){
                 Log.e(LOG_TAG, "handleSensorEventAccel: "+ e.getMessage(), e.getCause());
                 e.printStackTrace();
             }
        } else {
            accelLock.unlock();
        }
    }

    private void handleSensorEventGyro(SensorEvent sensorEvent) {
        gyroData.add(new SensorData(sensorEvent,start));
        if (gyroData.size() > 12000) {
            List<SensorData> temp = gyroData;
            gyroData = new ArrayList<>();
            gyroLock.unlock();
            try {
                CSVWriter writer = new CSVWriter(new FileWriter(gyroFile,true));
                for (SensorData sensorData : temp) {
                    writer.writeNext(sensorData.toStringArray());
                }
                writer.close();
                Log.d(LOG_TAG, "handleSensorEventGyro: wrote " + temp.size() + " entries");
            }catch (IOException e){
                Log.e(LOG_TAG, "handleSensorEventGyro: "+ e.getMessage(), e.getCause());
                e.printStackTrace();
            }
        } else {
            gyroLock.unlock();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
