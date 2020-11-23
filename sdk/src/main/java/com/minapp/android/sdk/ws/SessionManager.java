package com.minapp.android.sdk.ws;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.minapp.android.sdk.database.Table;
import com.minapp.android.sdk.database.query.Query;
import com.minapp.android.sdk.database.query.Where;
import com.minapp.android.sdk.exception.UnauthorizedException;
import com.minapp.android.sdk.util.HandlerExecutor;
import com.minapp.android.sdk.util.LazyProperty;
import com.minapp.android.sdk.util.Util;
import com.minapp.android.sdk.ws.exceptions.BeforeConnectException;
import com.minapp.android.sdk.ws.exceptions.LostTransportException;
import com.minapp.android.sdk.ws.exceptions.UserErrorException;
import com.minapp.android.sdk.ws.exceptions.WampCloseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.interfaces.TriConsumer;
import io.crossbar.autobahn.wamp.messages.Hello;
import io.crossbar.autobahn.wamp.messages.Subscribe;
import io.crossbar.autobahn.wamp.messages.Subscribed;
import io.crossbar.autobahn.wamp.messages.Welcome;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.EventDetails;
import io.crossbar.autobahn.wamp.types.SessionDetails;
import io.crossbar.autobahn.wamp.types.SubscribeOptions;

/**
 * WAMP 有两种模式：Pub/Sub（发布/订阅）和 RPC（远程方法调用），这里使用 Pub/Sub 模式
 *
 * Pub/Sub 模式下有三种角色：Subscribers（订阅者），Publishers（发布者）和 Broker（经纪人）；客户端只涉及
 * Subscribers（客户端）和 Broker（服务端）这两种角色
 *
 * WAMP 是应用层协议，需要通过「Handshake」建立连接/会话；客户端发送 HELLO，服务端返回 WELCOME 就表示握手成功，
 * 这一过程由 {@link Session} 完成
 *
 * Subscriber 和 Broker 之间通过 Message 沟通，WAMP 定义了很多 Message 类型：{@link Hello},
 * {@link Welcome}, {@link Subscribe}, {@link Subscribed} 等等，都在包
 * io.crossbar.autobahn.wamp.messages 下面，对应 WAMP 的各种类型的消息
 *
 * 1）WebSocketReader 是 wamp io 读线程，当发生网络被断开的情况时（WLAN 开关被关闭、app 进入后台后被断开网络等），
 * 抛出 ConnectionLost(null)；ping/pong 心跳超时，抛出 ConnectionLost("WebSocket ping timed out.")
 * 2）进入 WebSocketConnection#failConnection(CLOSE_CONNECTION_LOST, reason)
 * 3）关闭 socket、读线程、写线程，进入 WebSocketConnection.onClose(int, reason)
 * 4）开启 reconnect 任务，进入 IWebSocketConnectionHandler.onClose(CLOSE_RECONNECT, reason)
 * 5）进入 WebSocket.connect(...) 里的匿名实现 onClose(CLOSE_RECONNECT, reason)
 * 6）回调 {@link ISession.OnLeaveListener#onLeave(Session, CloseDetails)} 和
 * {@link ISession.OnDisconnectListener#onDisconnect(Session, boolean)}，但这两个回调都无法知晓是 reconnect
 *
 *
 * Reconnect（重连接）
 * reconnect 的逻辑在 WebSocketConnection.scheduleReconnect 和 WebSocketConnection.reconnect
 * 触发路径是：WebSocketConnection.onClose -> CannotConnect/ConnectionLost/Close(invalid close)/
 * Error（CLOSE_INTERNAL_ERROR，当网络断开时抛出 SSLException）
 *
 *
 *
 */
public class SessionManager implements IWampSessionListener {

    public static volatile boolean ENABLE_RECONNECT = true;

    private static final LazyProperty<SessionManager> SINGLETON =
            new LazyProperty<SessionManager>() {
                @NonNull
                @Override
                protected SessionManager newInstance() {
                    return new SessionManager();
                }
            };

    public static @NonNull SessionManager get() {
        return SINGLETON.get();
    }


    private final Queue<SubscribeRequest> requests = new ConcurrentLinkedQueue<>();

