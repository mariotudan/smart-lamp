package com.tudan.smartlamp.view;

/**
 * Created by mario on 13.10.2017..
 */

public interface ColorPickerView {
    int ENABLE_BLUETOOTH = 1;
    void showBluetoothUnavailable();
    void enableBluetoothIntent();
    void showConnectingMessage();
    void showConnectedMessage();
}
