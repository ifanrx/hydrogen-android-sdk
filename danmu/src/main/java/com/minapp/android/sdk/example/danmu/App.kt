package com.minapp.android.sdk.example.danmu

import android.app.Application
import com.minapp.android.sdk.BaaS
import io.crossbar.autobahn.Autobahn

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        BaaS.init(Const.CLIENT_ID, this)
        Autobahn.enableDebugLog(true)
    }
}