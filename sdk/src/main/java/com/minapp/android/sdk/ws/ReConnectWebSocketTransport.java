package com.minapp.android.sdk.ws;

import android.content.Context;
import android.os.Handler;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.minapp.android.sdk.Global;
import com.minapp.android.sdk.util.Util;
import com.minapp.android.sdk.ws.serializers.JsonArraySerializer;
import com.minapp.android.sdk.ws.serializers.JsonElementSerializer;
import com.minapp.android.sdk.ws.serializers.JsonNullSerializer;
import com.minapp.android.sdk.ws.serializers.JsonObjectSerializer;
import com.minapp.android.sdk.ws.serializers.JsonPrimitiveSerializer;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import io.crossbar.autobahn.utils.ABLogger;
import io.crossbar.autobahn.utils.IABLogger;
import io.crossbar.autobahn.wamp.interfaces.ISerializer;
import io.crossbar.autobahn.wamp.interfaces.ITransport;
import io.crossbar.autobahn.wamp.interfaces.ITransportHandler;
import io.crossbar.autobahn.wamp.serializers.CBORSerializer;
import io.crossbar.autobahn.wamp.serializers.JSONSerializer;
import io.crossbar.autobahn.wamp.serializers.MessagePackSerializer;
import io.crossbar.autobahn.wamp.transports.WebSocket;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.TransportOptions;
import io.crossbar.autobahn.websocket.WebSocketConnection;
import io.crossbar.autobahn.websocket.WebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.interfaces.IWebSocketConnectionHandler;
import io.crossbar.autobahn.websocket.types.ConnectionResponse;
import io.crossbar.autobahn.websocket.types.WebSocketOptions;

/**
 *
 * {@link WebSocket} 的一个复制，添加 reconnect 特性
 * 添加我们的 reconnect 逻辑
 */
class ReConnectWebSocketTransport implements ITransport {

    public static interface IReconnectListener {

        /**
         * 丢失连接，在几秒后即将进行 reconnect
         */
        void onReconnect();

    }

    public static final IABLogger LOGGER = ABLogger.getLogger(
            ReConnectWebSocketTransport.class.getName());
    private static final String[] SERIALIZERS_DEFAULT = new String[] {
            CBORSerializer.NAME, MessagePackSerializer.NAME, JSONSerializer.NAME};

    private final WebSocketConnection mConnection;
    private final String mUri;

    private List<String> mSerializers;
    private ISerializer mSerializer;
    private int mReconnectCount;
    private final long mReconnectDelay = 1000;
    IReconnectListener reconnectListener = null;
    private final Handler mHandler;

    public ReConnectWebSocketTransport(String uri, Handler handler) {
        mUri = uri;
        mConnection = new WebSocketConnection();
        mHandler = handler;
    }

    private String[] getSerializers() {
        if (mSerializers != null) {
            return (String[]) mSerializers.toArray();
        }
        return SERIALIZERS_DEFAULT;
    }

    @Override
    public void send(byte[] payload, boolean isBinary) {
        mConnection.sendMessage(payload, isBinary);
    }

    @Override
    public void connect(ITransportHandler transportHandler) throws Exception {
        connect(transportHandler, new TransportOptions());
    }

    @Override
    public void connect(ITransportHandler transportHandler, TransportOptions options)
            throws Exception {

        WebSocketOptions webSocketOptions = new WebSocketOptions();
        webSocketOptions.setAutoPingInterval(options.getAutoPingInterval());
        webSocketOptions.setAutoPingTimeout(options.getAutoPingTimeout());
        webSocketOptions.setMaxFramePayloadSize(options.getMaxFramePayloadSize());
        // disable reconnect in WebSocketConnection, use mime
        webSocketOptions.setReconnectInterval(-1);

        mConnection.connect(mUri, getSerializers(), new WebSocketConnectionHandler() {

            @Override
            public void onConnect(ConnectionResponse response) {
                LOGGER.d("onConnect");
                LOGGER.d(String.format("Negotiated serializer=%s", response.protocol));
                mReconnectCount = 0;
                try {
                    mSerializer = initializeSerializer(response.protocol);
                } catch (Exception e) {
                    LOGGER.v(e.getMessage(), e);
                }
            }

            @Override
            public void onOpen() {
                try {
                    LOGGER.d("onOpen");
                    transportHandler.onConnect(
                            ReConnectWebSocketTransport.this, mSerializer);
                } catch (Exception e) {
                    LOGGER.v(e.getMessage(), e);
                }
            }

            @Override
            public void onClose(int code, String reason) {
                LOGGER.d("onClose");

                // 尝试 reconnect
                if (scheduleReconnect(code, transportHandler))
                    return;

                String closeReason;
                if (code == IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST) {
                    closeReason = CloseDetails.REASON_TRANSPORT_LOST;
                } else {
                    closeReason = CloseDetails.REASON_DEFAULT;
                }
                transportHandler.onLeave(new CloseDetails(closeReason, null));
                LOGGER.d(String.format("Disconnected, code=%s, reasons=%s", code, reason));
                transportHandler.onDisconnect(
                        code == IWebSocketConnectionHandler.CLOSE_NORMAL || code == 1000);
            }

            @Override
            public void onMessage(String payload) {
                try {
                    transportHandler.onMessage(payload.getBytes(), false);
                } catch (Exception e) {
                    LOGGER.v(e.getMessage(), e);
                }
            }

            @Override
            public void onMessage(byte[] payload, boolean isBinary) {
                try {
                    transportHandler.onMessage(payload, isBinary);
                } catch (Exception e) {
                    LOGGER.v(e.getMessage(), e);
                }
            }
        }, webSocketOptions, null);
    }

