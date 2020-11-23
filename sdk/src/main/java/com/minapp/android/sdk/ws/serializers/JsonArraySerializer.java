package com.minapp.android.sdk.ws.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.IOException;

public class JsonArraySerializer extends StdSerializer<JsonArray> {

    public JsonArraySerializer() {
        super(JsonArray.class, false);
    }

    @Override
    public void serialize(JsonArray value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartArray();
        for (JsonElement elem : value) {
            provider.findValueSerializer(JsonElement.class).serialize(elem, gen, provider);
        }
        gen.writeEndArray();
    }
}
