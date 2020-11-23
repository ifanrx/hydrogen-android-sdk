package com.minapp.android.sdk.example.danmu

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.minapp.android.sdk.database.Table

val TABLE by lazy { Table("danmu_list") }

val MAIN_HANDLER by lazy {
    Handler(Looper.getMainLooper())
}

val WORKER_HANDLER by lazy {
    val thread = HandlerThread("worker")
    thread.start()
    Handler(thread.looper)
}