package com.example.tester;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;



public class MainActivity extends AppCompatActivity {



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


    public double getCPUUsage(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int pid = android.os.Process.myPid();
        android.os.Debug.MemoryInfo[] memoryInfo = activityManager.getProcessMemoryInfo(new int[]{pid});
        long cpuTime = Debug.threadCpuTimeNanos();
        long elapsedTime = System.nanoTime();
        double cpuUsage = 100 * (double) cpuTime / (double) elapsedTime;
        return cpuUsage;
    }


    //for multi core value
    private float readCore(int i)
    {
        /*
         * how to calculate multicore
         * this function reads the bytes from a logging file in the android system (/proc/stat for cpu values)
         * then puts the line into a string
         * then spilts up each individual part into an array
         * then(since he know which part represents what) we are able to determine each cpu total and work
         * then combine it together to get a single float for overall cpu usage
         */
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            //skip to the line we need
            for(int ii = 0; ii < i + 1; ++ii)
            {
                reader.readLine();
            }
            String load = reader.readLine();

            //cores will eventually go offline, and if it does, then it is at 0% because it is not being
            //used. so we need to do check if the line we got contains cpu, if not, then this core = 0
            if(load.contains("cpu"))
            {
                String[] toks = load.split(" ");

                //we are recording the work being used by the user and system(work) and the total info
                //of cpu stuff (total)
                //https://stackoverflow.com/questions/3017162/how-to-get-total-cpu-usage-in-linux-c/3017438#3017438

                long work1 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
                long total1 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                        Long.parseLong(toks[4]) + Long.parseLong(toks[5])
                        + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

                try
                {
                    //short sleep time = less accurate. But android devices typically don't have more than
                    //4 cores, and I'n my app, I run this all in a second. So, I need it a bit shorter
                    Thread.sleep(200);
                }
                catch (Exception e) {}

                reader.seek(0);
                //skip to the line we need
                for(int ii = 0; ii < i + 1; ++ii)
                {
                    reader.readLine();
                }
                load = reader.readLine();
                //cores will eventually go offline, and if it does, then it is at 0% because it is not being
                //used. so we need to do check if the line we got contains cpu, if not, then this core = 0%
                if(load.contains("cpu"))
                {
                    reader.close();
                    toks = load.split(" ");

                    long work2 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
                    long total2 = Long.parseLong(toks[1])+ Long.parseLong(toks[2]) + Long.parseLong(toks[3]) +
                            Long.parseLong(toks[4]) + Long.parseLong(toks[5])
                            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);



                    //here we find the change in user work and total info, and divide by one another to get our total
                    //seems to be accurate need to test on quad core
                    //https://stackoverflow.com/questions/3017162/how-to-get-total-cpu-usage-in-linux-c/3017438#3017438

                    return (float)(work2 - work1) / ((total2 - total1));
                }
                else
                {
                    reader.close();
                    return 0;
                }

            }
            else
            {
                reader.close();
                return 0;
            }

        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        return 0;
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
        StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        long blockSize = statFs.getBlockSizeLong();
        long totalSize = statFs.getBlockCountLong() * blockSize;
        long availableSize = statFs.getAvailableBlocksLong() * blockSize;
        String storageInfoStr = "\n\nStorage Info:\n";
        storageInfoStr += "Total Storage: " + Formatter.formatFileSize(this, totalSize) + "\n";
        storageInfoStr += "Available Storage: " + Formatter.formatFileSize(this, availableSize);

        String all_storage_info = " ALL Storage info : " + " Total Storage: " + Formatter.formatFileSize(this, totalSize) + " Available Storage: " + Formatter.formatFileSize(this, availableSize);
        textView.append(storageInfoStr);

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








        // Get the CPU usage for the current process
        Debug.MemoryInfo memInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memInfo);
//        int totalCpuTime = (int) Process.getElapsedCpuTime();

// Calculate the CPU usage for each core

//        long bufferCacheSize = memoryInfo.bufferSize;



        Timer timer = new Timer();
        Context bla = this;

//        TimerTask idleChecker = new TimerTask() {
//            @Override
//            public void run() {
//                ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
//                ActivityManager.getMyMemoryState(info);
//                isIdle = info.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
//                handler.post(() -> {
//
//
//                    String idleInfo = "\n\nIdle: " + isIdle;
//
//                    textView_1.setText(idleInfo);
//                    try {
//                        writeCsvData(batt_lv);
//                    } catch (IOException e) {
////                        throw new RuntimeException(e);
//                        System.out.println(e);
//                        throw new RuntimeException(e);
//                    }
//
//                });
//            }
//        };




        int numCores = Runtime.getRuntime().availableProcessors();
        float[] coreValues = new float[10];
        String all_cpu_data = " ALL CPU DATA : ";
        for(byte i = 0; i < numCores; i++)
        {
            coreValues[i] = readCore(i);
            textView.append( " core " + i + " : "+ coreValues[i]);
            all_cpu_data += " core " + i + " : "+ coreValues[i];

        }
//        textView.append(result_1);



        // CPU consumption for our app
        String cpu_storage = "\n\nCPU Usage:\n";
        cpu_storage += getCPUUsage(this) + " microseconds\n";
        textView.append(cpu_storage);
        all_cpu_data += " CPU usage : " + cpu_storage;


        // overal cpu cores :

        int numCores_1 = Runtime.getRuntime().availableProcessors();
        String cpu_cores = "\n\nCPU Cores:\n" + numCores_1;
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

        textView.append("CPU Info" + "CPU ABI: " + cpuABI);

        // Extracting the number of running tasks and their lists :

        ActivityManager activityManager_2 = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager_2.getRunningAppProcesses();
        int numOfRunningTasks = runningProcesses.size();
        textView.append("number of running tasks: " + numOfRunningTasks);
//


        // Printing the lists of all the processes
        List<String> processNames = new ArrayList<String>();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {


            int pid = processInfo.pid;
            Debug.MemoryInfo[] memoryInfo_1 = activityManager.getProcessMemoryInfo(new int[]{pid});
            int cpuUsage = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cpuUsage = memoryInfo_1[0].getTotalPss();
            }
            textView.append("CPU usage of process " + processInfo.processName + " is " + cpuUsage + "kBs");

//            processNames.add(processName);
        }
//




        String final_str = "battery % = " + batt_lv + "   " + all_ram_info + "    "  + all_storage_info + "    " + all_cache_use + "    " + all_cpu_data;

        TimerTask idleChecker = new TimerTask() {
            @Override
            public void run() {
                ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
                ActivityManager.getMyMemoryState(info);
                isIdle = info.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
                handler.post(() -> {


                    String idleInfo = "\n\nIdle: " + isIdle;

                    textView_1.setText(idleInfo);
                    try {
                        writeCsvData(final_str);
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


    }



}
