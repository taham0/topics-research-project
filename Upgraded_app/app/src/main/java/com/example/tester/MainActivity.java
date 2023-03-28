package com.example.tester;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.text.format.Formatter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;





public class MainActivity extends AppCompatActivity {

    // CPU FUNCTIONS ::

    private String getCpuUsageInfo(int[] cores) {
        String info = new String();
        info += " cores: \n";
        for (int i = 1; i < cores.length; i++) {
            if (cores[i] < 0)
                info += "  " + i + ": x\n";
            else
                info += "  " + i + ": " + cores[i] + "%\n";
        }
        info += "  moy=" + cores[0] + "% \n";
        info += "CPU total: " + CpuInfo.getCpuUsage(cores) + "%";
        return info;
    }


    //



    // Code to create & save our sensor_data.txt

    private BufferedWriter bufferedWriter;

    private void openFileWriter() throws FileNotFoundException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
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


    //for multi core value




    // Wi-Fi Info
    private void getWiFiInfo() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int signalStrength = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 10);
        String wifiInfoStr = "\n\nWi-Fi Info:\n";
        wifiInfoStr += "SSID: " + wifiInfo.getSSID() + "\n";
        wifiInfoStr += "BSSID: " + wifiInfo.getBSSID() + "\n";
        wifiInfoStr += "Signal Strength: " + signalStrength + "/10\n";
        wifiInfoStr += "Link Speed: " + wifiInfo.getLinkSpeed() + " Mbps";
        textView.append(wifiInfoStr);
    }

    // Memory_info data :

    private String getRamInfo(){

        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        String ramInfoStr = "\n\nRAM Info:\n";
        ramInfoStr += "Total RAM: " + Formatter.formatFileSize(this, memoryInfo.totalMem) + "\n";
        ramInfoStr += "Available RAM: " + Formatter.formatFileSize(this, memoryInfo.availMem) + "\n";
        ramInfoStr += "Threshold RAM: " + Formatter.formatFileSize(this, memoryInfo.threshold);

        String all_ram_info = "Total RAM: " + Formatter.formatFileSize(this, memoryInfo.totalMem) + " : " + " Available RAM: " + Formatter.formatFileSize(this, memoryInfo.availMem) + " Threshold RAM: " + Formatter.formatFileSize(this, memoryInfo.threshold);

        return all_ram_info;

    }


    // DEvice's total storage :

    private String getStorageInfo(){

        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        long blockSize = statFs.getBlockSizeLong();
        long totalSize = statFs.getBlockCountLong() * blockSize;
        long availableSize = statFs.getAvailableBlocksLong() * blockSize;
        String storageInfoStr = "\n\nStorage Info:\n";
        storageInfoStr += "Total Storage: " + Formatter.formatFileSize(this, totalSize) + "\n";
        storageInfoStr += "Available Storage: " + Formatter.formatFileSize(this, availableSize);

        String all_storage_info = " ALL Storage info : " + " Total Storage: " + Formatter.formatFileSize(this, totalSize) + " Available Storage: " + Formatter.formatFileSize(this, availableSize);
        return all_storage_info;

    }

    // Device's Catche memory info :

    private String getCatchInfo(){


        ActivityManager activityManager2 = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        int totalCacheSize = activityManager2.getLargeMemoryClass() * 1024 * 1024;
        int freeCacheSize = activityManager2.getMemoryClass() * 1024 * 1024;
        int usedCacheSize = totalCacheSize - freeCacheSize;
        int cacheUsagePercentage = (int) ((double) usedCacheSize / totalCacheSize * 100);
        String cacheInfoStr = "\n\nCache Usage:\n";
        cacheInfoStr += "Total Cache Size: " + Formatter.formatFileSize(this, totalCacheSize) + "\n";
        cacheInfoStr += "Used Cache Size: " + Formatter.formatFileSize(this, usedCacheSize) + "\n";
        cacheInfoStr += "Free Cache Size: " + Formatter.formatFileSize(this, freeCacheSize) + "\n";
        cacheInfoStr += "Cache Usage: " + cacheUsagePercentage + "%";


        String all_cache_use = " ALL CACHE : " + " Total Cache Size: " + Formatter.formatFileSize(this, totalCacheSize) + " Used Cache Size: " + Formatter.formatFileSize(this, usedCacheSize) + " Free Cache Size: " + Formatter.formatFileSize(this, freeCacheSize);
        return all_cache_use ;

    }

    // Device's Deep Memory info :

    private String getDeepInfo(){

        Debug.MemoryInfo debugMemoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(debugMemoryInfo);

        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());

        long external_bytesAvailable = stat.getAvailableBytes();
        long private_dirty_mem = debugMemoryInfo.dalvikPrivateDirty;
        long swapCached = debugMemoryInfo.dalvikPss;
        long swapTotal = debugMemoryInfo.getTotalSwappablePss();
        long swapFree = debugMemoryInfo.getTotalSwappablePss() - debugMemoryInfo.getTotalPss();
        long shared_dirty_mem = debugMemoryInfo.dalvikSharedDirty;
        long write_back = debugMemoryInfo.getTotalSwappablePss();
        long active = debugMemoryInfo.getTotalPss();
        long inactive = debugMemoryInfo.getTotalPrivateDirty();

        String other_info = " External bytes available info : " + external_bytesAvailable + " private dirty memory : " + private_dirty_mem + " Swapper cache : "
                +  swapCached +  " swapTotal : " + swapTotal + " swapfree : " + swapFree + " shared_dirty_memory " + shared_dirty_mem + " write back bytes : " + write_back +
                " active proceses : " + active + " inactive : " + inactive;

        return other_info;



    }





    private TextView textView;
    private TextView textView_1 ;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isIdle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            openFileWriter();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        textView = findViewById(R.id.textVIew);
        textView_1 = findViewById(R.id.textView_1);


        // Battery Level
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100 / (float) scale;

        String batteryLevelStr = "\n\nBattery Level: " + batteryPct + "%";
        String batt_lv = "" + batteryPct;
        textView.append(batteryLevelStr);
        // RAM Info
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        String ramInfoStr = "\n\nRAM Info:\n";
        ramInfoStr += "Total RAM: " + Formatter.formatFileSize(this, memoryInfo.totalMem) + "\n";
        ramInfoStr += "Available RAM: " + Formatter.formatFileSize(this, memoryInfo.availMem) + "\n";
        ramInfoStr += "Threshold RAM: " + Formatter.formatFileSize(this, memoryInfo.threshold);

        String all_ram_info = "Total RAM: " + Formatter.formatFileSize(this, memoryInfo.totalMem) + " : " + " Available RAM: " + Formatter.formatFileSize(this, memoryInfo.availMem) + " Threshold RAM: " + Formatter.formatFileSize(this, memoryInfo.threshold);
        textView.append(ramInfoStr);

        // Storage Info
        textView.append(getStorageInfo());

        // Wi-Fi Info
        getWiFiInfo();

        // Cache Usage
        ActivityManager activityManager2 = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memoryInfo);
        int totalCacheSize = activityManager2.getLargeMemoryClass() * 1024 * 1024;
        int freeCacheSize = activityManager2.getMemoryClass() * 1024 * 1024;
        int usedCacheSize = totalCacheSize - freeCacheSize;
        int cacheUsagePercentage = (int) ((double) usedCacheSize / totalCacheSize * 100);
        String cacheInfoStr = "\n\nCache Usage:\n";
        cacheInfoStr += "Total Cache Size: " + Formatter.formatFileSize(this, totalCacheSize) + "\n";
        cacheInfoStr += "Used Cache Size: " + Formatter.formatFileSize(this, usedCacheSize) + "\n";
        cacheInfoStr += "Free Cache Size: " + Formatter.formatFileSize(this, freeCacheSize) + "\n";
        cacheInfoStr += "Cache Usage: " + cacheUsagePercentage + "%";
        textView.append(cacheInfoStr);

        String all_cache_use = " ALL CACHE : " + " Total Cache Size: " + Formatter.formatFileSize(this, totalCacheSize) + " Used Cache Size: " + Formatter.formatFileSize(this, usedCacheSize) + " Free Cache Size: " + Formatter.formatFileSize(this, freeCacheSize);

        // overal cpu cores :

        int numCores_1 = Runtime.getRuntime().availableProcessors();
        String cpu_cores = "\n\nCPU Cores:\n" + numCores_1;
        textView.append(cpu_cores);



        //  LOGGING THE DATA INTO TXT BEGINS
    // LOOP begins

        Timer timer = new Timer();

        TimerTask idleChecker = new TimerTask() {
            @Override
            public void run() {
                ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
                ActivityManager.getMyMemoryState(info);
                isIdle = info.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
                handler.post(() -> {


                    String idleInfo = "\n\nIdle: " + isIdle;
                    textView_1.setText(idleInfo);

                    // getting all data & concatuenating them :

                    String info_1 = new String();
                    info_1 += "using getCoresUsageGuessFromFreq";
                    info_1 += getCpuUsageInfo(CpuInfo.getCoresUsageGuessFromFreq());

                    info_1 = getRamInfo() + getStorageInfo() + getCatchInfo() + getDeepInfo() + info_1;

                    try {
                        writeCsvData(info_1 );

                    } catch (IOException e) {
//                        throw new RuntimeException(e);
                        System.out.println(e);
                        throw new RuntimeException(e);
                    }

                });
            }
        };
        timer.schedule(idleChecker, 0, 1000);



        try {
            closeFileWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // Making my main activity in foreground now
        Intent service_intent = new Intent(this, MyForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service_intent);
        }

        foregroundServiceRunning();

    }


    // foreground : Service
    public boolean foregroundServiceRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if(MyForegroundService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



}
