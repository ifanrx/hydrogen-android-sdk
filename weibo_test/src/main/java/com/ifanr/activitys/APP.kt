package com.ifanr.activitys

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import com.minapp.android.sdk.BaaS

class APP: Application() {

    override fun onCreate() {
        super.onCreate()

        // TODO 初始化
        BaaS.init("a4d2d62965ddb57fa4d6", this)
        BaaS.initWeiboComponent(this, "1440651666", "http://sns.whalecloud.com/sina2/callback", "email")
    }
}

val workerHandler by lazy {
    val worker = HandlerThread("app_worker")
    worker.start()
    Handler(worker.looper)
}