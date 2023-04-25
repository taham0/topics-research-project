package flwr.android_client;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import  flwr.android_client.FlowerServiceGrpc.FlowerServiceStub;
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
import java.util.HashMap;

import java.util.Map;


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
    public static String device_model;
    public static String device_serial;
    public static String android_id;
    public static String start_time;
    public static String end_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Acquire a WakeLock to allow communication when the screen is off

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
        resultText = (TextView) findViewById(R.id.grpc_response_text);
        resultText.setMovementMethod(new ScrollingMovementMethod());
        device_id = (EditText) findViewById(R.id.device_id_edit_text);
        ip = (EditText) findViewById(R.id.serverIP);
        port = (EditText) findViewById(R.id.serverPort);
        loadDataButton = (Button) findViewById(R.id.load_data) ;
        connectButton = (Button) findViewById(R.id.connect);
        trainButton = (Button) findViewById(R.id.trainFederated);

        device_model = Build.MODEL;
        device_serial = Build.SERIAL;
        android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        // code to make the app automated :
        fc = new FlowerClient(this);


        String serverIp = "192.168.18.91";
        String serverPort = "8080";
        String dataslice = "1";

        // assigning the IP and port values to the corresponding EditText views
        ip.setText(serverIp);
        port.setText(String.valueOf(serverPort));
        device_id.setText(dataslice);


//        connectButton.performClick();
        connectButton.setEnabled(true);
        connectButton.performClick();



        // Code to start the foregrounding process :
        Intent serviceIntent = new Intent(this, MyForegroundService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }
        foregroundServiceRunning();

    }


    public boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
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
                    trainButton.setEnabled(false);
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
                    SimpleDateFormat sdf = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
                    }

                   // Get the current date and time
                    Date currentDate = new Date();

                   // Format the date and time using the SimpleDateFormat object
                   // String formattedDate = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        start_time = sdf.format(currentDate);
                    }
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
                    currentDate = new Date();
                   // Format the date and time using the SimpleDateFormat object
                   // String formattedDate = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        end_time = sdf.format(currentDate);
                    }
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
        Map<String, Scalar> metrics = new HashMap<>();
        metrics.put("device_model", Scalar.newBuilder().setString(device_model).build());
        metrics.put("android_id", Scalar.newBuilder().setString(android_id).build());
        metrics.put("start_time", Scalar.newBuilder().setString(start_time).build());
        metrics.put("end_time", Scalar.newBuilder().setString(end_time).build());
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.FitRes res = ClientMessage.FitRes.newBuilder().setParameters(p).setNumExamples(training_size).putAllMetrics(metrics).build();
        return ClientMessage.newBuilder().setFitRes(res).build();
    }

    private static ClientMessage evaluateResAsProto(float accuracy, int testing_size){
        ClientMessage.EvaluateRes res = ClientMessage.EvaluateRes.newBuilder().setLoss(accuracy).setNumExamples(testing_size).build();
        return ClientMessage.newBuilder().setEvaluateRes(res).build();
    }
}