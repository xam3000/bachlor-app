package de.xam3000.movetothemusic;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.xam3000.movetothemusic.databinding.ActivityMainBinding;

public class MainActivity extends Activity implements SensorEventListener {

    //private final String LOG_TAG = "MainActivity";

    private Button sensorButton;

    private String fileName = null;
    private String fileNameEnding = null;

    private boolean collecting = false;

    private SensorManager sensorManager;

    private Map<String, List<SensorData>> sensorEvents;

    private MediaRecorder recorder = null;

    private Long start = null;
    private Long delay = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding;
        Button sendButton;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String[] permissionString = {Manifest.permission.RECORD_AUDIO};
        requestPermissions(permissionString,0);

        sensorButton = findViewById(R.id.button_sensor);
        sendButton = findViewById(R.id.button_send_sensor);

        sensorButton.setOnClickListener((View view) -> collectSensorData());

        sendButton.setOnClickListener((View view) -> sendData());

        fileName = this.getFilesDir().toString();
        fileNameEnding = "audio.mp4";
        fileName += "/" + fileNameEnding;


    }

    private void sendData() {
        new SendThread(fileName,fileNameEnding,delay,sensorEvents,getFilesDir()).start();
    }

    private void collectSensorData() {

        if (collecting) {
            sensorManager.unregisterListener(this);
            stopRecording();
            sensorButton.setText(R.string.collect_data);

        } else {
            sensorEvents = new HashMap<>();
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


            sensorEvents.put(Sensor.STRING_TYPE_ACCELEROMETER, new ArrayList<>());
            sensorEvents.put(Sensor.STRING_TYPE_GRAVITY, new ArrayList<>());
            sensorEvents.put(Sensor.STRING_TYPE_GYROSCOPE, new ArrayList<>());
            sensorEvents.put(Sensor.STRING_TYPE_LINEAR_ACCELERATION, new ArrayList<>());
            sensorEvents.put(Sensor.STRING_TYPE_ROTATION_VECTOR, new ArrayList<>());

            startRecording();

            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_FASTEST);

            sensorButton.setText(R.string.stop_collecting);
        }
        collecting = !collecting;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Objects.requireNonNull(sensorEvents.get(sensorEvent.sensor.getStringType())).add(new SensorData(sensorEvent,start));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    private void startRecording(){
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        recorder.setAudioEncodingBitRate(128000);
        recorder.setAudioSamplingRate(44000);

        try {
            recorder.prepare();
        } catch (IOException e) {
            final String LOG_TAG = "MainActivity";
            Log.e(LOG_TAG,  "prepare() failed");
        }
        long before = SystemClock.elapsedRealtimeNanos();
        recorder.start();
        long after = SystemClock.elapsedRealtimeNanos();
        start = (after + before)/2;
        delay = after - before;


    }
}