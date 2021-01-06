package com.minapp.android.sdk.util;

import androidx.annotation.NonNull;

import com.minapp.android.sdk.Assert;

public abstract class DoubleCheckProvider<T> implements Provider<T> {

    private Object object;
    private final Object lock;

    public DoubleCheckProvider(@NonNull Object lock) {
        Assert.notNull(lock, "lock");
        this.lock = lock;
    }

    @Override
    public T get() {
        if (object == null) {
            synchronized (lock) {
                if (object == null) {
                    object = create();
                }
            }
        }
        return (T) object;
    }

    public abstract T create();
}
