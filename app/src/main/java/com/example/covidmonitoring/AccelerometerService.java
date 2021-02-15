package com.example.covidmonitoring;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccelerometerService extends Service implements SensorEventListener {
    @Nullable
    private SensorManager accelerometermanage;
    private Sensor sense_accelerometer;
    Algorithms algos = new Algorithms();
    double accelerometervalueX[]= new double[1280];
    double accelerometervalueY[]= new double[1280];
    double accelerometervalueZ[]= new double[1280];
    int index = 0;

    // Start the service
    @Override
    public void onCreate()
    {
        accelerometermanage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sense_accelerometer = accelerometermanage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometermanage.registerListener(this, sense_accelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //Toast.makeText(this,"Inside Sensor Class", Toast.LENGTH_SHORT).show();
        Sensor my_sensor = sensorEvent.sensor;
        if (my_sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            index++;
            accelerometervalueX[index] = sensorEvent.values[0];
            accelerometervalueY[index] = sensorEvent.values[1];
            accelerometervalueZ[index] = sensorEvent.values[2];

           // Toast.makeText(this, "Inside Sensor Class " + Float.toString(sensorEvent.values[0]), Toast.LENGTH_SHORT).show();
            if (index>=1279) {
                index = 0;
                Toast.makeText(this, "Started to File", Toast.LENGTH_LONG).show();
                accelerometermanage.unregisterListener(this);
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                helper();
            }
        }
    }

    private void helper() {
        List<Double> values_x = new ArrayList<Double>();
        List<Double> values_y = new ArrayList<Double>();
        List<Double> values_z = new ArrayList<Double>();

        for (int i=0; i< accelerometervalueX.length; i++)
        {
            Double val = accelerometervalueX[i];
            values_x.add(val);
        }
        for (int i=0; i< accelerometervalueY.length; i++)
        {
            Double val = accelerometervalueY[i];
            values_y.add(val);
        }
        for (int i=0; i< accelerometervalueZ.length; i++)
        {
            Double val = accelerometervalueZ[i];
            values_z.add(val);
        }



        int mov_period = 50;

        // Calculating the moving average and peak detection
        //List<Double> values_x = hashMap.get("x");
        List<Double> avg_data_x = algos.calc_mov_avg(mov_period, values_x);
        int peak_counts_X = algos.count_zero_crossings_thres(avg_data_x);

        //List<Double> values_y = hashMap.get("y");
        List<Double> avg_data_y = algos.calc_mov_avg(mov_period, values_y);
        int peak_counts_Y = algos.count_zero_crossings_thres(avg_data_y);

        //List<Double> values_z = hashMap.get("z");
        List<Double> avg_data_z = algos.calc_mov_avg(mov_period, values_z);
        int peak_counts_Z = algos.count_zero_crossings_thres(avg_data_z);


        //String s = " " + peak_counts_X/2 + " " + peak_counts_Y/2 + " " + peak_counts_Z/2;
        String resp_rate_val = ""+peak_counts_Y/2;
        // Sending back the received value to the main activity
        sendBroadcast(resp_rate_val);
        Toast.makeText(this, "Ans is " + resp_rate_val, Toast.LENGTH_LONG).show();
        //Log.d(TAG, resp_rate_val);

        //sendBroadcast("Done writing");

    }

    private void sendBroadcast(String str) {
        Intent intent = new Intent ("message"); //put the same message as in the filter you used in the activity when registering the receiver
        intent.putExtra("success", str);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
