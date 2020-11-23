package com.minapp.android.sdk.test.util

import java.util.*

object Util {

    private val CHARS = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        )

    private val EMAIL_PROVIDERS = arrayOf(
        "qq.com", "gmail.com", "foxmail.com", "hotmail.com", "163.com", "126.com", "tom.com"
    )

    private fun randomEmailProvider(): String = EMAIL_PROVIDERS.random()

    fun randomString(length: Int = 12): String =
        String(CharArray(length) { CHARS.random() })

    fun randomEmail(length: Int = 12): String =
        "${randomString(length = length)}@${randomEmailProvider()}"

    fun calendar(year: Int, month: Int, date: Int, hour: Int, minute: Int, second: Int) =
        Calendar.getInstance().apply {
            set(year, month, date, hour, minute, second)
        }

}