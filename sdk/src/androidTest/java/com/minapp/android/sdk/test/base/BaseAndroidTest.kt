package com.minapp.android.sdk.test.base

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minapp.android.sdk.BaaS
import com.minapp.android.sdk.test.TestConst
import org.junit.After
import org.junit.BeforeClass
import org.junit.runner.RunWith

abstract class BaseAndroidTest: BaseTest() {
    companion object {

        @BeforeClass
        @JvmStatic
        fun registerSdk() {
            BaaS.init(
                TestConst.BAAS_CLIENT_ID,
                app
            )
        }
    }
}