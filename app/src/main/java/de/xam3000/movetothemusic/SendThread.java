package de.xam3000.movetothemusic;

import android.util.Log;

import com.opencsv.CSVWriter;

import org.zeroturnaround.zip.ZipUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SendThread extends Thread{

    private final String fileName;
    private final String fileNameEnding;

    private final Long delay;

    private final Map<String, List<SensorData>> sensorEvents;

    private final List<SensorData> accuracyChange;

    private final File filesDir;

    private static final String LOG_TAG = "SendThread";

    SendThread(String fileName, String fileNameEnding, Long delay, Map<String, List<SensorData>> sensorEvents, File filesDir,List<SensorData> accuracyChange){
        this.fileName = fileName;
        this.fileNameEnding = fileNameEnding;
        this.delay = delay;
        this.sensorEvents = sensorEvents;
        this.filesDir = filesDir;
        this.accuracyChange = accuracyChange;
    }



    public void run() {


        File file = new File(filesDir, LocalDateTime.now().toString().replace(":","_"));

        if (!file.mkdir()){
            Log.e(LOG_TAG, "failed to create folder");
            return;
        }



        File music = new File(fileName);
        File copyMusic = new File(file, fileNameEnding);

        try {
            Files.copy(music.toPath(),copyMusic.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            File delayFile = new File(file,"delay");
            FileWriter writer = new FileWriter(delayFile);

            writer.write(delay.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sensorEvents.forEach((s, sensorData1) -> {
            try {
                String[] header ={"timestamp", "x", "y", "z"};

                CSVWriter writer = new CSVWriter(new FileWriter(new File(file, s + ".csv")));
                writer.writeNext(header);
                for (SensorData sensorData2 : sensorData1) {
                    writer.writeNext(sensorData2.toStringArray());
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        try {
            String[] header ={"timestamp", "sensor", "accuracy"};

            CSVWriter writer = new CSVWriter(new FileWriter(new File(file,"accuracy.csv")));
            writer.writeNext(header);
            for (SensorData sensorData: accuracyChange) {
                writer.writeNext(sensorData.toStringArray());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        File zip = new File(file.getPath() + ".zip");

        ZipUtil.pack(file, zip);

        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(zip.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }



        if (data != null) {
            sendData(data, zip.getName());
        }
    }

    private void sendData(byte[] data, String fileName) {

        String crlf = "\r\n";
        String twoHyphens = "--";
        String boundary =  "*****";

        HttpURLConnection client = null;
        URL url;
        try {
            url = new URL("http://xam3000.de:8000/upload");
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");
            client.setDoOutput(true);
            client.setUseCaches(false);

            client.setRequestMethod("POST");
            client.setRequestProperty("Connection", "Keep-Alive");
            client.setRequestProperty("Cache-Control", "no-cache");
            client.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream request = new DataOutputStream(client.getOutputStream());
            request.writeBytes(twoHyphens + boundary + crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"" +
                    "files" + "\";filename=\"" +
                    fileName + "\"" + crlf);
            request.writeBytes(crlf);
            request.write(data);

            request.writeBytes(crlf);
            request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

            request.flush();
            request.close();

            client.getResponseCode();

            InputStream responseStream = new BufferedInputStream(client.getInputStream());
            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream));

            String line;
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            responseStreamReader.close();



            responseStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Objects.requireNonNull(client).disconnect();
        }

    }

}
