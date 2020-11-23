package com.minapp.android.sdk.test.base

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minapp.android.sdk.test.util.TestConst
import org.junit.After
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseTest {

    companion object {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val app: Application = ApplicationProvider.getApplicationContext()
    }

    @After
    fun delay() {
        Thread.sleep(TestConst.NETWORK_DELAY)
    }

}