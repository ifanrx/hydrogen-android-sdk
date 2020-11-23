package com.minapp.android.sdk.example.danmu

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.minapp.android.sdk.auth.Auth
import com.minapp.android.sdk.database.Record
import com.minapp.android.sdk.ws.SubscribeEventData
import kotlinx.android.synthetic.main.activity_main.*
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext

class MainActivity : AppCompatActivity() {

    private val danmakuConsumerThread = object : DanmakuConsumerThread() {
        override fun onEvent(event: SubscribeEventData) {
            val record = event.after ?: return
            Log.d(Const.TAG, "consume dammuka: ${record.getString("text")}")
            MAIN_HANDLER.post {
                if (!isDestroyed) {
                    addDanmaku(record)
                }
            }
        }
    }

    private val danmakuContext by lazy {
        DanmakuContext.create().apply {
            setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3f)
            isDuplicateMergingEnabled = false
            setScrollSpeedFactor(1.2f)
            setScaleTextSize(1.2f)
        }
    }

    private val networkMonitor by lazy {
        NetworkMonitor(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        danmukaView.setCallback(object : DrawHandler.Callback {
            override fun drawingFinished() {}
            override fun danmakuShown(danmaku: BaseDanmaku?) {}
            override fun updateTimer(timer: DanmakuTimer?) {}

            override fun prepared() {
                danmukaView.start()
                signIn()
            }
        })

        danmukaView.prepare(AppDanmukuParser(), danmakuContext)
        danmukaView.showFPS(true)
        danmukaView.enableDanmakuDrawingCache(true)
    }

    private fun signIn() {
        WORKER_HANDLER.post {
            try {
                if (!Auth.signedIn()) {
                    val email = Util.randomEmail()
                    val pwd = Util.randomString()
                    try {
                        Auth.signInWithEmail(email, pwd)
                    } catch (e: Exception) {}
                    Auth.signUpWithEmail(email, pwd)
                }
                danmakuConsumerThread.start()
            } catch (e: Exception) {
                Log.e(Const.TAG, e.message, e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (danmukaView.isPrepared && danmukaView.isPaused) {
            danmukaView.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (danmukaView.isPrepared) {
            danmukaView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        danmukaView.release()
        networkMonitor.exit = true
    }

    private fun addDanmaku(record: Record) {
        val danmaku = danmakuContext.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL)
        danmaku.text = record.getString("text")
        danmaku.padding = 5
        danmaku.priority = 0 // 可能会被各种过滤器过滤并隐藏显示
        danmaku.isLive = true
        danmaku.time = danmukaView.currentTime + 1200
        danmaku.textSize = resources.displayMetrics.density * 16
        danmaku.textColor = Color.WHITE
        danmukaView.addDanmaku(danmaku)
    }
}
