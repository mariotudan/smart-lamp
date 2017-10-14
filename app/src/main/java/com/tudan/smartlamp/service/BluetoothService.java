package com.tudan.smartlamp.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.tudan.smartlamp.model.RGBColor;
import com.tudan.smartlamp.utils.ColorHelper;
import com.tudan.smartlamp.view.ColorPickerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;

/**
 * Created by mario on 12.10.2017..
 */

public class BluetoothService {
    public static final String LAMP_HW_ADDRESS = "98:D3:32:30:B0:91";
    public static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String REQUEST_COLOR = "0";
    public static final String RECEIVE_COLOR = "1";
    public static final String SET_COLOR = "2";

    private static ColorPickerView view;

    private static BluetoothAdapter btAdapter;
    private static BluetoothDevice lamp;
    private static BluetoothSocket btSocket;

    private static List<BluetoothSocket> historySockets = new ArrayList<>();

    private static boolean btConnected = false;
    private static boolean btConnecting = false;

    private static RGBColor unsentColor = null;

    private static boolean isColorRequested = false;
    private static boolean requestedColorReceived = false;
    private static ReplaySubject<RGBColor> requestedColor = ReplaySubject.create();

    static {
        Observable.interval(30, TimeUnit.SECONDS)
                .flatMap(ignored ->
                        Observable.range(0, historySockets.size())
                                .map(index -> {
                                    BluetoothSocket bts = getHistorySocket(index);
                                    return bts != null ? bts : btSocket;
                                })
                )
                .filter(bluetoothSocket -> !btSocket.equals(bluetoothSocket))
                .observeOn(Schedulers.computation())
                .subscribe(btSocket1 -> {
                    stopBluetooth(btSocket1);
                    syncFunction(historySockets::remove, btSocket1);
                });
    }

    public static void setupBluetooth(ColorPickerView colorPickerView) {
        view = colorPickerView;
        getBluetoothAdapter();
    }

    private static void getBluetoothAdapter() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            view.showBluetoothUnavailable();
        } else {
            if (!btAdapter.isEnabled()) {
                view.enableBluetoothIntent();
            } else {
                btAdapter.cancelDiscovery();
                lamp = btAdapter.getRemoteDevice(LAMP_HW_ADDRESS);
                connectToLamp();
            }
        }
    }

    private static void connectToLamp() {
        if (!btConnecting) {
            btConnecting = true;

            Observable.just(BluetoothService.LAMP_HW_ADDRESS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(ignored -> view.showConnectingMessage())
                    .observeOn(Schedulers.computation())
                    .delay(1000, TimeUnit.MILLISECONDS)
                    .doOnNext(address -> {
                        stopBluetooth();
                        btSocket = lamp.createInsecureRfcommSocketToServiceRecord(uuid);
                        historySockets.add(btSocket);
                        btSocket.connect();
                        btConnected = true;
                        btConnecting = false;
                        if (unsentColor != null) {
                            sendColorBT(unsentColor);
                            unsentColor = null;
                        }
                        if (!isColorRequested) {
                            isColorRequested = true;
                            requestColorBT();
                        }
                    })
                    .retry(10)
                    .onErrorReturn(throwable -> {
                        btConnected = false;
                        btAdapter = BluetoothAdapter.getDefaultAdapter();
                        Observable.timer(10, TimeUnit.SECONDS)
                                .subscribe(ignored -> {
                                    btAdapter.cancelDiscovery();
                                    lamp = btAdapter.getRemoteDevice(LAMP_HW_ADDRESS);
                                    btConnecting = false;
                                    connectToLamp();
                                });
                        return "";
                    })
                    .filter(s -> !s.isEmpty())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(ignored -> view.showConnectedMessage());
        }
    }

    public static void sendColorBT(RGBColor rgbColor) {
        if (btConnected) {
            String text = String.format("%s%03d%03d%03d", SET_COLOR, rgbColor.getRed(), rgbColor.getGreen(), rgbColor.getBlue());
            Log.d("BLUETOOTH COLOR", text);
            Observable.just(text)
                    .observeOn(Schedulers.computation())
                    .doOnNext(message -> btSocket.getOutputStream().write(message.getBytes()))
                    .retry(1)
                    .onErrorReturn(throwable -> {
                        unsentColor = rgbColor;
                        recoverConnection();
                        return "";
                    })
                    .subscribe();
        } else {
            unsentColor = rgbColor;
        }
    }

    private static void requestColorBT() {
        Log.d("BLUETOOTH COLOR", "REQUESTED");

        Observable.just(REQUEST_COLOR)
                .observeOn(Schedulers.computation())
                .doOnNext(message -> btSocket.getOutputStream().write(message.getBytes()))
                .retry(1)
                .onErrorReturn(throwable -> {
                    recoverConnection();
                    Observable.timer(2, TimeUnit.SECONDS)
                            .subscribe(ignored -> requestColorBT());
                    return "";
                })
                .doOnNext(s -> {
                    while (!requestedColorReceived) {
                        if (btSocket.getInputStream().available() > 1) {
                            byte[] buffer = new byte[256];
                            int bytes = btSocket.getInputStream().read(buffer);
                            String result = new String(buffer, 0, bytes);
                            Log.d("BLUETOOTH RESULT", "" + result);
                            requestedColor.onNext(ColorHelper.getRGBColor(result));
                            requestedColorReceived = true;
                            break;
                        }
                    }
                })
                .onErrorReturn(throwable -> "")
                .subscribe();
    }

    public static Observable<RGBColor> getLampColor() {
        return requestedColor.hide();
    }

    private static void recoverConnection() {
        btConnected = false;
        stopBluetooth();
        connectToLamp();
    }

    public static void stopBluetooth() {
        stopBluetooth(btSocket);
    }

    private static void stopBluetooth(BluetoothSocket btSocket) {
        if (btSocket != null) {
            try {
                btSocket.getInputStream().close();
                btSocket.getOutputStream().close();
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static BluetoothSocket getHistorySocket(int index) {
        return syncFunction(i -> {
            if (historySockets.size() > i) {
                return historySockets.get(i);
            }
            return null;
        }, index);
    }

    private synchronized static <T, R> R syncFunction(Function<T, R> function, T value) {
        try {
            return function.apply(value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
