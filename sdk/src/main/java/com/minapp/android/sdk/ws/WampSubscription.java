package com.minapp.android.sdk.ws;

import com.google.common.collect.Lists;

public class WampSubscription {

    private final SubscribeRequest request;
    private final SessionManager manager;

    public WampSubscription(SubscribeRequest request, SessionManager manager) {
        this.request = request;
        this.manager = manager;
    }

    public void unsubscribe() {
        manager.removeRequest(new SubscribeRequest[]{request});
    }

    public boolean alive() {
        return manager.containRequest(request);
    }
}
