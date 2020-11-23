package com.minapp.android.sdk.ws.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

public class JsonObjectSerializer extends StdSerializer<JsonObject> {

    public JsonObjectSerializer() {
        super(JsonObject.class, false);
    }

    @Override
    public void serialize(JsonObject value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        JsonElement prop = null;
        for (String key : value.keySet()) {
            if (key != null) {
                prop = value.get(key);
                gen.writeFieldName(key);
                if (prop == null) {
                    gen.writeNull();
                } else {
                    provider.findValueSerializer(JsonElement.class).serialize(prop, gen, provider);
                }
            }
        }
        gen.writeEndObject();
    }
}