    @Override
    public boolean isOpen() {
        return mConnection.isConnected();
    }

    @Override
    public void close() throws Exception {
        mConnection.sendClose();
    }

    @Override
    public void abort() throws Exception {
        mConnection.sendClose();
    }

    @Override
    public void setOptions(TransportOptions options) {
        WebSocketOptions webSocketOptions = new WebSocketOptions();
        webSocketOptions.setAutoPingTimeout(options.getAutoPingTimeout());
        webSocketOptions.setAutoPingInterval(options.getAutoPingInterval());
        mConnection.setOptions(webSocketOptions);
    }

    private ISerializer initializeSerializer(String negotiatedSerializer) throws Exception {
        ISerializer serializer = null;
        switch (negotiatedSerializer) {
            case CBORSerializer.NAME:
                serializer = new CBORSerializer();
                break;

            case JSONSerializer.NAME:
                serializer = new JSONSerializer();
                break;

            case MessagePackSerializer.NAME:
                serializer = new MessagePackSerializer();
                break;

            default:
                throw new IllegalArgumentException("Unsupported serializer.");
        }

        SimpleModule module = new SimpleModule("GSON");
        module.addSerializer(new JsonPrimitiveSerializer());
        module.addSerializer(new JsonNullSerializer());
        module.addSerializer(new JsonObjectSerializer());
        module.addSerializer(new JsonArraySerializer());
        module.addSerializer(new JsonElementSerializer());
        serializer.mapper.registerModule(module);
        return serializer;
    }

    /**
     * reconnect 逻辑
     * @param closeCode
     * @return
     */
    private boolean scheduleReconnect(int closeCode, ITransportHandler session) {

        /**
         * CLOSE_CONNECTION_LOST 发生在：
         * 1）broker 发送非法 CLOSE 消息（close code != 1000）
         * 2）ping/pong timeout
         * 3）read/write thread 发生 SocketException
         * 4）read thread 收到空内容
         *
         * CLOSE_CANNOT_CONNECT 发生在：
         * 1）无法建立 socket 连接
         *
         * CLOSE_INTERNAL_ERROR
         * 当连接被关闭时发生 SSLException（iQOO 3 测试），被 autobahn 归到 CLOSE_INTERNAL_ERROR 了，
         * 所以这里也要捕获这类问题
         *
         */
        if (closeCode == IWebSocketConnectionHandler.CLOSE_CANNOT_CONNECT ||
                closeCode == IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST ||
                closeCode == IWebSocketConnectionHandler.CLOSE_INTERNAL_ERROR) {

            // 开关
            if (!SessionManager.ENABLE_RECONNECT)
                return false;

            /**
             * reconnect 上限为 6，每次间隔 1s，共 6s
             * 后 3 次如果发现没有网络连接则不再 reconnect
             *
             * mReconnectCount 将在连接成功后置为 0
             */
            if (mReconnectCount <= 10) {
                Context ctx = Global.getApplication();
                if (mReconnectCount > 6 && ctx != null && !Util.isNetworkAvailable(ctx)) {
                    return false;
                }

                // 不能让 session 把 onLeave 和 onDisconnect 事件发出去（内部 reconnect）
                // 但是要重置 session
                try {
                    SessionWiper.wipe(session);
                } catch (Exception e) {
                    LOGGER.w("wipe session fail, unable to reconnect", e);
                    return false;
                }

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mConnection.reconnect();
                    }
                }, mReconnectDelay);

                IReconnectListener l = reconnectListener;
                if (l != null) {
                    l.onReconnect();
                }

                mReconnectCount++;
                LOGGER.d("reconnect");
                return true;
            }
        }
        return false;
    }
}
