package com.example.tester;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MyBackgroundService extends Service {
    private Handler mHandler;
    private final int INTERVAL = 1000; // 1 second
    private BufferedWriter mWriter;
    // variables to be written to the log file
    private String variable1;
    private String variable2;
    private String variable3;

    public MyBackgroundService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        try {
            mWriter = new BufferedWriter(new FileWriter(new File(getFilesDir(), "log.txt"), true));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mHandler.postDelayed(mRunnable, INTERVAL);
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            // read values of your variables and write them to log file
            try {

                mWriter.write("Variable 1: " + variable1 + "\n");

                mWriter.write("Variable 2: " + variable2 + "\n");

                mWriter.write("Variable 3: " + variable3 + "\n");
                mWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mHandler.postDelayed(this, INTERVAL);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        variable1 = intent.getStringExtra("variable1");
        variable2 = intent.getStringExtra("variable2");
        variable3 = intent.getStringExtra("variable3");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
        try {
            mWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
