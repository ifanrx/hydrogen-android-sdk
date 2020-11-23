package com.minapp.android.sdk.util;

import androidx.annotation.NonNull;

public abstract class LazyProperty<T> {

    private T single = null;

    public @NonNull T get() {
        if (single == null) {
            synchronized (LazyProperty.class) {
                if (single == null) {
                    single = newInstance();
                }
            }
        }
        return single;
    }

    protected abstract @NonNull T newInstance();
}
