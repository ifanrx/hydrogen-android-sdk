package com.minapp.android.sdk.example.danmu

import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser

class AppDanmukuParser: BaseDanmakuParser() {

    override fun parse(): IDanmakus {
        return Danmakus()
    }
}