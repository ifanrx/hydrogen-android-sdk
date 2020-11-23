package com.minapp.android.sdk.ws;

import android.util.Log;

import androidx.annotation.NonNull;

public class SafeSubscribeCallbackAdapter implements SubscribeCallback {

    private SubscribeCallback impl = null;

    public SafeSubscribeCallbackAdapter(@NonNull SubscribeCallback impl) {
        this.impl = impl;
    }

    @Override
    public void onInit() {
        if (impl != null) {
            try {
                impl.onInit();
            } catch (Exception e) {
                Log.e(WsConst.TAG, "exception in SubscribeCallback.init", e);
            }
        }
    }

    @Override
    public void onEvent(@NonNull SubscribeEventData event) {
        if (impl != null) {
            try {
                impl.onEvent(event);
            } catch (Exception e) {
                Log.e(WsConst.TAG, "exception in SubscribeCallback.onEvent", e);
            }
        }
    }

    @Override
    public void onError(@NonNull Throwable tr) {
        if (impl != null) {
            try {
                impl.onError(tr);
            } catch (Exception e) {
                Log.e(WsConst.TAG, "exception in SubscribeCallback.onError", e);
            }
        }
    }
}
