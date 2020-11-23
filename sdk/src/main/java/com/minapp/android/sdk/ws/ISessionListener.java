package com.minapp.android.sdk.ws;

import io.crossbar.autobahn.wamp.interfaces.ISession;

interface ISessionListener extends ISession.OnJoinListener, ISession.OnReadyListener,
        ISession.OnLeaveListener, ISession.OnConnectListener, ISession.OnDisconnectListener,
        ISession.OnUserErrorListener {
}
