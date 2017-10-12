package com.tudan.smartlamp.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.tudan.smartlamp.model.RGBColor;
import com.tudan.smartlamp.utils.RGBEyeFix;

import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by mario on 12.10.2017..
 */

public class BluetoothService {
    public static final String LAMP_HW_ADDRESS = "98:D3:32:30:B0:91";
    public static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static BluetoothAdapter myBluetooth;
    private static BluetoothDevice lamp;
    private static BluetoothSocket btSocket;

    private static boolean btConnected = false;

    public static void setupBluetooth(){
        getBluetoothAdapter();
        connectToLamp();
    }

    private static void getBluetoothAdapter() {
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        myBluetooth.cancelDiscovery();
        /*
        if (myBluetooth == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
        } else {
            if (!myBluetooth.isEnabled()) {
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, 1);
            }
        }*/
    }

    private static void connectToLamp() {
        Observable.just(BluetoothService.LAMP_HW_ADDRESS)
                .observeOn(Schedulers.computation())
                .subscribe(address -> {
                    lamp = myBluetooth.getRemoteDevice(address);
                    btSocket = lamp.createInsecureRfcommSocketToServiceRecord(uuid);
                    btSocket.connect();
                    btConnected = true;
                });
    }

    public static void sendColorBT(int intColor) {
        if (btConnected) {
            RGBColor rgbColor = new RGBColor(intColor);
            RGBEyeFix.fixColor(rgbColor);

            String text = String.format("%03d%03d%03d", rgbColor.getRed(), rgbColor.getGreen(), rgbColor.getBlue());
            Log.d("BLUETOOTH COLOR", text);
            Observable.just(text)
                    .observeOn(Schedulers.computation())
                    .subscribe(message -> btSocket.getOutputStream().write(message.getBytes()));
        }
    }
}
