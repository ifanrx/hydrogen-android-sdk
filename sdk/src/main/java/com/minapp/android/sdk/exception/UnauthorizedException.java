package com.minapp.android.sdk.exception;

public class UnauthorizedException extends SdkException {

    static final int CODE = 401;
    static final String MESSAGE = "Unauthorized";


    public UnauthorizedException() {
        super(CODE, MESSAGE);
    }

    public UnauthorizedException(String message) {
        super(CODE, message);
    }

    public UnauthorizedException(Throwable cause) {
        super(CODE, MESSAGE, cause);
    }
}
