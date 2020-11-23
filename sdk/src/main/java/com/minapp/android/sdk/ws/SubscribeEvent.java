package com.minapp.android.sdk.ws;

import androidx.annotation.Nullable;

public enum SubscribeEvent {

    CREATE("on_create"), UPDATE("on_update"), DELETE("on_delete");

    public final String event;

    SubscribeEvent(String event) {
        this.event = event;
    }

    public static @Nullable SubscribeEvent from(String str) {
        if (CREATE.event.equalsIgnoreCase(str))
            return CREATE;
        if (UPDATE.event.equalsIgnoreCase(str))
            return UPDATE;
        if (DELETE.event.equalsIgnoreCase(str))
            return DELETE;
        return null;
    }
}
