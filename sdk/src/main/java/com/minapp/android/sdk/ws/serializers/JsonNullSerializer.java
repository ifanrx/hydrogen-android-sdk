package com.minapp.android.sdk.ws.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.gson.JsonNull;

import java.io.IOException;

public class JsonNullSerializer extends StdSerializer<JsonNull> {

    public JsonNullSerializer() {
        super(JsonNull.class, false);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, JsonNull value) {
        return true;
    }

    @Override
    public void serialize(JsonNull value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeNull();
    }
}
