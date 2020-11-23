package com.minapp.android.sdk.ws.exceptions;

public class BeforeConnectException extends RuntimeException {

    public BeforeConnectException() {}

    public BeforeConnectException(String message) {
        super(message);
    }

    public BeforeConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeforeConnectException(Throwable cause) {
        super(cause);
    }
}
