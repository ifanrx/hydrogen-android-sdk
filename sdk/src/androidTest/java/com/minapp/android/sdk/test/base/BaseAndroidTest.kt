package com.minapp.android.sdk.test.base

import com.minapp.android.sdk.BaaS
import com.minapp.android.sdk.test.util.TestConst
import org.junit.BeforeClass

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