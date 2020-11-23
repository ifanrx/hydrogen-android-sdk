package com.minapp.android.sdk.example.danmu

import android.util.Log
import com.minapp.android.sdk.database.Table

class DanmakuProducerThread: Thread("DanmakuProducer") {

    companion object {
        private val SOURCE = arrayOf(
            "厉害了我的弹幕君！", "_(•̀ω•́ 」∠)_见一次进一次", "高级弹幕合影", "卡的我手机都不动了，额????",
            "尾部有洛殿和音", "一个耳朵BOX另一个van是耳机不行了吗", "飞机………", "厉害了",
            "神弹幕再次合影留念（天依蓝）", "前方弹幕核能预警", "抖腿抖得都要痉挛了~~~",
            "(●´ϖ`●)卡的我忍不住发了条弹幕", "ㄟ( ▔, ▔ )ㄏ给您拜年了",
            "????????????????????????????????????☕️☕️????????????????????????????????",
            "动次打次动词打次", "动次打次动词打次", "二营长，我他娘的意大利炮呢", "┻━┻︵╰(‵□′)╯︵┻━┻",
            "我不是看见了天依吗？天依哪去了", "-＝ ∧ ∧ 我来啦-＝と( •∀•)　-＝/ と_ノ-＝_/／⌒ノ",
            "我有一句妈卖批一定要讲", "我从未见过有如此厚颜无耻之人", "诸君快看  还有一个灰机",
            "(*・_・)ノ&lt;(＃＃)&gt;彡来个烤红薯冷静一下", "ρθπρρρπθθνπθριππρρθπθρν",
            ".▄█▀█●◖|´⌣`*|◗·˳♪⁎˚♫◖|´⌣`*|◗·˳♪⁎˚♫", "▄█▀█●给跪了", "我tm全程找高级弹幕没找到2333",
            "在这里刷炮姐的滚出去", "我从未见过有如此厚颜无耻之人", "合影乾杯 []~（￣▽￣）~*",
            "合影╭( ′• o •′ )╭☞就是这个人！"
        )
    }


    override fun run() {
        val length = SOURCE.size
        var index = 0
        while (true) {
            try {
                val record = TABLE.createRecord()
                record.put("text", SOURCE[index])
                record.put("time", System.currentTimeMillis())
                record.save()
                Log.d(Const.TAG, "send danmuka success: ${SOURCE[index]}")
            } catch (e: Exception) {
                Log.e(Const.TAG, "send danmuka fail", e)
            }

            index++
            if (index >= length) {
                index = 0
            }

            Thread.sleep(500)
        }
    }

}