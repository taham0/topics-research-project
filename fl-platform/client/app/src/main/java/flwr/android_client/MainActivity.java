package flwr.android_client;

import android.app.Activity;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import  flwr.android_client.FlowerServiceGrpc.FlowerServiceStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;


import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Integrating the imports of foreground :
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
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;







public class MainActivity extends AppCompatActivity {
    private EditText ip;
    private EditText port;
    private Button loadDataButton;
    private Button connectButton;
    private Button trainButton;
    private TextView resultText;
    private EditText device_id;
    private ManagedChannel channel;
    public FlowerClient fc;
    private static final String TAG = "Flower";

    // Following are the functions of the foregrounding :


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




    private Handler handler = new Handler(Looper.getMainLooper());



    //  Foreground functions ends





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        resultText = (TextView) findViewById(R.id.grpc_response_text);
        resultText.setMovementMethod(new ScrollingMovementMethod());
        device_id = (EditText) findViewById(R.id.device_id_edit_text);
        ip = (EditText) findViewById(R.id.serverIP);
        port = (EditText) findViewById(R.id.serverPort);
        loadDataButton = (Button) findViewById(R.id.load_data) ;
        connectButton = (Button) findViewById(R.id.connect);
        trainButton = (Button) findViewById(R.id.trainFederated);

        fc = new FlowerClient(this);
        // get the IP and port from the X and Y variables respectively
        String serverIp = "10.130.149.121";
        String serverPort = "8080";
        String dataslice = "1";

// assign the IP and port values to the corresponding EditText views
        ip.setText(serverIp);
        port.setText(String.valueOf(serverPort));
        device_id.setText(dataslice);

        connectButton.performClick();

        // Following code is of foreground :
        try {
            openFileWriter();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }



        /// Getting data from APIs:
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        String ramInfoStr = "\n\nRAM Info:\n";
        ramInfoStr += "Total RAM: " + Formatter.formatFileSize(this, memoryInfo.totalMem) + "\n";
        ramInfoStr += "Available RAM: " + Formatter.formatFileSize(this, memoryInfo.availMem) + "\n";
        ramInfoStr += "Threshold RAM: " + Formatter.formatFileSize(this, memoryInfo.threshold);

        String all_ram_info = "Total RAM: " + Formatter.formatFileSize(this, memoryInfo.totalMem) + " : " + " Available RAM: " + Formatter.formatFileSize(this, memoryInfo.availMem) + " Threshold RAM: " + Formatter.formatFileSize(this, memoryInfo.threshold);



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

        String all_cache_use = " ALL CACHE : " + " Total Cache Size: " + Formatter.formatFileSize(this, totalCacheSize) + " Used Cache Size: " + Formatter.formatFileSize(this, usedCacheSize) + " Free Cache Size: " + Formatter.formatFileSize(this, freeCacheSize);

        int numCores_1 = Runtime.getRuntime().availableProcessors();
        String cpu_cores = "\n\nCPU Cores:\n" + numCores_1;

        // Loop of foreground begins :

        //  LOGGING THE DATA INTO TXT BEGINS
        // LOOP begins

        Timer timer = new Timer();

