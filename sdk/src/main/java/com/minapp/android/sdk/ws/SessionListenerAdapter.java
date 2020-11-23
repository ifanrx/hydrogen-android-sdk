package com.minapp.android.sdk.ws;

import android.util.Log;

import io.crossbar.autobahn.wamp.Session;
import io.crossbar.autobahn.wamp.interfaces.ISession;
import io.crossbar.autobahn.wamp.types.CloseDetails;
import io.crossbar.autobahn.wamp.types.SessionDetails;

public class SessionListenerAdapter implements ISessionListener {

    private static final String TAG = "SessionListenerAdapter";

    /**
     * 建立 socket 连接，发送 http request 请求建立 websocket 连接成功
     * @param session
     */
    @Override
    public void onConnect(Session session) {
        Log.d(TAG, String.format("onConnect(%s)", session.getID()));
    }

    /**
     * {@link #onLeave(Session, CloseDetails)} 之后执行
     * @param session
     * @param wasClean
     */
    @Override
    public void onDisconnect(Session session, boolean wasClean) {
        Log.d(TAG, String.format("onDisconnect(%s)", session.getID()));
    }

    /**
     * 建立 websocket 连接后，发送 HELLO，成功收到 WELCOME 后
     * @param session
     * @param details
     */
    @Override
    public void onJoin(Session session, SessionDetails details) {
        Log.d(TAG, String.format("onJoin(%s)", session.getID()));
    }

    /**
     * 连接失败 or 收到 CLOSE
     * @param session
     * @param details
     */
    @Override
    public void onLeave(Session session, CloseDetails details) {
        Log.d(TAG, String.format("onLeave(%s, %s, %s)",
                session.getID(), details.reason, details.message));
    }

    /**
     * 所有 {@link #onJoin(Session, SessionDetails)} 执行成功后
     * @param session
     */
    @Override
    public void onReady(Session session) {
        Log.d(TAG, String.format("onReady(%s)", session.getID()));
    }

    /**
     * 没找到调用的地方（？）
     * @param session
     * @param message
     */
    @Override
    public void onUserError(Session session, String message) {
        Log.d(TAG, String.format("onUserError(%s, %s)", session.getID(), message));
    }
}
