package com.minapp.android.sdk.ws.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.IOException;

public class JsonElementSerializer extends StdSerializer<JsonElement> {

    public JsonElementSerializer() {
        super(JsonElement.class, false);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, JsonElement value) {
        return value == null || value instanceof JsonNull;
    }

    @Override
    public void serialize(JsonElement value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null || value instanceof JsonNull) {
            gen.writeNull();
        } else if (value instanceof JsonPrimitive) {
            provider.findValueSerializer(JsonPrimitive.class).serialize(value, gen, provider);
        } else if (value instanceof JsonObject) {
            provider.findValueSerializer(JsonObject.class).serialize(value, gen, provider);
        } else if (value instanceof JsonArray) {
            provider.findValueSerializer(JsonArray.class).serialize(value, gen, provider);
        } else {
            throw new IllegalStateException(String.format(
                    "invalid JsonElement type %s", value.getClass().getCanonicalName()));
        }
    }
}
