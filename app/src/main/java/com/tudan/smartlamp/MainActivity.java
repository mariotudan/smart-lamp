package com.tudan.smartlamp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.tudan.smartlamp.model.HSLColor;

import java.util.concurrent.TimeUnit;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function3;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.seekBarHue)
    SeekBar hueSlider;
    @BindView(R.id.seekBarSaturation)
    SeekBar saturationSlider;
    @BindView(R.id.seekBarLightness)
    SeekBar lightnessSlider;

    @BindView(R.id.colorView)
    TextView colorView;
    @BindView(R.id.btnPickDevice)
    Button btnPickDevice;

    @BindView(R.id.btnUpdateColor)
    Button btnUpdateColor;

    CompositeDisposable disposables;
    HSLColor hslColor = new HSLColor(0, 100, 50);
    int color = ColorUtils.HSLToColor(new float[]{hslColor.getHue(), hslColor.getSaturation(), hslColor.getLightness()});

    BluetoothSPP bt;

    final int[] rgbFix = new int[]{
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2,
            2, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 5, 5, 5,
            5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10,
            10, 10, 11, 11, 11, 12, 12, 13, 13, 13, 14, 14, 15, 15, 16, 16,
            17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22, 23, 24, 24, 25,
            25, 26, 27, 27, 28, 29, 29, 30, 31, 32, 32, 33, 34, 35, 35, 36,
            37, 38, 39, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 50,
            51, 52, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 66, 67, 68,
            69, 70, 72, 73, 74, 75, 77, 78, 79, 81, 82, 83, 85, 86, 87, 89,
            90, 92, 93, 95, 96, 98, 99, 101, 102, 104, 105, 107, 109, 110, 112, 114,
            115, 117, 119, 120, 122, 124, 126, 127, 129, 131, 133, 135, 137, 138, 140, 142,
            144, 146, 148, 150, 152, 154, 156, 158, 160, 162, 164, 167, 169, 171, 173, 175,
            177, 180, 182, 184, 186, 189, 191, 193, 196, 198, 200, 203, 205, 208, 210, 213,
            215, 218, 220, 223, 225, 228, 231, 233, 236, 239, 241, 244, 247, 249, 252, 255};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        bt = new BluetoothSPP(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        disposables = new CompositeDisposable();

        disposables.add(Observable.combineLatest(
                seekBarChangeObservable(hueSlider).startWith(0),
                seekBarChangeObservable(saturationSlider).startWith(100),
                seekBarChangeObservable(lightnessSlider).startWith(50),
                (Function3<Integer, Integer, Integer, HSLColor>) (hue, saturation, lightness) -> hslColor.update(hue, saturation, lightness)
        ).debounce(20, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<HSLColor>() {
                    @Override
                    public void accept(HSLColor hslColor) throws Exception {
                        color = ColorUtils.HSLToColor(new float[]{hslColor.getHue(), hslColor.getSaturation(), hslColor.getLightness()});
                        colorView.setBackgroundColor(color);
                        Log.d("Color", String.format("%f, %f, %f", hslColor.getHue(), hslColor.getSaturation(), hslColor.getLightness()));
                    }
                }));

        disposables.add(RxView.clicks(btnPickDevice).subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(o -> {
                    bt.setupService();
                    bt.startService(BluetoothState.DEVICE_OTHER);

                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                }));

        disposables.add(RxView.clicks(btnUpdateColor)
                .observeOn(AndroidSchedulers.mainThread())
                .map(o -> color)
                .doOnNext(color -> Log.d("BLUETOOTH UPDATE COLOR", String.format("%f, %f, %f", hslColor.getHue(), hslColor.getSaturation(), hslColor.getLightness())))
                .subscribe(color -> {
                    int red = Color.red(color);
                    int green = Color.green(color);
                    int blue = Color.blue(color);
                    red = rgbFix[red];
                    green = rgbFix[green];
                    blue = rgbFix[blue];

                    red = Math.min(red * 2, 255);
                    green = Math.min((int) (green * 0.5f), 255);
                    blue = Math.min((int) (blue * 0.3f), 255);

                    String text = String.format("%03d%03d%03d", red, green, blue);
                    Log.d("BLUETOOTH COLOR", text);
                    bt.send(text, false);
                }));
    }

    private Observable seekBarChangeObservable(final SeekBar seekBar) {
        return Observable.create((ObservableEmitter<Integer> emitter) -> {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                    emitter.onNext(value);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            emitter.setDisposable(new Disposable() {
                @Override
                public void dispose() {
                    seekBar.setOnSeekBarChangeListener(null);
                }

                @Override
                public boolean isDisposed() {
                    return false;
                }
            });
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.dispose();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                bt.connect(data);
            }
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            } else {
                // Do something if user doesn't choose any device (Pressed back)
            }
        }
    }
}
