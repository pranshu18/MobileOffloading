package com.example.offloadingproject;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SlaveDetails {

    private String name;
    private String id;
    private int[][] matrix1;
    private int[][] matrix2;
    private int[][] result;
    private boolean computed;
    private double batteryLevel;
    private double latitude;
    private double longitude;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int[][] getMatrix1() {
        return matrix1;
    }

    public void setMatrix1(int[][] matrix1) {
        this.matrix1 = matrix1;
    }

    public int[][] getMatrix2() {
        return matrix2;
    }

    public void setMatrix2(int[][] matrix2) {
        this.matrix2 = matrix2;
    }

    public int[][] getResult() {
        return result;
    }

    public void setResult(int[][] result) {
        this.result = result;
    }

    public boolean isComputed() {
        return computed;
    }

    public void setComputed(boolean computed) {
        this.computed = computed;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SlaveDetails(String name, String id, double battery, double latitude, double longitude, int[][] m1, int[][] m2, int[][] result) {
        this.name = name;
        this.id = id;
        this.batteryLevel = battery;
        this.latitude = latitude;
        this.longitude = longitude;
        this.matrix1 = m1;
        this.matrix2 = m2;
        this.result = result;
        this.computed = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getAllContentAsString() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        return "Server Name: " + name + "\n" + "Time: "+ time + "\n"+"EndpointID: " + id + "\n" + "Battery: " + batteryLevel + "%\n" + "Latitude: " + latitude + "\n" + "Longitude: " + longitude;
    }

}
