package com.minapp.android.sdk.example.danmu

import android.util.Log
import com.minapp.android.sdk.ws.SubscribeCallback
import com.minapp.android.sdk.ws.SubscribeEvent
import com.minapp.android.sdk.ws.SubscribeEventData

abstract class DanmakuConsumerThread: Thread("DanmakuConsumer"), SubscribeCallback {

    override fun run() {
        TABLE.subscribe(SubscribeEvent.CREATE, this)
    }

    override fun onInit() {
        DanmakuProducerThread().start()
    }

    override fun onError(tr: Throwable) {
        Log.e(Const.TAG, "consume danmuka fail", tr)
    }
}