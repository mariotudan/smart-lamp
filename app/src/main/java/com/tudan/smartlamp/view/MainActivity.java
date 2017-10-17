package com.tudan.smartlamp.view;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tudan.smartlamp.R;
import com.tudan.smartlamp.model.RGBColor;
import com.tudan.smartlamp.model.TouchCoordinate;
import com.tudan.smartlamp.model.HSLColor;
import com.tudan.smartlamp.service.BluetoothService;
import com.tudan.smartlamp.utils.DisposableUtils;
import com.tudan.smartlamp.utils.RGBEyeFix;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements ColorPickerView {

    @BindView(R.id.colorBackground)
    TextView colorBackground;
    @BindView(R.id.whiteGradientBackground)
    TextView whiteGradientBackground;
    @BindView(R.id.blackGradientBackground)
    TextView blackGradientBackground;
    GradientDrawable blackGradient;

    @BindView(R.id.bulbLight)
    ImageView bulbLight;

    @BindView(R.id.layout)
    ConstraintLayout layout;

    @BindView(R.id.btStatusMessage)
    TextView btStatusMessage;
    @BindView(R.id.btStatusMessageBackground)
    TextView btStatusMessageBackground;

    CompositeDisposable disposables;
    HSLColor hslColor = new HSLColor(0, 0, 0.5f);
    int intColor = ColorUtils.HSLToColor(new float[]{hslColor.getHue(), hslColor.getSaturation(), hslColor.getLightness()});
    float brightness = 0.8f;

    boolean receivedColorFromLamp = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setupBackground();
        setBackgroundColor();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    }

    @Override
    protected void onStart() {
        super.onStart();

        disposables = new CompositeDisposable();

        BluetoothService.setupBluetooth(this);
        if (!receivedColorFromLamp) {
            receivedColorFromLamp = true;
            BluetoothService.getLampColor()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(rgbColor -> {
                        float[] hsl = new float[]{0, 0, 0};
                        ColorUtils.RGBToHSL(rgbColor.getRed(), rgbColor.getGreen(), rgbColor.getBlue(), hsl);
                        hslColor.setHue(hsl[0]);
                        hslColor.setSaturation(1);
                        hslColor.setLightness(Math.min(1f, Math.max(0.5f, hsl[2])));
                        brightness = 1f;
                        if (hsl[2] < 0.5f) {
                            brightness = hsl[2] * 2;
                        }
                        setBackgroundColor();
                    });
        }

        disposables.add(Observable.create((ObservableEmitter<TouchCoordinate> e) -> {
            layout.setOnTouchListener((v, event) -> {
                final int action = event.getAction();
                boolean dualTouch = event.getPointerCount() > 1;
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        // to be filtered as too big movement
                        e.onNext(new TouchCoordinate(100000, 100000, dualTouch));
                    case MotionEvent.ACTION_MOVE: {
                        e.onNext(new TouchCoordinate(event.getX(), event.getY(), dualTouch));
                        break;
                    }
                }
                return true;
            });
            e.setDisposable(DisposableUtils.getDisposable(i -> layout.setOnTouchListener(null)));
        })
                .observeOn(Schedulers.computation())
                .buffer(2, 1)
                .map((coords) -> {
                    TouchCoordinate coord1 = coords.get(0);
                    TouchCoordinate coord2 = coords.get(1);
                    boolean dualTouch = coord1.isDualTouch() || coord2.isDualTouch();
                    return new TouchCoordinate(coord2.getX() - coord1.getX(), coord2.getY() - coord1.getY(), dualTouch);
                })
                .filter(coordinate -> coordinate.getX() < 200 && coordinate.getX() > -200) // filtering touch down as too big movement
                .doOnNext(coord -> {
                    if (Math.abs(coord.getX()) > Math.abs(coord.getY())) coord.setY(0);
                    else coord.setX(0);
                })
                .throttleFirst(20, TimeUnit.MILLISECONDS)
                .map(movement -> {
                    if (movement.isDualTouch()) {
                        brightness -= movement.getY() * 0.001f;
                        brightness = Math.min(1f, Math.max(0f, brightness));
                    } else {
                        float hue = hslColor.getHue();
                        hue -= movement.getX() * 0.2f;
                        hue = (hue + 360) % 360;
                        hslColor.setHue(hue);

                        float lightness = hslColor.getLightness();
                        lightness -= movement.getY() * 0.001f;
                        lightness = Math.min(1f, Math.max(0.5f, lightness));
                        hslColor.setLightness(lightness);
                    }
                    return hslColor;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(hslColor -> setBackgroundColor())
                .observeOn(Schedulers.single())
                .flatMap(hslColor -> Observable.interval(0, 2500, TimeUnit.MILLISECONDS)
                        .take(2)
                        .map(ignored -> hslColor))
                .debounce(1000, TimeUnit.MILLISECONDS)
                .subscribe(hslColor -> {
                    RGBColor rgbColor = new RGBColor(intColor);
                    rgbColor.adjustBrightness(brightness);
                    RGBEyeFix.fixColor(rgbColor);
                    BluetoothService.sendColorBT(rgbColor);
                }));
    }

    @Override
    protected void onPause() {
        super.onPause();
        BluetoothService.stopBluetooth();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.dispose();
        BluetoothService.stopBluetooth();
    }

    private void setupBackground() {
        colorBackground.setScaleX(1.3f);
        whiteGradientBackground.setScaleY(1.7f);
        blackGradientBackground.setScaleY(1.7f);
        btStatusMessageBackground.setScaleX(6f);

        GradientDrawable whiteGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(120, 255, 255, 255), Color.TRANSPARENT, Color.TRANSPARENT});
        whiteGradientBackground.setBackground(whiteGradient);

        blackGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.TRANSPARENT, Color.rgb(0, 0, 0)});
        blackGradient.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        blackGradient.setGradientRadius(400);
        blackGradientBackground.setBackground(blackGradient);

        GradientDrawable btStatusGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.argb(100, 0, 0, 0), Color.TRANSPARENT});
        btStatusGradient.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        btStatusGradient.setGradientRadius(70);
        btStatusMessageBackground.setBackground(btStatusGradient);
    }

    private void setBackgroundColor() {
        intColor = ColorUtils.HSLToColor(new float[]{hslColor.getHue(), hslColor.getSaturation(), hslColor.getLightness()});
        int colorLeft = ColorUtils.HSLToColor(new float[]{(hslColor.getHue() - 15 + 360) % 360, hslColor.getSaturation(), hslColor.getLightness()});
        int colorRight = ColorUtils.HSLToColor(new float[]{(hslColor.getHue() + 15 + 360) % 360, hslColor.getSaturation(), hslColor.getLightness()});

        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{colorLeft, intColor, intColor, colorRight});

        colorBackground.setBackground(gradient);

        float brightnesSq = brightness * brightness;
        blackGradient.setGradientRadius(brightness > 0.11f ? ((brightnesSq * 500) + 300) : 0);
        blackGradientBackground.setAlpha(1 - brightnesSq);
        bulbLight.setAlpha(brightness > 0.11f && receivedColorFromLamp ? (brightness + 0.4f) : 0);
    }

    @Override
    public void showBluetoothUnavailable() {
        Toast.makeText(getApplicationContext(), "Bluetooth not available, exiting application", Toast.LENGTH_SHORT).show();
        Observable.timer(3, TimeUnit.SECONDS)
                .subscribe(ignored -> finish());
    }

    @Override
    public void enableBluetoothIntent() {
        Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(turnBTon, ColorPickerView.ENABLE_BLUETOOTH);
    }

    @Override
    public void showConnectingMessage() {
        btStatusMessage.setText(R.string.connecting_to_smart_lamp);
        setStatusMessageVisibility(View.VISIBLE);
    }

    @Override
    public void showConnectedMessage() {
        btStatusMessage.setText(R.string.connected_to_smart_lamp);
        setStatusMessageVisibility(View.VISIBLE);
        Observable.timer(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(ignored -> setStatusMessageVisibility(View.INVISIBLE));
    }

    private void setStatusMessageVisibility(int visibility) {
        btStatusMessage.setVisibility(visibility);
        btStatusMessageBackground.setVisibility(visibility);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ColorPickerView.ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                BluetoothService.setupBluetooth(this);
            } else {
                finish();
            }
        }
    }
}
