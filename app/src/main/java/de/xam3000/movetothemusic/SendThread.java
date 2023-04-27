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

    private final File zip;

    private static final String LOG_TAG = "SendThread";

    SendThread(File zip){
        this.zip = zip;
    }



    public void run() {

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