    private final LazyProperty<Handler> worker = new LazyProperty<Handler>() {
        @NonNull
        @Override
        protected Handler newInstance() {
            HandlerThread worker = new HandlerThread("SessionManager");
            worker.start();
            return new Handler(worker.getLooper());
        }
    };

    private final LazyProperty<WampSession> session = new LazyProperty<WampSession>() {
        @NonNull
        @Override
        protected WampSession newInstance() {
            WampSession session = new WampSession(worker.get());
            session.addListener(SessionManager.this);
            return session;
        }
    };

    private SessionManager() {}

    @AnyThread
    public @NonNull
    WampSubscription subscribe(
            @NonNull Table table, @NonNull Query query, @NonNull SubscribeEvent event,
            @NonNull SubscribeCallback cb) {
        final SubscribeRequest request = new SubscribeRequest(table.getTableName(), query, event, cb);
        requests.add(request);
        worker.get().post(new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    session.get().subscribe(new SubscribeRequest[]{request});
                } else {
                    session.get().connect();
                }
            }
        });
        return new WampSubscription(request, this);
    }


    public boolean isConnected() {
        return session.get().isConnected();
    }


    public void clearSubscribers() {
        removeRequest(copyRequests());
    }


    /**
     * 正常关闭连接，取消订阅
     * @param session
     * @param wasClean
     */
    @Override
    public void onDisconnect(Session session, boolean wasClean) {
        long sessionId = session != null ? session.getID() : -1;
        Log.d(WsConst.TAG, String.format("session(%s) onDisconnect", sessionId));
    }


    @Override
    public void onLeave(Session session, CloseDetails details) {
        long sessionId = session != null ? session.getID() : -1;
        Log.d(WsConst.TAG, String.format("session(%s) onLeave, reason: %s, message: %s",
                sessionId, details.reason, details.message));

        Throwable tr;
        if (WsConst.WAMP_ERROR_NOT_AUTHORIZED.equalsIgnoreCase(details.reason)) {
            tr = new UnauthorizedException(details.message);
        } else {
            tr = new WampCloseException(details.reason, details.message);
        }
        dispatchException(tr);
    }

    @Override
    public void onReady(Session session) {
        this.session.get().subscribe(copyRequests());
        long sessionId = session != null ? session.getID() : -1;
        Log.d(WsConst.TAG, String.format("session(%s) onReady", sessionId));
    }

    /**
     * 执行网络请求前（建立 session 前）就发生了异常
     * @param tr
     */
    @Override
    public void onExceptionBeforeConnection(Throwable tr) {
        dispatchException(tr);
    }

    private void dispatchException(Throwable tr) {
        SubscribeRequest[] requests = copyRequests();
        for (SubscribeRequest request : requests) {
            request.cb.onError(tr);
        }
        removeRequest(requests);
    }

    void removeRequest(SubscribeRequest[] requests) {
        worker.get().post(new Runnable() {
            @Override
            public void run() {
                SessionManager.this.requests.removeAll(Lists.newArrayList(requests));
                for (SubscribeRequest request : requests) {
                    session.get().unsubscribe(request);
                }

                if (SessionManager.this.requests.isEmpty())
                    session.get().disconnect();
            }
        });
    }

    boolean containRequest(@NonNull SubscribeRequest request) {
        return requests.contains(request);
    }

    SubscribeRequest[] copyRequests() {
        SubscribeRequest[] requests = new SubscribeRequest[this.requests.size()];
        requests = this.requests.toArray(requests);
        return requests;
    }

    @Override
    public void onUserError(Session session, String msg) {
        long sessionId = session != null ? session.getID() : -1;
        Log.d(WsConst.TAG, String.format("session(%s) onUserError, %s", sessionId, msg));
    }

    @Override
    public void onJoin(Session session, SessionDetails details) {
        long sessionId = session != null ? session.getID() : -1;
        Log.d(WsConst.TAG, String.format("session(%s) onJoin", sessionId));
    }


    @Override
    public void onConnect(Session session) {
        long sessionId = session != null ? session.getID() : -1;
        Log.d(WsConst.TAG, String.format("session(%s) onConnect", sessionId));
    }
}
