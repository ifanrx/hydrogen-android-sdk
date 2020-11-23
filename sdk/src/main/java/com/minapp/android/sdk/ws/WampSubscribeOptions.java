package com.minapp.android.sdk.ws;

import androidx.annotation.NonNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.minapp.android.sdk.Assert;
import com.minapp.android.sdk.Global;
import com.minapp.android.sdk.database.query.Query;

import java.util.Set;

import io.crossbar.autobahn.wamp.types.SubscribeOptions;

class WampSubscribeOptions extends SubscribeOptions {

    private static final String WHERE = "where";
    private static final String AND = "$and";
    private static final String OR = "$or";

    WampSubscribeOptions(@NonNull Query query) {
        putAll(query);
        makeCompatibility();
    }

    /**
     * 1）当查询条件只有一个时：{"id":{"$eq":12}}，需要改成：{"$and":[{"id":{"$eq":12}}]}
     * 2）确保 where 映射为一个 JsonObject 对象（自定义 jackson 的 JsonObject 序列化器）
     */
    private void makeCompatibility() {
        Object value = get(WHERE);
        if (value == null) {
            put(WHERE, new JsonObject());
            return;
        }

        JsonObject where = null;
        try {
            where = Global.gson().fromJson((String) value, JsonObject.class);
            Assert.notNull(where, "where in WampSubscribeOptions");
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("%s must be a json string instance", WHERE), e);
        }

        Set<String> keys = where.keySet();
        if (keys.size() == 1) {
            String key = keys.iterator().next();
            if (!AND.equalsIgnoreCase(key) && !OR.equalsIgnoreCase(key)) {
                JsonArray array = new JsonArray(1);
                array.add(where);
                where = new JsonObject();
                where.add(AND, array);
                put(WHERE, where);
            }
        }
        put(WHERE, where);
    }


}
