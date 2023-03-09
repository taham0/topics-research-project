package com.example.tester;

import static java.security.AccessController.getContext;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.TransportInfo;
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
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
//import android.os.Processor;





import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Flow;

public class MainActivity extends AppCompatActivity {




    public double getCPUUsage(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int pid = android.os.Process.myPid();
        android.os.Debug.MemoryInfo[] memoryInfo = activityManager.getProcessMemoryInfo(new int[]{pid});
        long cpuTime = Debug.threadCpuTimeNanos();
        long elapsedTime = System.nanoTime();
        double cpuUsage = 100 * (double) cpuTime / (double) elapsedTime;
        return cpuUsage;
    }


    private long lastCpuTime = 0;
    private long lastUpTime = 0;

    public float getCpuUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" +");

            long currUpTime = SystemClock.uptimeMillis();
            long currCpuTime = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                    Long.parseLong(toks[4]) + Long.parseLong(toks[5]) + Long.parseLong(toks[6]) + Long.parseLong(toks[7]);

            long upTimeDelta = currUpTime - lastUpTime;
            long cpuDelta = currCpuTime - lastCpuTime;

            float cpuUsage = (cpuDelta / (float) upTimeDelta) * 100;

            lastUpTime = currUpTime;
            lastCpuTime = currCpuTime;

            return cpuUsage;

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

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

    // Network Info
    private void getNetworkInfo() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities != null) {
                String networkInfoStr = "\n\nNetwork Info:\n";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    networkInfoStr += "Network Type: " + capabilities.getTransportInfo().toString() + "\n";
                }
                networkInfoStr += "Download Bandwidth: " + capabilities.getLinkDownstreamBandwidthKbps() / 1000 + " Mbps\n";
                networkInfoStr += "Upload Bandwidth: " + capabilities.getLinkUpstreamBandwidthKbps() / 1000 + " Mbps";
                textView.append(networkInfoStr);
            }
        }
    }



    private TextView textView;
    private TextView textView_1 ;

    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isIdle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textVIew);
        textView_1 = findViewById(R.id.textView_1);




        // Battery Level
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100 / (float) scale;

        String batteryLevelStr = "\n\nBattery Level: " + batteryPct + "%";
        textView.append(batteryLevelStr);
        // RAM Info
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        String ramInfoStr = "\n\nRAM Info:\n";
        ramInfoStr += "Total RAM: " + Formatter.formatFileSize(this, memoryInfo.totalMem) + "\n";
        ramInfoStr += "Available RAM: " + Formatter.formatFileSize(this, memoryInfo.availMem) + "\n";
        ramInfoStr += "Threshold RAM: " + Formatter.formatFileSize(this, memoryInfo.threshold);
        textView.append(ramInfoStr);

        // Storage Info
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        long blockSize = statFs.getBlockSizeLong();
        long totalSize = statFs.getBlockCountLong() * blockSize;
        long availableSize = statFs.getAvailableBlocksLong() * blockSize;
        String storageInfoStr = "\n\nStorage Info:\n";
        storageInfoStr += "Total Storage: " + Formatter.formatFileSize(this, totalSize) + "\n";
        storageInfoStr += "Available Storage: " + Formatter.formatFileSize(this, availableSize);
        textView.append(storageInfoStr);

        // Wi-Fi Info
        getWiFiInfo();

        // Cache Usage
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
        textView.append(cacheInfoStr);



        Timer timer = new Timer();
        Context bla = this;
//        textView_1 = findViewById(R.id.textView_1);
        TimerTask idleChecker = new TimerTask() {
            @Override
            public void run() {
                ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
                ActivityManager.getMyMemoryState(info);
                isIdle = info.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
                handler.post(() -> {
                    String idleInfo = "\n\nIdle: " + isIdle;

                    textView_1.setText(idleInfo);
                    // Add the new TextView to your layout if needed


                });
            }
        };