        TimerTask idleChecker = new TimerTask()  {
            @Override
            public void run() {
                ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
                ActivityManager.getMyMemoryState(info);

                handler.post(() -> {

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

    // this function is our local function , not flowers

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



    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    public void setResultText(String text) {
        SimpleDateFormat dateFormat = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.GERMANY);
        }
        String time = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            time = dateFormat.format(new Date());
        }
        resultText.append("\n" + time + "   " + text);
    }

    public void loadData(View view){
        if (TextUtils.isEmpty(device_id.getText().toString())) {
            Toast.makeText(this, "Please enter a client partition ID between 1 and 10 (inclusive)", Toast.LENGTH_LONG).show();
        }
        else if (Integer.parseInt(device_id.getText().toString()) > 10 ||  Integer.parseInt(device_id.getText().toString()) < 1)
        {
            Toast.makeText(this, "Please enter a client partition ID between 1 and 10 (inclusive)", Toast.LENGTH_LONG).show();
        }
        else{
            hideKeyboard(this);
            setResultText("Loading the local training dataset in memory. It will take several seconds.");
            loadDataButton.setEnabled(false);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                private String result;
                @Override
                public void run() {
                    try {
                        fc.loadData(Integer.parseInt(device_id.getText().toString()));
                        result =  "Training dataset is loaded in memory.";
                    } catch (Exception e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        pw.flush();
                        result =  "Training dataset is loaded in memory.";
                    }
                    handler.post(() -> {
                        setResultText(result);
                        trainButton.setEnabled(true);
                        trainButton.performClick();
                        connectButton.setEnabled(true);
                    });
                }
            });
        }
    }

    public void connect(View view) {
        String host = ip.getText().toString();
        String portStr = port.getText().toString();
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(portStr) || !Patterns.IP_ADDRESS.matcher(host).matches()) {
            Toast.makeText(this, "Please enter the correct IP and port of the FL server", Toast.LENGTH_LONG).show();
        }
        else {
            int port = TextUtils.isEmpty(portStr) ? 0 : Integer.parseInt(portStr);
            channel = ManagedChannelBuilder.forAddress(host, port).maxInboundMessageSize(10 * 1024 * 1024).usePlaintext().build();
            hideKeyboard(this);
            trainButton.setEnabled(true);
            loadDataButton.setEnabled(true);
            loadDataButton.performClick();
            connectButton.setEnabled(true);
            setResultText("Channel object created. Ready to train!");
        }
    }

    public void runGrpc(View view){
        MainActivity activity = this;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            private String result;
            @Override
            public void run() {
                try {
                    (new FlowerServiceRunnable()).run(FlowerServiceGrpc.newStub(channel), activity);
                    result =  "Connection to the FL server successful \n";
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    pw.flush();
                    result = "Failed to connect to the FL server \n" + sw;
                }
                handler.post(() -> {
                    setResultText(result);
                    trainButton.setEnabled(true);
                });
            }
        });
    }


    private static class FlowerServiceRunnable{
        protected Throwable failed;
        private StreamObserver<ClientMessage> requestObserver;

        public void run(FlowerServiceStub asyncStub, MainActivity activity) {
             join(asyncStub, activity);
        }

        private void join(FlowerServiceStub asyncStub, MainActivity activity)
                throws RuntimeException {

            final CountDownLatch finishLatch = new CountDownLatch(1);
            requestObserver = asyncStub.join(
                            new StreamObserver<ServerMessage>() {
                                @Override
                                public void onNext(ServerMessage msg) {
                                    handleMessage(msg, activity);
                                }

                                @Override
                                public void onError(Throwable t) {
                                    t.printStackTrace();
                                    failed = t;
                                    finishLatch.countDown();
                                    Log.e(TAG, t.getMessage());
                                }

                                @Override
                                public void onCompleted() {
                                    finishLatch.countDown();
                                    Log.e(TAG, "Done");
                                }
                            });
        }

        private void handleMessage(ServerMessage message, MainActivity activity) {

            try {
                ByteBuffer[] weights;
                ClientMessage c = null;

                if (message.hasGetParametersIns()) {
                    Log.e(TAG, "Handling GetParameters");
                    activity.setResultText("Handling GetParameters message from the server.");

                    weights = activity.fc.getWeights();
                    c = weightsAsProto(weights);
                } else if (message.hasFitIns()) {
                    Log.e(TAG, "Handling FitIns");
                    activity.setResultText("Handling Fit request from the server.");

                    List<ByteString> layers = message.getFitIns().getParameters().getTensorsList();

                    Scalar epoch_config = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        epoch_config = message.getFitIns().getConfigMap().getOrDefault("local_epochs", Scalar.newBuilder().setSint64(1).build());
                    }

                    assert epoch_config != null;
                    int local_epochs = (int) epoch_config.getSint64();

                    // Our model has 10 layers
                    ByteBuffer[] newWeights = new ByteBuffer[10] ;
                    for (int i = 0; i < 10; i++) {
                        newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                    }

                    Pair<ByteBuffer[], Integer> outputs = activity.fc.fit(newWeights, local_epochs);
                    c = fitResAsProto(outputs.first, outputs.second);
                } else if (message.hasEvaluateIns()) {
                    Log.e(TAG, "Handling EvaluateIns");
                    activity.setResultText("Handling Evaluate request from the server");

                    List<ByteString> layers = message.getEvaluateIns().getParameters().getTensorsList();

                    // Our model has 10 layers
                    ByteBuffer[] newWeights = new ByteBuffer[10] ;
                    for (int i = 0; i < 10; i++) {
                        newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                    }
                    Pair<Pair<Float, Float>, Integer> inference = activity.fc.evaluate(newWeights);

                    float loss = inference.first.first;
                    float accuracy = inference.first.second;
                    activity.setResultText("Test Accuracy after this round = " + accuracy);
                    int test_size = inference.second;
                    c = evaluateResAsProto(loss, test_size);
                }
                requestObserver.onNext(c);
                activity.setResultText("Response sent to the server");
            }
            catch (Exception e){
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private static ClientMessage weightsAsProto(ByteBuffer[] weights){
        List<ByteString> layers = new ArrayList<>();
        for (ByteBuffer weight : weights) {
            layers.add(ByteString.copyFrom(weight));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.GetParametersRes res = ClientMessage.GetParametersRes.newBuilder().setParameters(p).build();
        return ClientMessage.newBuilder().setGetParametersRes(res).build();
    }

    private static ClientMessage fitResAsProto(ByteBuffer[] weights, int training_size){
        List<ByteString> layers = new ArrayList<>();
        for (ByteBuffer weight : weights) {
            layers.add(ByteString.copyFrom(weight));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.FitRes res = ClientMessage.FitRes.newBuilder().setParameters(p).setNumExamples(training_size).build();
        return ClientMessage.newBuilder().setFitRes(res).build();
    }

    private static ClientMessage evaluateResAsProto(float accuracy, int testing_size){
        ClientMessage.EvaluateRes res = ClientMessage.EvaluateRes.newBuilder().setLoss(accuracy).setNumExamples(testing_size).build();
        return ClientMessage.newBuilder().setEvaluateRes(res).build();
    }
}
