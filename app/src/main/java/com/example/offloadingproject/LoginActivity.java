package com.example.offloadingproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    int permissionRequestCode =  1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().setTitle("Mobile Offloading Project");

        String[] permissions = {
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.ACCESS_WIFI_STATE,
        };

        if (!checkPermissions(this, permissions)){
            ActivityCompat.requestPermissions(this, permissions, permissionRequestCode);
        }

        EditText nameTextBox = findViewById(R.id.editTextTextPersonName);
        Button submitMstBtn = findViewById(R.id.button4);
        Button submitSlvBtn = findViewById(R.id.button5);

        submitMstBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = nameTextBox.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Enter server name!", Toast.LENGTH_SHORT).show();
                } else {
                    Intent k = new Intent(LoginActivity.this, MasterActivity.class);
                    k.putExtra("serverName", name);
                    startActivity(k);
                }
            }
        });

        submitSlvBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = nameTextBox.getText().toString().trim();
                if (name.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Enter server name!", Toast.LENGTH_SHORT).show();
                } else {
                    Intent k = new Intent(LoginActivity.this, SlaveActivity.class);
                    k.putExtra("serverName", name);
                    startActivity(k);
                }
            }
        });
    }

    public static boolean checkPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == permissionRequestCode) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "provide permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

}