package de.xam3000.movetothemusic;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

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
    private Button syncButton;

    private Intent intent;

    private String fileName = null;
    private String fileNameEnding = null;

    private boolean collecting = false;
    private boolean syncing = false;

    private TextView textView;

    private SensorManager sensorManager;

    private Map<String, List<SensorData>> sensorEvents;

    private List<SensorData> accuracyChange;

    private MediaRecorder recorder = null;

    private SyncHelper syncHelper;

    private static Long start = null;
    //private Long delay = null;


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
        syncButton = findViewById(R.id.button_sync);
        textView = findViewById(R.id.textView);

        sensorButton.setOnClickListener((View view) -> collectSensorData());

        sendButton.setOnClickListener((View view) -> sendData());

        syncButton.setOnClickListener((View view) -> sync());

        fileName = this.getFilesDir().toString();
        fileNameEnding = "audio.mp4";
        fileName += "/" + fileNameEnding;

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    }

    private void sendData() {
        //new SendThread(fileName,fileNameEnding,sensorEvents,getFilesDir()).start();
    }

    private void collectSensorData() {
        if (start == null)
            return;
        if (collecting) {
            //sensorManager.unregisterListener(this);
            //stopRecording();

            stopService(intent);

            sensorButton.setText(R.string.collect_data);

        } else {
            /*sensorEvents = new HashMap<>();



            sensorEvents.put(Sensor.STRING_TYPE_ACCELEROMETER, new ArrayList<>());
            sensorEvents.put(Sensor.STRING_TYPE_GYROSCOPE, new ArrayList<>());
            accuracyChange = new ArrayList<>();

            //startRecording();

            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
            */

            intent = new Intent(this, CollectingService.class);
            intent.putExtra("start",start);
            intent.putExtra("folder",getFilesDir());
            startService(intent);



            sensorButton.setText(R.string.stop_collecting);
        }
        collecting = !collecting;
    }

    private void sync(){
        if (syncing) {
            sensorManager.unregisterListener(syncHelper);
            syncButton.setText(R.string.sync);
        } else {
            syncHelper = new SyncHelper(this);
            sensorManager.registerListener(syncHelper,sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_FASTEST);
            syncButton.setText(R.string.cancel);
        }
        syncing = !syncing;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Objects.requireNonNull(sensorEvents.get(sensorEvent.sensor.getStringType())).add(new SensorData(sensorEvent,start));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        long timestamp = SystemClock.elapsedRealtimeNanos();
        float[] values = {sensor.getType(),i};
        SensorData sensorData = new SensorData(values,timestamp - start);
        accuracyChange.add(sensorData);
        sensor.getType();

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
        recorder.setAudioSamplingRate(44100);

        try {
            recorder.prepare();
        } catch (IOException e) {
            final String LOG_TAG = "MainActivity";
            Log.e(LOG_TAG,  "prepare() failed");
        }
        //long before = SystemClock.elapsedRealtimeNanos();
        recorder.start();
        //long after = SystemClock.elapsedRealtimeNanos();
        //start = (after + before)/2;
        //delay = after - before;


    }

    public void setStart(Long start) {
        MainActivity.start = start;
        sensorManager.unregisterListener(syncHelper);
        syncButton.setText(R.string.sync);
        textView.setText(start.toString());
        syncing = false;
    }
}