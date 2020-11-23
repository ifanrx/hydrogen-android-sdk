package com.minapp.android.sdk.ws.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.gson.JsonPrimitive;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonPrimitiveSerializer extends StdSerializer<JsonPrimitive> {

    public JsonPrimitiveSerializer() {
        super(JsonPrimitive.class, false);
    }

    @Override
    public void serialize(JsonPrimitive value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            gen.writeNull();
        } else if (value.isBoolean()) {
            gen.writeBoolean(value.getAsBoolean());
        } else if (value.isNumber()) {
            writeNumber(value.getAsNumber(), gen);
        } else if (value.isString()) {
            gen.writeString(value.getAsString());
        } else {
            throw new IllegalStateException(String.format(
                    "unrecognized JsonPrimitive type %s", value.getClass().getCanonicalName()));
        }
    }

    private void writeNumber(Number number, JsonGenerator gen) throws IOException {
        if (number instanceof Short) {
            gen.writeNumber((Short) number);
        } else if (number instanceof Integer) {
            gen.writeNumber((Integer) number);
        } else if (number instanceof Long) {
            gen.writeNumber((Long) number);
        } else if (number instanceof Float) {
            gen.writeNumber((Float) number);
        } else if (number instanceof Double) {
            gen.writeNumber((Double) number);
        } else if (number instanceof BigDecimal) {
            gen.writeNumber((BigDecimal) number);
        } else if (number instanceof BigInteger) {
            gen.writeNumber((BigInteger) number);
        } else if (number instanceof Byte) {
            gen.writeNumber(number.intValue());
        } else {
            gen.writeNumber(number.doubleValue());
        }
    }
}
