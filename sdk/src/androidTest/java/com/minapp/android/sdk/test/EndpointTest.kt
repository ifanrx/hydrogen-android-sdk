package com.minapp.android.sdk.test

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.minapp.android.sdk.BaaS
import com.minapp.android.sdk.auth.Auth
import com.minapp.android.sdk.test.util.TestConst
import org.junit.Test
import org.junit.runner.RunWith
import java.net.UnknownHostException

@RunWith(AndroidJUnit4::class)
class EndpointTest {

    companion object {
        private const val OFFICIAL_ENDPOINT = "https://cloud.minapp.com/"
        private const val ERROR_ENDPOINT = "http://Anofficialissomeonewhoholdsanoffice.com"
    }

    /**
     * 正常情况下（填对了服务端域名），能匿名登录
     */
    @Test
    fun officialEndpointTest() {
        BaaS.init(TestConst.BAAS_CLIENT_ID, OFFICIAL_ENDPOINT, ApplicationProvider.getApplicationContext())
        Thread.sleep(2000)
        Auth.signInAnonymous()
    }

    /**
     * 异常情况，这里测试填错域名导致 UnknownHostException，说明自定义的域名生效了
     */
    @Test(expected = UnknownHostException::class)
    fun errorEndpointTest() {
        BaaS.init(TestConst.BAAS_CLIENT_ID, ERROR_ENDPOINT, ApplicationProvider.getApplicationContext())
        Thread.sleep(2000)
        Auth.signInAnonymous()
    }

}