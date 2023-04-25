package com.example.tester;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StatFs;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.*;


public class MainActivity extends AppCompatActivity {
    private EditText ip;
    private EditText port;
    private Button loadDataButton;
    private Button connectButton;
    private Button trainButton;
    private TextView resultText;
    private EditText device_id;


    private static final String TAG = "Flower";

    // Following are the functions of the foregrounding :

    // Post request method :
    private static final String URL = "https://profiling-server.herokuapp.com/log";
    private void makePostRequest(String mainData) {
        // Create a JSON string to send in the request body


        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("user_logs", mainData);
            jsonObject.put("user_device_model", Build.MODEL);
            jsonObject.put("user_android_id", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String json = jsonObject.toString();
        Log.d("myJson", json);

        // Create an OkHttpClient instance
        OkHttpClient client = new OkHttpClient();

        // Create a request object with the POST method and the JSON object in the body
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Request request = new Request.Builder()
                .url(URL)
                .post(requestBody)
                .build();

        // Use the OkHttpClient instance to send the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle the failure
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Handle the response
                final String responseData = response.body().string();
//                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Response: " + responseData, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // CPU FUNCTIONS ::
    private String getCpuUsageInfo(int[] cores) {
        String info = new String();
//        info += " cores: \n";
//        for (int i = 1; i < cores.length; i++) {
//            if (cores[i] < 0)
//                info += "  " + i + ": x\n";
//            else
//                info += "  " + i + ": " + cores[i] + "%\n";
//        }
//        info = "  moy=" + cores[0] + "% \n";
        info = "CPU total: " + CpuInfo.getCpuUsage(cores) + "% \n";
        return info;
    }


    //


    // Code to create & save our sensor_data.txt

    private BufferedWriter bufferedWriter;

    private void openFileWriter() throws FileNotFoundException {
        SimpleDateFormat dateFormat = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            String timestamp = dateFormat.format(new Date());
        }
        String fileName = "sensor_data.txt";
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        dir.mkdirs();
        String path = new File(dir, fileName).getAbsolutePath();
        FileOutputStream fileOutputStream = new FileOutputStream(path);
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
    }

    private void closeFileWriter() throws IOException {
        bufferedWriter.close();
    }

    private void writeCsvData(String data) throws IOException {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String path = new File(dir, "sensor_data.txt").getAbsolutePath();
        bufferedWriter = new BufferedWriter(new FileWriter(path, true));
        bufferedWriter.write(data);
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }



    // Memory_info data :

    private String getRamInfo() {

        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        String ramInfoStr = "\n\nRAM Info:\n";
        ramInfoStr += "Total RAM: " + Formatter.formatFileSize(this, memoryInfo.totalMem) + "\n";
        ramInfoStr += "Available RAM: " + Formatter.formatFileSize(this, memoryInfo.availMem) + "\n";
        ramInfoStr += "Threshold RAM: " + Formatter.formatFileSize(this, memoryInfo.threshold);

        String all_ram_info = "Available RAM: " + Formatter.formatFileSize(this, memoryInfo.availMem) + "\n";

        return all_ram_info;

    }



    private Handler handler = new Handler(Looper.getMainLooper());
    private String info_1 = "";
    private Integer counter = 0;
    private Timer timer = new Timer();


    //  Foreground functions ends


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if the app has been granted permission to ignore battery optimizations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                // If permission has not been granted, open the system settings screen to request permission
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag");

        wakeLock.acquire();

        setContentView(R.layout.activity_main);




// assign the IP and port values to the corresponding EditText views

        // create the LogDataRunnable instance
//        LogDataRunnable logDataRunnable = new LogDataRunnable(this);

        // schedule the runnable to run every second
//        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//        executor.scheduleAtFixedRate(logDataRunnable, 0, 1, TimeUnit.SECONDS);


        // Following code is of foreground :
//        try {
//            openFileWriter();
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }

        int numCores_1 = Runtime.getRuntime().availableProcessors();
        String cpu_cores = "\n\nCPU Cores:\n" + numCores_1;


        // Loop of foreground begins :

        //  LOGGING THE DATA INTO TXT BEGINS
        // LOOP begins


        TimerTask idleChecker = new TimerTask() {
            @Override
            public void run() {
                ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
                ActivityManager.getMyMemoryState(info);

                handler.post(() -> {

                    // getting all data & concatuenating them :

                    if (counter < 5) {

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            info_1 += sdf.format(new Date()); // get the current time
                            info_1 += "\n";
//                            info_1 += "using getCoresUsageGuessFromFreq";
                            info_1 += getCpuUsageInfo(CpuInfo.getCoresUsageGuessFromFreq());
                            info_1 += getRamInfo();
                            counter = counter + 1;
//                            info_1 +="\n";
                        }

                    }
                    else
                    {
//                        try {
//                            writeCsvData(info_1);
////                            makePostRequest(info_1);
//                        } catch (IOException e) {
////                        throw new RuntimeException(e);
//                            System.out.println(e);
//                            throw new RuntimeException(e);
//                        }
//                        Log.d( "post", info_1 );
                        makePostRequest(info_1);
                        info_1 = "";
                        counter = 0;

                    }


                });
            }
        };
        timer.schedule(idleChecker, 0, 1000);


//        try {
//            closeFileWriter();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }


        // Making my main activity in foreground now
        Intent service_intent = new Intent(this, MyForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service_intent);
        }

        foregroundServiceRunning();

    }

    // this function is our local function , not flowers

    // foreground : Service
    public boolean foregroundServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (MyForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}