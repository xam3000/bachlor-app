package de.xam3000.movetothemusic;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;

public class SyncHelper implements SensorEventListener {

    MainActivity mainActivity;

    public SyncHelper(MainActivity mainActivity){
        this.mainActivity = mainActivity;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double length = Math.sqrt(Math.pow(sensorEvent.values[0],2) + Math.pow(sensorEvent.values[1],2) + Math.pow(sensorEvent.values[2],2));

        if (length > 100){
            mainActivity.setStart(SystemClock.elapsedRealtimeNanos());
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
