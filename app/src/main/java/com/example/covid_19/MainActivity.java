package com.example.covid_19;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int VIDEO_CAPTURE = 101;
    private Uri fileUri;
    Button symptom;
    Button heart_rate;
    Button upload;
    private Sensor mySensor;
    private SensorManager SM;
    private float[] mAccelerometerData = new float[3];
    Button respiratory_rate;
    float heart_rate_value;
    float respiratory_rate_value;
    int FRAMES = 21;
    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    static{
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        UI_connect();
        symptom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(v.getContext(), MainActivity2.class);
                myIntent.putExtra("Heart_Rate_value", Float.toString(heart_rate_value));
                myIntent.putExtra("Respiratory_Rate_value", Float.toString(respiratory_rate_value));
                startActivity(myIntent);
            }
        });
        if(!hasCamera()){
            heart_rate.setEnabled(false);
        }

        heart_rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });
        final int[] frame = {10};
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                while(frame[0] >=0){
                    //processing();
                   // frame[0]--;
                //}
                heart_rate_value=processing();
                Log.d("Heart Rate", ""+ heart_rate_value);
                respiratory_rate_value=respiratory_rate();
            }
        });
        SM = (SensorManager)getSystemService(SENSOR_SERVICE);
        // Accelerometer Sensor
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        respiratory_rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    SM.registerListener((SensorEventListener) MainActivity.this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
                    String[] a = Arrays.toString(mAccelerometerData).split("[\\[\\]]")[1].split(", ");
                    writeToCsv(a);


            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
            return;
        }
    }
    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    private void writeToCsv(String[] a) {

        File file = new File("/sdcard/");
        file.mkdirs();
        String csv = "/sdcard/myCSVBreathe.csv";

        try {
            CSVWriter csvWriter = new CSVWriter(new FileWriter(csv, true));
            csvWriter.writeNext(a);
            csvWriter.close();
            Toast.makeText(MainActivity.this, "File Successfully Created!!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void UI_connect()
    {
        symptom=(Button)findViewById(R.id.symptom);
        heart_rate=(Button)findViewById(R.id.heart_rate);
        upload=(Button)findViewById(R.id.upload);
        respiratory_rate=(Button)findViewById(R.id.respiratory_rate);
    }
    public void startRecording()
    {StrictMode.VmPolicy.Builder builder =new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        File mediaFile = new
                File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/myvideo.mp4");
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,45);
        fileUri = Uri.fromFile(mediaFile);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, VIDEO_CAPTURE);
    }

    private boolean hasCamera() {
        if (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_ANY)){
            return true;
        } else {
            return false;
        }
    }



    public void onActivityResult(int requestCode,
    int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_CAPTURE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Video has been saved to:\n" +
                        data.getData(), Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Video recording cancelled.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to record video",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public float processing() {

        String uri = "/sdcard/FingertipVideo.mp4";
        ArrayList<Float> bitmapArray = new ArrayList<>();
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(uri);
        int frameRate = 30000000;
        int frameCount = 0;
        float sum = (float) 0.0;
        int pixel, R;
        int window = 5;


        while (frameCount < FRAMES) {
            Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(frameRate);
            if (bmFrame == null)
                break;
            frameRate += 1000000;

            //extracting the red channel from bitmap frames
            for (int x = 0; x < bmFrame.getWidth(); x++) {
                for (int y = 0; y < bmFrame.getHeight(); y++) {
                    pixel = bmFrame.getPixel(x, y);
                    int redValue = Color.red(pixel);
                    sum += redValue;
                }
            }

            bitmapArray.add(sum / (bmFrame.getWidth() * bmFrame.getHeight()));

            sum = 0;
            frameCount++;
            Log.d("Generating Frame ","....");

        }
        ArrayList<Float> rolling_mean_list = rolling_mean(bitmapArray);

        Float[] rolling_mean_array = new Float[rolling_mean_list.size()];
        rolling_mean_array =rolling_mean_list.toArray(rolling_mean_array);
        ArrayList<Float[]> result = new ArrayList<>();

        for (int frame = 0; frame <= rolling_mean_array.length - window; frame += window) {
            Float[] newArray = Arrays.copyOfRange(rolling_mean_array, frame, frame + window);
            result.add(newArray);
        }
        int beatspersec=10;
        ArrayList<Integer> zc = new ArrayList<>();

        for (int i = 0; i < result.size(); i++) {
            int zeroCrossings = invokeZeroCrossing(result.get(i));
            zc.add(zeroCrossings);
        }


        for (int i = 0; i < zc.size(); i++) {
            sum += zc.get(i);
        }
        sum /= 2;
        float heartRate = (sum / zc.size()) * 12;
        heartRate *= beatspersec;
        return heartRate;

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mAccelerometerData[0] = event.values[0];
        mAccelerometerData[1] = event.values[1];
        mAccelerometerData[2] = event.values[2];


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public ArrayList<Float> rolling_mean(ArrayList<Float> bitmapArray) {
        int size = 5;
        ArrayList<Float> movingAvgArray = new ArrayList<>();

        for (int i = 0; i + size <= bitmapArray.size(); i++) {
            float sum = 0;
            for (int j = i; j < i + size; j++) {
                float temp = bitmapArray.get(j);
                sum += temp;
            }

            float average = sum / size;
            movingAvgArray.add(average);
        }
        return movingAvgArray;
    }
    public static int invokeZeroCrossing(Float[] div) {

        ArrayList<Float> diff = new ArrayList<>();
        for (int i = 0; i < div.length - 1; i++) {
            diff.add((div[i] - div[i + 1]));
        }

        return zeroCrossing(diff);
    }

    public static int zeroCrossing(ArrayList<Float> diff) {

        ArrayList<Integer> zc = new ArrayList<>();
        for (int i = 0; i < diff.size() - 1; i++) {
            zc.add(f(diff.get(i), diff.get(i + 1)));
        }

        int sum = 0;
        for (int i = 0; i < zc.size(); i++) {
            sum += zc.get(i);
        }

        return sum;
    }

    public static int f(float x, float y) {
        if (x * y < 0) return 1;
        else return 0;
    }

    public float respiratory_rate()
    {
        accelerometer acc=new accelerometer();
        ArrayList<Float> values= null;
        ArrayList<Float> values_x_axis= new ArrayList<>();
        int max_x=1280;
        try {
            values = acc.csvProcessing();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int i=0;i<max_x;i++)
        {
            values_x_axis.add(values.get(i));
        }

        ArrayList<Float> rolling_mean = rolling_mean(values);

        Float[] simpleMovingAvgArray = new Float[rolling_mean.size()];
        simpleMovingAvgArray = rolling_mean.toArray(simpleMovingAvgArray);

        int zeroCrossings = invokeZeroCrossing(simpleMovingAvgArray);
        zeroCrossings /= 2;



        float resp_rate = (zeroCrossings * 60) / values.size();
        Log.d("RESPIRATORY_RATE", String.valueOf(resp_rate));
        return resp_rate;

    }


}


