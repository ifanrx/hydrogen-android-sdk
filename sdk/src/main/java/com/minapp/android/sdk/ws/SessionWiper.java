package com.minapp.android.sdk.ws;

import androidx.annotation.NonNull;

import com.minapp.android.sdk.util.LazyProperty;

import java.lang.reflect.Field;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;

abstract class SessionWiper {

    private static class LazyField extends LazyProperty<Field> {

        private final String fieldName;

        LazyField(String fieldName) {
            this.fieldName = fieldName;
        }

        @NonNull
        @Override
        protected Field newInstance() {
            try {
                Field transport = Session.class.getDeclaredField(fieldName);
                transport.setAccessible(true);
                return transport;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final LazyField TRANSPORT = new LazyField("mTransport");
    private static final LazyField SERIALIZER = new LazyField("mSerializer");
    private static final LazyField STATE = new LazyField("mState");
    private static final LazyField ID = new LazyField("mSessionID");


    static void wipe(ITransportHandler handler) throws Exception {
        ITransport transport = (ITransport) TRANSPORT.get().get(handler);
        if (transport != null) {
            transport.close();
        }
        TRANSPORT.get().set(handler, null);
        SERIALIZER.get().set(handler, null);
        STATE.get().setInt(handler, 1);
        ID.get().setLong(handler, 0);
    }
}
