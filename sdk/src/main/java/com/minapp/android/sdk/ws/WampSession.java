package com.minapp.android.sdk.ws;

import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.minapp.android.sdk.Config;
import com.minapp.android.sdk.Const;
import com.minapp.android.sdk.auth.Auth;
import com.minapp.android.sdk.exception.SessionMissingException;
import com.minapp.android.sdk.exception.UninitializedException;
import com.minapp.android.sdk.util.HandlerExecutor;
import com.minapp.android.sdk.util.LazyProperty;
import com.minapp.android.sdk.util.Util;
import com.minapp.android.sdk.ws.exceptions.SubscribeErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

import io.crossbar.autobahn.wamp.Client;
import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.types.ExitInfo;
import io.crossbar.autobahn.wamp.types.Subscription;
import io.crossbar.autobahn.wamp.types.TransportOptions;

/**
 * 对 {@link Session} 和 {@link Client} 的封装
 */
class WampSession implements ReConnectWebSocketTransport.IReconnectListener {

    private final Handler handler;
    private final List<ISessionListener> listeners = new ArrayList<>();
    private volatile Session session = null;

    private final LazyProperty<SessionManager> sessionManager =
            new LazyProperty<SessionManager>() {
                @NonNull
                @Override
                protected SessionManager newInstance() {
                    return SessionManager.get();
                }
            };

    private ReConnectWebSocketTransport transport = null;

    WampSession(@NonNull Handler handler) {
        this.handler = handler;
    }

    void unsubscribe(SubscribeRequest request) {
        if (request.subscription != null) {

            // 订阅中，取消订阅
            if (request.subscription == SubscribeRequest.DOING_SUBSCRIBE) {
                request.subscription = null;
            } else {

                // 已订阅，取消订阅
                try {
                    request.subscription.unsubscribe();
                } catch (Exception ignored) {}
                request.subscription = null;
            }
        }
    }

    void subscribe(SubscribeRequest[] requests) {
        for (SubscribeRequest request : requests) {
            if (request.subscription == null) {
                subscribe(request);
            }
        }
    }

    private void subscribe(SubscribeRequest request) {
        Session session = this.session;
        if (session == null || request.subscription != null)
            return;

        String topic = String.format("%s.%s.%s",
                Const.WAMP_TOPIC_PREFIX, request.tableName, request.event.event);
        WampSubscribeOptions options = new WampSubscribeOptions(request.query);
        request.subscription = SubscribeRequest.DOING_SUBSCRIBE;
        session.subscribe(topic, request, options).whenComplete(new BiConsumer<Subscription, Throwable>() {
            @Override
            public void accept(Subscription subscription, Throwable throwable) {
                if (subscription != null && throwable == null) {

                    // 此时，request 应该是订阅中的状态；否则可能是在订阅过程中取消了订阅，或者被覆盖了订阅，
                    // 无论哪种情况，都应该取消当前订阅
                    if (request.subscription == SubscribeRequest.DOING_SUBSCRIBE) {
                        request.subscription = subscription;

                        // init once
                        if (!request.initInvoked) {
                            request.cb.onInit();
                            request.initInvoked = true;
                        }
                    } else {
                        subscription.unsubscribe();
                    }

                    // 订阅失败
                } else if (throwable != null) {
                    request.cb.onError(new SubscribeErrorException(throwable));
                    sessionManager.get().removeRequest(new SubscribeRequest[]{request});
                }
            }
        });
    }

    void addListener(ISessionListener listener) {
        if (listener == null)
            return;
        listeners.add(listener);
    }

    public void removeListener(ISessionListener listener) {
        if (listener == null)
            return;
        listeners.remove(listener);
    }

    void connect() {
        if (session == null)
            _connect();
    }

    void disconnect() {
        if (session != null) {
            if (session.isConnected()) {
                session.leave(WsConst.DISCONNECT_WHEN_NO_SUBSCRIBER);
            } else {
                session.addOnConnectListener(new ISession.OnConnectListener() {
                    @Override
                    public void onConnect(Session session) {
                        session.leave(WsConst.DISCONNECT_WHEN_NO_SUBSCRIBER);
                    }
                });
                session = null;
            }
            session = null;
        }

        if (transport != null) {
            transport.reconnectListener = null;
        }
        transport = null;
    }

    boolean isConnected() {
        return session != null && session.isConnected();
    }

    private void _connect() {
        String clientId = null;
        String token = null;
        String envId = null;
        try {
            clientId = Config.getClientId();
            if (clientId == null)
                throw new UninitializedException();
            token = Auth.token();
            if (token == null)
                throw new SessionMissingException();
            envId = Config.getEnvId();
        } catch (Exception e) {
            dispatchExceptionBeforeConnection(e);
            return;
        }

        Uri.Builder pathBuilder = Uri.parse(Const.WAMP_DEFAULT_PATH).buildUpon();
        pathBuilder.appendQueryParameter(Const.HTTP_HEADER_CLIENT_ID.toLowerCase(), clientId);
        pathBuilder.appendQueryParameter(Const.HTTP_HEADER_AUTH.toLowerCase(),
                Const.HTTP_HEADER_AUTH_PREFIX + token);
        if (!Util.isNullOrEmpty(envId))
            pathBuilder.appendQueryParameter(Const.HTTP_HEADER_ENV, envId);
        String path = pathBuilder.build().toString();

        Session session = new Session(new HandlerExecutor(handler));
        addListenersToSession(session);
        this.session = session;

        // 立刻起另一个 thread 执行网络连接，在网络连接前会执行参数的检查，如果参数检查失败会在
        // CompletableFuture 抛出异常；如果抛出异常，Session 和 Client 都要重新构建
        transport = new ReConnectWebSocketTransport(path, handler);
        transport.reconnectListener = this;
        Client client = new Client(transport);
        client.add(session, Const.WAMP_REALM);
        TransportOptions options = new TransportOptions();
        options.setAutoPingInterval(20);
        options.setAutoPingTimeout(30);
        client.connect(options).whenComplete(new BiConsumer<ExitInfo, Throwable>() {
            @Override
            public void accept(ExitInfo exitInfo, Throwable tr) {
                if (tr != null) {
                    dispatchExceptionBeforeConnection(tr);
                }
            }
        });
    }

    /**
     * 在网络连接前，发生了异常
     * @param tr
     */
    private void dispatchExceptionBeforeConnection(@NonNull Throwable tr) {
        removeListenersFromSession(session);
        session = null;
        for (ISessionListener listener : listeners) {
            if (listener instanceof IWampSessionListener) {
                ((IWampSessionListener) listener).onExceptionBeforeConnection(tr);
            }
        }
    }

    private void addListenersToSession(Session session) {
        if (session == null)
            return;
        for (ISessionListener listener : listeners) {
            session.addOnConnectListener(listener);
            session.addOnDisconnectListener(listener);
            session.addOnJoinListener(listener);
            session.addOnLeaveListener(listener);
            session.addOnUserErrorListener(listener);
            session.addOnReadyListener(listener);
        }
    }

    private void removeListenersFromSession(Session session) {
        if (session == null)
            return;
        for (ISessionListener listener : listeners) {
            session.removeOnConnectListener(listener);
            session.removeOnDisconnectListener(listener);
            session.removeOnJoinListener(listener);
            session.removeOnLeaveListener(listener);
            session.removeOnUserErrorListener(listener);
            session.removeOnReadyListener(listener);
        }
    }

    @Override
    public void onReconnect() {
        SubscribeRequest[] requests = SessionManager.get().copyRequests();
        for (SubscribeRequest request : requests) {
            unsubscribe(request);
        }
    }
}
