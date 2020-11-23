package com.minapp.android.sdk.util;

import android.os.Handler;

import java.util.concurrent.Executor;

public class HandlerExecutor implements Executor {

    private final Handler handler;

    public HandlerExecutor(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void execute(Runnable command) {
        if (command == null)
            return;
        handler.post(command);
    }
}
