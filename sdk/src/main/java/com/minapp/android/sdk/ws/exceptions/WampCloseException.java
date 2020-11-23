package com.minapp.android.sdk.ws.exceptions;

import androidx.annotation.Nullable;

public class WampCloseException extends RuntimeException {

    public final @Nullable String reason;
    public final @Nullable String message;

    public WampCloseException(String reason, String message) {
        super(message);
        this.reason = reason;
        this.message = message;
    }
}
