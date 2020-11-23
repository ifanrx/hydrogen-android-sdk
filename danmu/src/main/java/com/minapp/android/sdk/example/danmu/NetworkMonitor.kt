package com.minapp.android.sdk.example.danmu

import android.content.Context
import android.util.Log
import com.minapp.android.sdk.util.Util

class NetworkMonitor(
    private val ctx: Context
) : Thread("NetworkMonitor"){

    companion object {
        private const val TAG = "NetworkMonitor"
    }

    @Volatile
    var exit = false

    override fun run() {
        while (!exit) {
            Log.d(TAG, "${Util.isNetworkAvailable(ctx)}")
            sleep(500)
        }
    }
}