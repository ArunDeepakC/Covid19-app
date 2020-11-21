package com.example.covid_19;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;


import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity2 extends AppCompatActivity {
    private Spinner list;
    private TextView text;
    private RatingBar rating;
    private Button upload;
    private Hashtable<String, Float> symptom_hash;
    private Button btn_upload;
    private EditText txt_name;
    private EditText txt_date;
    private String postBodyString;
    private MediaType mediaType;
    private RequestBody requestBody;
    private Button gps;

    Database db;
    Button save;
    String heart_rate_value, respiratory_rate_value;

    public MainActivity2() {
        symptom_hash = new Hashtable<String, Float>() {{

            put("Heart Rate", (float) 0);
            put("Respiratory Rate", (float) 0);
            put("Fever", (float) 0);
            put("Nausea", (float) 0);
            put("Headache", (float) 0);
            put("Diarrhea", (float) 0);
            put("Soar Throat", (float) 0);
            put("Muscle Ache", (float) 0);
            put("Loss Of Smell Or Taste", (float) 0);
            put("Cough", (float) 0);
            put("Shortness Of Breath", (float) 0);
            put("Feeling Tired", (float) 0);
            put("Latitude", (float) 0);
            put("Longitude", (float) 0);
        }};

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        UI_connect();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            heart_rate_value = extras.getString("Heart_Rate_value");
            respiratory_rate_value = extras.getString("Respiratory_Rate_value");
        } else {
            Log.e("exception", "Null event");
        }
        db = new Database(this);
        symptom_hash.put("Heart Rate", Float.parseFloat(heart_rate_value));
        symptom_hash.put("Respiratory Rate", Float.parseFloat(respiratory_rate_value));

        upload.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {

                float get_rating = rating.getRating();
                Log.e("1", "get_rating");

                symptom_hash.put((String) list.getSelectedItem(), get_rating);
                Log.e("2", "" + list.getSelectedItem());

            }
        });


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.insert(symptom_hash);
            }
        });

        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UploadTask upload=new UploadTask();
                upload.execute();
               // postRequest("your message here", "http://10.0.2.2:5000/");

            }
        });
        gps.setOnClickListener(v->{
            GPSSensor sensor= new GPSSensor(MainActivity2.this);
            if(sensor.canGetLocation()){
                double latitude = sensor.getLatitude();
                double longitude = sensor.getLongitude();

                Toast.makeText(this, "Location:\nLatitude: " + latitude + "\nLongitude: " + longitude, Toast.LENGTH_SHORT).show();
                symptom_hash.put("Latitude", (float) latitude);
                symptom_hash.put("Longitude", (float) longitude);
            }else{
                sensor.showSettingsAlert();
            }
        });
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void UI_connect() {
        list = (Spinner) findViewById(R.id.spinner2);
        rating = (RatingBar) findViewById(R.id.ratingBar2);
        upload = (Button) findViewById(R.id.upload);
        text = (TextView) findViewById(R.id.textView);
        save = (Button) findViewById(R.id.save);
        btn_upload = (Button) findViewById(R.id.btn_upload);
        txt_name = (EditText) findViewById(R.id.txt_name);
        txt_date = (EditText) findViewById(R.id.txt_date);
        gps=(Button)findViewById(R.id.btn_gps);

    }



    public class UploadTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            try {

                String url = "http://10.0.2.2:5000/androidInput";
                String charset = "UTF-8";

                String accept = "1";


                File videoFile = new File(Environment.getDataDirectory().getPath()+"/user/0/com.example.covid_19/databases/Symptoms.db");

                String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
                String CRLF = "\r\n"; // Line separator required by multipart/form-data.

                URLConnection connection;

                connection = new URL(url).openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (
                        OutputStream output = connection.getOutputStream();
                        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
                ) {

                    writer.append("--" + boundary).append(CRLF);
                    writer.append("Content-Disposition: form-data; name=\"accept\"").append(CRLF);
                    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                    writer.append(CRLF).append(accept).append(CRLF).flush();


                    writer.append("--" + boundary).append(CRLF);
                    writer.append("Content-Disposition: form-data; name=\"date\"").append(CRLF);
                    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                    writer.append(CRLF).append(txt_date.getText().toString()).append(CRLF).flush();


                    writer.append("--" + boundary).append(CRLF);
                    writer.append("Content-Disposition: form-data; name=\"name\"").append(CRLF);
                    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
                    writer.append(CRLF).append(txt_name.getText().toString()).append(CRLF).flush();



                    writer.append("--" + boundary).append(CRLF);
                    writer.append("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\"" + videoFile.getName() + "\"").append(CRLF);
                    //writer.append("Content-Type: video/mp4; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
                    writer.append(CRLF).flush();
                    FileInputStream vf = new FileInputStream(videoFile);
                    try {
                        byte[] buffer = new byte[1024];
                        int bytesRead = 0;
                        while ((bytesRead = vf.read(buffer, 0, buffer.length)) >= 0)
                        {
                            output.write(buffer, 0, bytesRead);

                        }
                        //   output.close();
                        //Toast.makeText(getApplicationContext(),"Read Done", Toast.LENGTH_LONG).show();
                    }catch (Exception exception)
                    {


                        //Toast.makeText(getApplicationContext(),"output exception in catch....."+ exception + "", Toast.LENGTH_LONG).show();
                        Log.d("Error", String.valueOf(exception));
                        publishProgress(String.valueOf(exception));
                        // output.close();

                    }

                    output.flush(); // Important before continuing with writer!
                    writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.


                    // End of multipart/form-data.
                    writer.append("--" + boundary + "--").append(CRLF).flush();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Request is lazily fired whenever you need to obtain information about response.
                int responseCode=((HttpURLConnection) connection).getResponseCode();


                System.out.println(responseCode); // Should be 200
                try {
                    //Object responseCode = ((HttpURLConnection) connection).getInputStream();

                    BufferedReader br = new BufferedReader(new InputStreamReader((((HttpURLConnection) connection).getInputStream())));
                    StringBuilder sb = new StringBuilder();


                    String response;
                    while ((response = br.readLine()) != null) {
                        sb.append(response);
                    }
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "adjacency_matrix.txt");
                    file.createNewFile();
                    FileOutputStream fOut = new FileOutputStream(file);
                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                    myOutWriter.append(sb);

                    myOutWriter.close();

                    fOut.flush();
                    fOut.close();

                }
                catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onProgressUpdate(String... text) {
            Toast.makeText(getApplicationContext(), "In Background Task " + text[0], Toast.LENGTH_LONG).show();
        }

    }
}


