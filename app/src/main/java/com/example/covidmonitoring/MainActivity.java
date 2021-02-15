package com.example.covidmonitoring;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int VIDEO_CAPTURE = 101;
    int checked_respiratory=0;
    int check_hr_measure=0;
    ProgressDialog progressDialog;
    TextView disp_txt;
    double resp_rate = 0.0;
    CameraActivity cam_act;
    Algorithms algos = new Algorithms();

    String folder_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CovidMonitoring/";
    String vid_name = "heart_rate.mp4";
    String mpjeg_name = "heart_rate_conv_mp.mjpeg";
    String avi_name = "final_heart_rate.avi";
    double hr = 0.0;

    DatabaseActivty db_act;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configure_permissions();

        cam_act = new CameraActivity();
        db_act = new DatabaseActivty();
        progressDialog = new ProgressDialog(this);

        disp_txt = (TextView)findViewById(R.id.instruction);
        String start_display = "Hello World";
        disp_txt.setText(start_display);

        // Button handler for measuring heart rate
        Button heart_rate_btn = (Button)findViewById(R.id.HeartButton);
        heart_rate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                check_hr_measure = 1;
                start_recording_intent();
            }
        });



        Button symptomButtom = (Button)findViewById(R.id.symptoms);
        symptomButtom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent int1 = new Intent(MainActivity.this, SymptomsActivity.class);
                startActivity(int1);
            }
        });


        // Button handler for uploading heart and respiratory rate and go to the symptoms logging page
        Button sym_btn = (Button)findViewById(R.id.uploadData);
        sym_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // Before going to the next page check if the heart and respiratory rate is measured or not
                if (true || (check_hr_measure == 1 && checked_respiratory == 1)) {
                    db_act.create_database();  // Create the database
                    db_act.create_table();   // Create the table
                    int up_check = db_act.upload_hr_resp_rate(hr, resp_rate); // Insert the values in the database
                    if (up_check == 1) {
                        Toast.makeText(MainActivity.this, "Signs Uploaded Successfully", Toast.LENGTH_LONG).show();
                    }
                    Intent intent = new Intent(MainActivity.this, SymptomsActivity.class);
                    startActivity(intent);
                }
                else
                {
                    Toast.makeText(MainActivity.this,"Please Measure the Heart and Respiratory Rate first.", Toast.LENGTH_LONG).show();
                }
            }
        });


        String TAG = "Open CV works!";

        if(OpenCVLoader.initDebug())
        {
            Toast.makeText(this,TAG, Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this,"A locha", Toast.LENGTH_SHORT).show();
        }


        // Respiratory
        Button respiratory_botton = (Button)findViewById(R.id.resp_button);
        respiratory_botton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checked_respiratory = 1;
                showProgressDialogWithTitle("Put your phone on Chest, Calculating Respiratory Rate...");
                Intent intent = new Intent(MainActivity.this, AccelerometerService.class);
                startService(intent);  // Calls the Accelerometer Service

            }
        });


    }

    // Function to perform action after the camera intent is finished
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_CAPTURE) {
            if (resultCode == RESULT_OK )
            {
                // The rest of the code takes the video into the input stream and writes it to the location given in the internal storage
                Log.d("uy","ok res");
                File newfile;
                //data.
                AssetFileDescriptor videoAsset = null;
                FileInputStream in_stream = null;
                OutputStream out_stream = null;
                try {

                    videoAsset = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
                    Log.d("uy","vid ead");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    in_stream = videoAsset.createInputStream();
                    Log.d("uy","in stream");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d("uy","dir");
                Log.d("uy",Environment.getExternalStorageDirectory().getAbsolutePath());
                File dir = new File(folder_path);
                if (!dir.exists())
                {
                    dir.mkdirs();
                    Log.d("uy","mkdir");
                }

                newfile = new File(dir, vid_name);
                Log.d("uy","hr");

                if (newfile.exists()) {
                    newfile.delete();
                }


                try {
                    out_stream = new FileOutputStream(newfile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                byte[] buf = new byte[1024];
                int len;

                while (true) {
                    try
                    {
                        Log.d("uy","try");
                        if (((len = in_stream.read(buf)) > 0))
                        {
                            Log.d("uy","File write");
                            out_stream.write(buf, 0, len);
                        }
                        else
                        {
                            Log.d("uy","else");
                            in_stream.close();
                            out_stream.close();
                            break;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                // Function to convert video to avi for processing the heart rate
                convert_video_commands();

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


    // Function to convert video to avi for processing the heart rate
    public void convert_video_commands()
    {
        //Loads the ffmpeg library
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onFailure() {
                }

                @Override
                public void onSuccess() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }

        // If the .mpjep files exist it deletes the older file
        File newfile = new File(folder_path + mpjeg_name);

        if (newfile.exists()) {
            newfile.delete();
        }

        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(new String[]{"-i", folder_path + vid_name, "-vcodec", "mjpeg", folder_path + mpjeg_name}, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart()
                {
                    showProgressDialogWithTitle("Converting to AVI and Measuring Heart Rate");
                }

                @Override
                public void onProgress(String message)
                {

                }

                @Override
                public void onFailure(String message) {
                }

                @Override
                public void onSuccess(String message)
                {

                }

                @Override
                public void onFinish()
                {

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }

        // If the .avi file exist it deletes the older file
        File avi_newfile = new File(folder_path + avi_name);

        if (avi_newfile.exists()) {
            avi_newfile.delete();
        }
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(new String[]{"-i", folder_path + mpjeg_name, "-vcodec", "mjpeg", folder_path + avi_name}, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart()
                {

                }

                @Override
                public void onProgress(String message)
                {

                }

                @Override
                public void onFailure(String message) {
                }

                @Override
                public void onSuccess(String message)
                {


                }

                @Override
                public void onFinish()
                {

                    while(true)
                    {

                        try {
                            // Calculate the heart rate
                            String heart_rate = cam_act.measure_heart_rate(folder_path, avi_name);

                            VideoCapture videoCapture = new VideoCapture();
                            String videoPath=folder_path ;
                            String videoName=avi_name;
                            // Reading the .avi file into opencv functions
                            if(new File(videoPath + videoName).exists()){
                                //Log.d(TAG, "AVI file exists!");
                                videoCapture.open(videoPath + videoName);
                                if(videoCapture.isOpened()){
                                    //Log.d(TAG, "isOpened() works!");

                                    Mat current_frame = new Mat();
                                    Mat next_frame = new Mat();
                                    Mat diff_frame = new Mat();

                                    List<Double> extremes = new ArrayList<Double>();


                                    int video_length = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_COUNT);
                                    //Log.d(TAG, "Video Length: " + video_length);
                                    int frames_per_second = (int) videoCapture.get(Videoio.CAP_PROP_FPS);
                                    //Log.d(TAG, "Frames per second: " + frames_per_second);

                                    List<Double> list = new ArrayList<Double>();

                                    // Processing the video to calculate the mean red pixel values in the frame
                                    videoCapture.read(current_frame);
                                    for(int k = 0; k < video_length - 1; k++){
                                        videoCapture.read(next_frame);
                                        Core.subtract(next_frame, current_frame, diff_frame);
                                        next_frame.copyTo(current_frame);
                                        list.add(Core.mean(diff_frame).val[0] + Core.mean(diff_frame).val[1] + Core.mean(diff_frame).val[2]);
                                    }

                                    List<Double> new_list = new ArrayList<Double>();
                                    for(int i = 0; i < (Integer)(list.size()/5) - 1; i++){
                                        List<Double> sublist = list.subList(i*5, (i+1)*5);
                                        double sum = 0.0;
                                        for(int j = 0; j < sublist.size(); j++){
                                            sum += sublist.get(j);
                                        }

                                        new_list.add(sum/5);
                                    }

                                    int mov_period = 50;
                                    // Calculating the moving average and performing peak detection on the signal
                                    List<Double> avg_data = algos.calc_mov_avg(mov_period, new_list);
                                    int peak_counts = algos.count_zero_crossings(avg_data);

                                    double fps_to_sec = (video_length/frames_per_second);
                                    double count_heart_rate = (peak_counts/2)*(60)/fps_to_sec;

                                    //return ""+count_heart_rate;

                                }
                                else{
                                    //Log.d(TAG, ":(");
                                    //return "";
                                }
                            }
                            else{
                                //Log.d(TAG, "AVI file does not exist!");
                                //return "";
                            }


                            if (heart_rate != "" )
                            {
                                // Display the heart rate
                                hr = Double.parseDouble(heart_rate);
                                disp_txt.append("The Heart Rate is: " + heart_rate + "\n");
                                hideProgressDialogWithTitle();
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                }
            });

        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }

    }

    // Function to ask the permissions required by the app
    void configure_permissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        , 10);
            }
            return;
        }
    }


    private BroadcastReceiver bReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {

            // Display the respiratory rate
            String output = intent.getStringExtra("success");
            Log.d("Output", output);
            disp_txt.setText("Respiratory Rate: " + output);
            resp_rate = Double.parseDouble(output);
            hideProgressDialogWithTitle();

        }
    };


    // Intent to open up the Camere and start recording
    public void start_recording_intent()
    {

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,4);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, VIDEO_CAPTURE);

    }



    // Function to hide the processing dialog box
    private void hideProgressDialogWithTitle() {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.dismiss();
    }

    //Function to show the Processing Dialog box
    private void showProgressDialogWithTitle(String substring) {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(substring);
        progressDialog.show();
    }

    protected void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("message"));
    }

    protected void onPause (){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
    }

}