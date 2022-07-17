package com.example.offloadingproject;

import static java.nio.charset.StandardCharsets.UTF_8;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class SlaveActivity extends AppCompatActivity {

    Button discoverBtn, connectBtn, disconnectBtn;
    TextView statusText, batteryText, latText, longText;
    Double latitude, longitude;
    FusedLocationProviderClient locationProviderClient;
    ConnectionsClient client;
    String slaveName, masterName, masterEndPtId;
    int batteryLvl;
    private Gson gson = new Gson();
    Timer timer;
    boolean calculationInProgress = false;
    long startBattery, startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slave);

        getSupportActionBar().setTitle("Slave");

        slaveName = getIntent().getStringExtra("serverName");
        discoverBtn = findViewById(R.id.button6);
        connectBtn = findViewById(R.id.button7);
        disconnectBtn = findViewById(R.id.button8);
        statusText = findViewById(R.id.textView);
        batteryText = findViewById(R.id.textView4);
        latText = findViewById(R.id.textView8);
        longText = findViewById(R.id.textView9);

        connectBtn.setEnabled(false);
        disconnectBtn.setEnabled(false);

        setBatteryLevel();
        setLocationValues();

        client = Nearby.getConnectionsClient(this);
        discoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    statusText.setText("Started Discovery");
                    client.startDiscovery(SlaveActivity.this.getPackageName(),
                            endpointDiscoveryCallback,
                            new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build());
                } catch (Exception e) {
                    Toast.makeText(SlaveActivity.this, "Error: Could not discover", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    masterName = info.getEndpointName();
                    masterEndPtId = endpointId;
                    connectBtn.setEnabled(true);
                    connectBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            client.requestConnection(slaveName, endpointId, connectionLifecycleCallback);
                        }
                    });
                }

                @Override
                public void onEndpointLost(String endpointId) {}
            };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    confirmConnection(endpointId, connectionInfo);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        client.stopDiscovery();
                        statusText.setText("Connected to: " + masterName + " : " + masterEndPtId);
                        connectBtn.setEnabled(false);
                        disconnectBtn.setEnabled(true);
                        disconnectBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                client.disconnectFromEndpoint(masterEndPtId);
                                recreate();
                            }
                        });

                        setLocationValues();
                        setBatteryLevel();
                        String id ="";
                        SlaveDetails slaveSend = new SlaveDetails(slaveName, id, batteryLvl, latitude, longitude, null, null, null);
                        byte[] bts = gson.toJson(slaveSend).getBytes(UTF_8);
                        ByteArrayInputStream bis = new ByteArrayInputStream(bts);
                        Payload pld = Payload.fromStream(bis);
                        client.sendPayload(masterEndPtId, pld);
                        TimerTask taskTmr = new SendRepeatedly();
                        timer = new Timer(true);
                        timer.scheduleAtFixedRate(taskTmr, 0, 60000);
                    } else {
                        statusText.setText("Connection Failed: " + masterName + " : " + masterEndPtId);
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                }
            };

    public class SendRepeatedly extends TimerTask {
        @Override
        public void run() {
            setLocationValues();
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    setBatteryLevel();

                }
            });
            String id = "";
            SlaveDetails slaveSend = new SlaveDetails(slaveName, id, batteryLvl, latitude, longitude, null, null, null);
            byte[] bts = gson.toJson(slaveSend).getBytes(UTF_8);
            ByteArrayInputStream bis = new ByteArrayInputStream(bts);
            Payload pld = Payload.fromStream(bis);
            client.sendPayload(masterEndPtId, pld);
        }
    }

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
                @Override
                public void onPayloadReceived(final String endpointId, Payload payload) {
                    InputStream is = payload.asStream().asInputStream();
                    SlaveDetails slaveRec = new Gson().fromJson(new InputStreamReader(is, UTF_8), SlaveDetails.class);
                    try {
                        if (!slaveRec.isComputed()) {
                            if (!calculationInProgress) {
                                BatteryManager bttryMgr = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                                startTime = System.currentTimeMillis();
                                startBattery = bttryMgr.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                                calculationInProgress = true;
                            }
                            String mastDetails = masterName + " : " + masterEndPtId;
                            statusText.setText("Performing Mat. Mulp. for: " + mastDetails);
                            performMatrixMultiplication(slaveRec);
                            client.sendPayload(masterEndPtId, Payload.fromStream(new ByteArrayInputStream(gson.toJson(slaveRec).getBytes(UTF_8))));
                        }else{
                            BatteryManager batteryMgr = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                            long edEg = batteryMgr.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                            long totEg = Math.abs(startBattery - edEg);
                            long endingTime = System.currentTimeMillis();
                            double elapsedTm = (double) (endingTime - startTime) / 1000;
                            String timeSt = elapsedTm + " seconds\n";
                            String egSt = totEg + " mAh";
                            statusText.setText("Finished in " + timeSt + "Total Energy Consumption: " + egSt);
                            startBattery = 0;
                            calculationInProgress = false;
                            startTime = 0;
                        }
                    } catch (Exception e) {
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {

                }
            };

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

    private void setLocationValues() {
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        int granted = PackageManager.PERMISSION_GRANTED;
        String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        if (this.checkCallingOrSelfPermission(permission) == granted){
            Task<Location> task = locationProviderClient.getLastLocation();
            task.addOnSuccessListener(SlaveActivity.this, new OnSuccessListener<Location>() {
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

    private void setBatteryLevel() {
        IntentFilter batChngIntent = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryRec = this.registerReceiver(null, batChngIntent);

        int batValue = batteryRec.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int value = batValue * 100;
        int scaleValue = batteryRec.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        value = value / scaleValue;
        batteryText.setText("Battery Level: " + value + "%");
        batteryLvl = value;
    }


}