//        String cpu_storage = "\n\nCPU Usage:\n";
//        int pid = android.os.Process.myPid();
//        ActivityManager activityManager1 = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        Debug.MemoryInfo[] memoryInfoArray = activityManager1.getProcessMemoryInfo(new int[]{pid});
//        long cpuUsage = memoryInfoArray[0].getTotalCpuTime();
//        cpu_storage += cpuUsage + " microseconds\n";
//        textView.append(cpu_storage);
//        String cpu_storage = "\n\nCPU Usage:\n";
//        int pid = android.os.Process.myPid();
//        ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(false);
//        processCpuTracker.init();
//        processCpuTracker.update();
//        long cpuUsage = processCpuTracker.getCpuTimeForPid(pid);
//        cpu_storage += cpuUsage + " microseconds\n";
//        textView.append(cpu_storage);

        // CPU consumption for our app
        String cpu_storage = "\n\nCPU Usage:\n";
        cpu_storage += getCPUUsage(this)+ " microseconds\n";
        textView.append(cpu_storage);

        // overal cpu cores :

        int numCores = Runtime.getRuntime().availableProcessors();
        String cpu_cores = "\n\nCPU Cores:\n" + numCores;
        textView.append(cpu_cores);



// First, try to get CPU ABI using Build.CPU_ABI property
        String cpuABI;

// Get a list of all supported ABIs on the device
        String[] supportedABIS = Build.SUPPORTED_ABIS;

// Use the first supported ABI as the primary CPU ABI
        if (supportedABIS != null && supportedABIS.length > 0) {
            cpuABI = supportedABIS[0];
        } else {
            // If the supported ABIs list is not available or empty, fall back to Build.CPU_ABI
            cpuABI = Build.CPU_ABI;
        }

        textView.append("CPU Info"+ "CPU ABI: " + cpuABI);

        // Extracting the number of running tasks and their lists :

        ActivityManager activityManager_2 = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager_2.getRunningAppProcesses();
        int numOfRunningTasks = runningProcesses.size();
        textView.append("number of running tasks: "  + numOfRunningTasks);
//        textView.append("name of running tasks: "  + runningProcesses.toArray());

//        List<String> processNames = new ArrayList<String>();
//        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
//            String processName = processInfo.processName;
//            processNames.add(processName);
//        }
//        StringBuilder sb = new StringBuilder();
//        for (String processName : processNames) {
//            sb.append(processName + "\n");
//        }
//        String allProcessNames = sb.toString();
//        textView.append(allProcessNames);


        // Printing the lists of all the processes
        List<String> processNames = new ArrayList<String>();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {


            int pid = processInfo.pid;
            Debug.MemoryInfo[] memoryInfo_1 = activityManager.getProcessMemoryInfo(new int[]{pid});
            int cpuUsage = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                cpuUsage = memoryInfo_1[0].getTotalPss();
            }
            textView.append("CPU usage of process " + processInfo.processName + " is " + cpuUsage + "kBs");

//            processNames.add(processName);
        }
//        StringBuilder sb = new StringBuilder();
//        for (String processName : processNames) {
//            sb.append(processName + "\n");
//        }
//        String allProcessNames = sb.toString();
//        textView.append(allProcessNames);

        // Check if device is idle

        timer.schedule(idleChecker, 0, 1000);

    }








//        int numCores = Processor.getCoreCount();
//        int numThreads = Processor.getThreadCount();
//        String cpuType = Processor.getCpuInfo().get(0).getName();
//        double[] cpuUsages = Processor.getCpuUsagePercent();
//
//        Log.d("CPU Info", "Number of Cores: " + numCores);
//        Log.d("CPU Info", "Number of Threads: " + numThreads);
//        Log.d("CPU Info", "CPU Type: " + cpuType);
//
//        for (int i = 0; i < cpuUsages.length; i++) {
//            Log.d("CPU Info", "CPU Usage for Core " + i + ": " + cpuUsages[i] + "%");
//        }













//

//
//        // Schedule CPU Usage Timer Task
//        scheduleCPUTimer();



}
