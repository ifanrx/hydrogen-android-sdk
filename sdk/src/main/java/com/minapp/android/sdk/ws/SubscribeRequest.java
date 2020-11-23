package com.minapp.android.sdk.ws;

import com.minapp.android.sdk.database.query.Query;
import com.minapp.android.sdk.util.Util;

import java.util.List;
import java.util.Map;

import io.crossbar.autobahn.wamp.interfaces.TriConsumer;
import io.crossbar.autobahn.wamp.types.EventDetails;
import io.crossbar.autobahn.wamp.types.Subscription;

class SubscribeRequest implements TriConsumer<List<Object>, Map<String, Object>, EventDetails> {

    static final Subscription DOING_SUBSCRIBE = new Subscription(-1L, null,
            null, null, null, null);

    final String tableName;
    final Query query;
    final SubscribeEvent event;
    final SubscribeCallback cb;
    boolean initInvoked = false;

    /**
     * 一个 request 有三种状态：
     * 1）还未进行订阅，== null
     * 2）订阅中，== DOING_SUBSCRIBE
     * 3）已订阅
     */
    Subscription subscription = null;

    SubscribeRequest(String tableName, Query query, SubscribeEvent event,
                            SubscribeCallback cb) {
        this.tableName = tableName;
        this.query = query;
        this.event = event;
        this.cb = new SafeSubscribeCallbackAdapter(cb);
    }

    @Override
    public void accept(List<Object> var1, Map<String, Object> map, EventDetails details) {
        Subscription sub = details != null ? details.subscription : null;
        if (subscription != null && subscription != DOING_SUBSCRIBE && subscription == sub) {
            SubscribeEventData data = new SubscribeEventData(map);
            if (data.event == event) {
                cb.onEvent(data);
            }
        }
    }
}
