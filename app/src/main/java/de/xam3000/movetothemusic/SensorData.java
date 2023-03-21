package de.xam3000.movetothemusic;

import android.hardware.SensorEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SensorData implements  Serializable {


    private final long timestamp;
    private final float[] values;
    private static final long serialVersionUID = 2L;

    SensorData(SensorEvent sensorEvent, Long start) {
        this.timestamp = sensorEvent.timestamp - start;
        this.values = sensorEvent.values.clone();
    }
    SensorData(float[] values, Long timestamp) {
        this.timestamp = timestamp;
        this.values = values;
    }


    public String[] toStringArray() {
        List<String> strings = new ArrayList<>();
        strings.add(String.valueOf(timestamp));
        for (float f : values) {
            strings.add(String.valueOf(f));
        }

        String[] strings1 = new String[strings.size()];
        strings1 = strings.toArray(strings1);
        return strings1;
    }
}