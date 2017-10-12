package com.tudan.smartlamp;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import com.tudan.smartlamp.model.Coordinate;
import com.tudan.smartlamp.model.HSLColor;
import com.tudan.smartlamp.service.BluetoothService;
import com.tudan.smartlamp.utils.DisposableUtils;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.colorBackground)
    TextView colorBackground;
    @BindView(R.id.whiteGradientBackground)
    TextView whiteGradientBackground;
    @BindView(R.id.bulbLight)
    TextView bulbLight;

    @BindView(R.id.layout)
    ConstraintLayout layout;


    CompositeDisposable disposables;
    HSLColor hslColor = new HSLColor(0, 1, 0.5f);
    int intColor = ColorUtils.HSLToColor(new float[]{hslColor.getHue(), hslColor.getSaturation(), hslColor.getLightness()});


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setBackgroundColor();

        colorBackground.setScaleX(1.7f);
        bulbLight.setScaleX(2f);
        whiteGradientBackground.setScaleY(1.7f);

        GradientDrawable whiteGradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(120, 255, 255, 255), Color.TRANSPARENT, Color.TRANSPARENT});
        whiteGradientBackground.setBackground(whiteGradient);

        GradientDrawable bulbLightGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.WHITE, Color.TRANSPARENT});
        bulbLightGradient.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        bulbLightGradient.setCornerRadius(5);
        bulbLightGradient.setGradientRadius(70);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    }

    @Override
    protected void onStart() {
        super.onStart();

        disposables = new CompositeDisposable();

        BluetoothService.setupBluetooth();

        disposables.add(Observable.create((ObservableEmitter<Coordinate> e) -> {
            layout.setOnTouchListener((v, event) -> {
                final int action = event.getAction();
                switch (action & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        // to be filtered as too big movement
                        e.onNext(new Coordinate(100000, 100000));
                    case MotionEvent.ACTION_MOVE: {
                        e.onNext(new Coordinate(event.getX(), event.getY()));
                        break;
                    }
                }
                return true;
            });
            e.setDisposable(DisposableUtils.getDisposable(i -> layout.setOnTouchListener(null)));
        }).buffer(2, 1)
                .map((coords) -> new Coordinate(coords.get(1).getX() - coords.get(0).getX(), coords.get(1).getY() - coords.get(0).getY()))
                .filter(coordinate -> coordinate.getX() < 10000 && coordinate.getX() > -10000) // filtering touch down as too big movement
                .doOnNext(coord -> {
                    if (Math.abs(coord.getX()) > Math.abs(coord.getY())) coord.setY(0);
                    else coord.setX(0);
                })
                .debounce(20, TimeUnit.MILLISECONDS)
                .map(movement -> {
                    float hue = hslColor.getHue();
                    hue -= movement.getX() * 0.2f;
                    hue = (hue + 360) % 360;
                    hslColor.setHue(hue);

                    float lightness = hslColor.getLightness();
                    lightness += movement.getY() * 0.0006f;
                    lightness = Math.min(1f, Math.max(0.5f, lightness));
                    hslColor.setLightness(lightness);
                    return hslColor;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(hslColor -> {
                    setBackgroundColor();
                    //Log.d("Color", String.format("%f, %f, %f", hslColor.getHue(), hslColor.getSaturation(), hslColor.getLightness()));
                })
                .observeOn(Schedulers.single())
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribe(hslColor -> BluetoothService.sendColorBT(intColor)));
    }

    @Override
    protected void onStop() {
        super.onStop();
        disposables.dispose();
    }

    private void setBackgroundColor() {
        intColor = ColorUtils.HSLToColor(new float[]{hslColor.getHue(), hslColor.getSaturation(), hslColor.getLightness()});
        int colorLeft = ColorUtils.HSLToColor(new float[]{(hslColor.getHue() - 20 + 360) % 360, hslColor.getSaturation(), hslColor.getLightness()});
        int colorRight = ColorUtils.HSLToColor(new float[]{(hslColor.getHue() + 20 + 360) % 360, hslColor.getSaturation(), hslColor.getLightness()});
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{colorLeft, intColor, intColor, colorRight});
        colorBackground.setBackground(gradient);
    }
}
