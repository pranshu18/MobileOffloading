package com.example.offloadingproject;

import static java.nio.charset.StandardCharsets.UTF_8;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MasterActivity extends AppCompatActivity {

    ConnectionsClient client;
    Button advertBtn, multMastBtn, multSlvBtn;
    TextView logDetailsText, slaveCountText, latText, longText, statusText;
    double longitude, latitude;
    FusedLocationProviderClient locationProviderClient;
    HashMap<String, SlaveDetails> slaveMap = new HashMap<>();
    String slaveName, slaveId;
    int[][] matrix1, matrix2;
    int matrixLen = 50, intervalLen = 5;
    int[][] matrixOutput = new int[matrixLen][matrixLen];;
    String masterName="";
    LinkedList<int[]> slaveMatrixList = new LinkedList<>();;
    private static volatile Map<String, int[]> slaveMatrixMap = new HashMap<>();
    long startTime, strBttr;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        getSupportActionBar().setTitle("Master");

        masterName = getIntent().getStringExtra("serverName");
        advertBtn = findViewById(R.id.button);
        multMastBtn = findViewById(R.id.button2);
        multSlvBtn = findViewById(R.id.button3);
        logDetailsText = findViewById(R.id.textView2);
        slaveCountText = findViewById(R.id.textView3);
        latText = findViewById(R.id.textView5);
        longText = findViewById(R.id.textView6);
        statusText = findViewById(R.id.textView7);
        multSlvBtn.setEnabled(false);

        setLocationValues();
        createMatrices();

        statusText.setText("Matrices created");

        client = Nearby.getConnectionsClient(this);

        advertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    statusText.setText("Started Advertising");
                    client.startAdvertising(masterName,
                            MasterActivity.this.getPackageName(),
                            connectionLifecycleCallback,
                            new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build());
                } catch (Exception e) {
                    Toast.makeText(MasterActivity.this, "Error: Could not advertise", Toast.LENGTH_SHORT).show();
                }
            }
        });

        multMastBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BatteryManager bttryMgr = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                long stEg = bttryMgr.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                long stTm = System.currentTimeMillis();

                String mstName = "master";
                String id = "";
                double batteryLvl = 0.0;
                double lat = 0.0;
                double longit = 0.0;
                SlaveDetails master = new SlaveDetails(mstName, id, batteryLvl, lat, longit, matrix1, matrix2, null);
                performMatrixMultiplication(master);

                long edEg = bttryMgr.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                long difference = Math.abs(stEg - edEg);
                long edTm = System.currentTimeMillis();
                double timeTaken = ((double) (edTm - stTm) )/ 1000;
                String timeSt = timeTaken + " seconds\n";
                String powConsSt =  difference + " mAh";

                statusText.setText("Time for Computation on Master :  " + timeSt + "Power Consumption: " + powConsSt);

            }
        });

        multSlvBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                startTime = System.currentTimeMillis();
                BatteryManager bttryMgr = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                strBttr = bttryMgr.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

                StringBuilder sb = new StringBuilder();
                for(Map.Entry<String, SlaveDetails> entry:slaveMap.entrySet()){
                    String endpoint = entry.getKey();
                    String slaveName = entry.getValue().getName();
                    String stringToAdd = slaveName + " : " + endpoint + "\n";
                    sb.append(stringToAdd);
                }

                String lst = sb.toString();
                statusText.setText("Slaves In Use :\n" + lst);


                for (int i = 0; i < matrix1.length; i += intervalLen) {
                    for (int j = 0; j < matrix2.length; j += intervalLen) {
                        slaveMatrixList.addLast(new int[]{i, j});
                    }
                }

                for(Map.Entry<String, SlaveDetails> entry:slaveMap.entrySet()) {
                    String endpoint = entry.getKey();
                    SlaveDetails currSlv = entry.getValue();
                    int[] indices = slaveMatrixList.removeFirst();
                    slaveMatrixMap.put(endpoint, indices);

                    int mat1Start = indices[0];
                    int mat1End = indices[0] + intervalLen;

                    int mat2Start = indices[1];
                    int mat2End = indices[1] + intervalLen;

                    currSlv.setMatrix1(Arrays.copyOfRange(matrix1, mat1Start, mat1End));
                    currSlv.setMatrix2(Arrays.copyOfRange(matrix2, mat2Start, mat2End));

                    byte[] bts = gson.toJson(currSlv).getBytes(UTF_8);
                    ByteArrayInputStream is = new ByteArrayInputStream(bts);
                    Payload pld = Payload.fromStream(is);
                    client.sendPayload(endpoint, pld);
                }
            }
        });
    }

    public void performMatrixMultiplication(SlaveDetails slv) {
        int[][] ans = new int[slv.getMatrix1().length][slv.getMatrix1()[0].length];

        for (int i = 0; i < slv.getMatrix1().length; i++) {
            int[] tmp = new int[slv.getMatrix1().length];
            for (int j = 0; j < slv.getMatrix1().length; j++) {
                int sum = 0;
                for (int k = 0; k < slv.getMatrix1()[0].length; k++) {
                    sum += slv.getMatrix1()[i][k] * slv.getMatrix1()[j][k];
                }
                tmp[j] = sum;
            }
            ans[i] = tmp;
        }

        String res = Arrays.deepToString(ans);
        Toast.makeText(this, "Result = \n" + res, Toast.LENGTH_SHORT).show();

        slv.setResult(ans);

    }

        private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    confirmConnection(endpointId, connectionInfo);
                    slaveId = endpointId;
                    slaveName = connectionInfo.getEndpointName();
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (!result.getStatus().isSuccess()) {
                        String infoTxt = "Connection failed: " + slaveName + " : " + slaveId;
                        statusText.setText(infoTxt);
                    } else {
                        String infoTxt = "Connected to : " + slaveName + " : " + slaveId;
                        statusText.setText(infoTxt);
                        client.stopAdvertising();
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    //failure recovery if computation is left
                    if (slaveMatrixMap.get(endpointId)!=null) {
                        int[] indices = slaveMatrixMap.get(endpointId);
                        slaveMatrixList.addLast(indices);
                        slaveMatrixMap.remove(endpointId);
                    }
                    if(slaveMap.get(endpointId)!=null)
                        slaveMap.remove(endpointId);
                    slaveCountText.setText("Currently connected to "+ slaveMap.size() +" slave(s)");


                }
            };

    private void confirmConnection(String endpointId, ConnectionInfo connectionInfo) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setNegativeButton("Deny", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                client.rejectConnection("Received Rejection");
            }
        });

        dialog.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                client.acceptConnection(endpointId, payloadCallback);
            }
        });

        dialog.setMessage("Connection req from server: " + connectionInfo.getEndpointName());
        dialog.setTitle("Confirm");
        dialog.create().show();
    }


    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onPayloadReceived(final String endpointId, Payload payload) {
                    InputStream is = payload.asStream().asInputStream();
                    SlaveDetails slave = new Gson().fromJson(new InputStreamReader(is, UTF_8), SlaveDetails.class);
                    slave.setId(endpointId);
                    try {
                        if (slave.getResult() != null) {
                            calculateOutput(slave,endpointId, 0);
                            saveDetails(slave);
                            if (!slaveMatrixList.isEmpty()) {
                                continueMatrixComputation(endpointId);
                            } else if (slaveMatrixMap.isEmpty()) {
                                BatteryManager bttrMgr = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                                long edEg = bttrMgr.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                                long totEg = Math.abs(strBttr - edEg);
                                long endTime = System.currentTimeMillis();
                                double timeTaken = (double) (endTime - startTime) / 1000;
                                startTime = 0;
                                strBttr = 0;
                                String timeSt = Double.toString(timeTaken) + " seconds\n";
                                String egSt = totEg + " mAh\n";
                                statusText.setText("Finished by Slave(s) in " + timeSt + "Total Energy Consumption: " + egSt);
                                for (String key : slaveMap.keySet()) {
                                    SlaveDetails currentSlave = slaveMap.get(key);
                                    currentSlave.setComputed(true);
                                    client.sendPayload(key, Payload.fromStream(new ByteArrayInputStream(gson.toJson(currentSlave).getBytes(UTF_8))));
                                }
                            }
                        }else{
                            saveDetails(slave);
                            if(!slaveMap.containsKey(endpointId)) {
                                double dist = getDistanceBetweenLocations(latitude, longitude, slave.getLatitude(), slave.getLongitude());
                                if (dist < 2500 && slave.getBatteryLevel() > 15) {
                                    multSlvBtn.setEnabled(true);
                                    if (!slaveMap.containsKey(endpointId)) {
                                        slaveMap.put(endpointId, slave);
                                    }

                                    slaveCountText.setText("Currently connected to " + slaveMap.size() + " slave(s)");

                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {

                }
            };

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void continueMatrixComputation(String endPoint)
    {
        List<String> list = new ArrayList<>();
        for (String k : slaveMap.keySet()) {
            list.add(slaveMap.get(k).getName() + " : " + k);
        }
        statusText.setText("Slaves in use :\n" + String.join("\n", list));

        int[] indices = slaveMatrixList.removeFirst();
        slaveMatrixMap.put(endPoint, indices);
        Thread th = new Thread("ContinueComputation"){
            public void run() {
                SlaveDetails currSlv = slaveMap.get(endPoint);
                if (currSlv == null) {
                    slaveMap.remove(endPoint);
                } else {
                    currSlv.setMatrix1(Arrays.copyOfRange(matrix1, indices[0], indices[0] + intervalLen));
                    currSlv.setMatrix2(Arrays.copyOfRange(matrix2, indices[1], indices[1] + intervalLen));

                    byte[] bts = gson.toJson(currSlv).getBytes(UTF_8);
                    ByteArrayInputStream is = new ByteArrayInputStream(bts);
                    Payload pld = Payload.fromStream(is);
                    client.sendPayload(endPoint, pld);
                }
            }
        };
        th.start();
    }

    public void calculateOutput(SlaveDetails slave, String endpointId, int defaultId) {
        if(endpointId==null)
            endpointId = Integer.toString(defaultId);
        final int[] indices = slaveMatrixMap.get(endpointId);
        slaveMatrixMap.remove(endpointId);
        final int[][] resultSlave = slave.getResult();
        Thread thread = new Thread("OutputCalculation") {
            public void run(){
                if (indices == null) {
                    //add later
                }else{
                    int st1 = indices[0];
                    int ed1 = indices[0] + intervalLen;
                    int st2 = indices[1];
                    int ed2 = indices[1] + intervalLen;
                    for (int i = st1; i < ed1; i++) {
                        for (int j = st2; j < ed2; j++) {
                            int ind1 = i - indices[0];
                            int ind2 = j - indices[1];
                            matrixOutput[i][j] = resultSlave[ind1][ind2];
                        }
                    }
                }
            }
        };
        thread.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveDetails(SlaveDetails slave) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyy HH:mm:ss"));
        logDetailsText.setText("Logs updated at "+timestamp);

        File folder=new File(this.getExternalFilesDir(Environment.DIRECTORY_DCIM) + File.separator + "logFolder");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File logFile = new File(folder, "Log.txt");
        StringBuilder text;
        if(logFile.exists()){
            text=readFile(logFile);
        }else{
            text=new StringBuilder();
        }
        text.append('\n');
        String line=slave.getAllContentAsString();
        text.append(line);
        FileWriter writer = null;
        try {
            writer = new FileWriter(logFile);
            writer.append(text.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private StringBuilder readFile(File file) {
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine())!= null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) { }
        return text;
    }

    private double getDistanceBetweenLocations(double latitude1, double longitude1, double latitude2, double longitude2)
    {
        if(latitude1 != 0 && longitude1 != 0 && latitude2 != 0 && longitude2 != 0)
        {
            Location stPnt=new Location("locationA");
            stPnt.setLongitude(longitude1);
            stPnt.setLatitude(latitude1);

            Location edPnt=new Location("locationB");
            edPnt.setLongitude(longitude2);
            edPnt.setLatitude(latitude2);

            double ans = stPnt.distanceTo(edPnt);
            return ans;
        }else{
            return 0;
        }

    }

    private void setLocationValues() {
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        int granted = PackageManager.PERMISSION_GRANTED;
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        if (this.checkCallingOrSelfPermission(permission) == granted){
            Task<Location> task = locationProviderClient.getLastLocation();
            task.addOnSuccessListener(MasterActivity.this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        latText.setText("Latitude: " + String.valueOf(latitude));
                        longText.setText("Longitude: " + String.valueOf(longitude));
                    }
                }
            });
        }

    }

    private void createMatrices() {
        Random r = new Random();

        matrix1 = new int[matrixLen][matrixLen];
        matrix2 = new int[matrixLen][matrixLen];

        for (int i = 0; i < matrixLen; i++) {
            for (int j = 0; j < matrixLen; j++) {
                matrix1[i][j] = r.nextInt(100);
                matrix2[i][j] = r.nextInt(100);
            }
        }
    }
}