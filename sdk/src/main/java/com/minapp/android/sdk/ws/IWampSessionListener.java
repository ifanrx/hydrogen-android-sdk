package com.minapp.android.sdk.ws;

import com.minapp.android.sdk.database.query.Query;

import io.crossbar.autobahn.wamp.types.Subscription;

interface IWampSessionListener extends ISessionListener {

    /**
     * 在网络连接前，发生了异常
     * @param tr
     */
    void onExceptionBeforeConnection(Throwable tr);

}
