package com.tudan.smartlamp.utils;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by mario on 11.10.2017..
 */

public class DisposableUtils {
    public static Disposable getDisposable(Consumer<Void> disposeConsumer){
        return new Disposable() {
            @Override
            public void dispose() {
                try {
                    disposeConsumer.accept(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public boolean isDisposed() {
                return false;
            }
        };
    }
}
