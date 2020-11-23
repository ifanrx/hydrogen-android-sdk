package com.minapp.android.sdk.test

import com.minapp.android.sdk.BaaS
import com.minapp.android.sdk.auth.Auth
import com.minapp.android.sdk.database.Table
import com.minapp.android.sdk.database.query.Query
import com.minapp.android.sdk.exception.HttpException
import com.minapp.android.sdk.test.base.BaseTest
import com.minapp.android.sdk.test.util.TestConst
import com.minapp.android.sdk.test.util.Util
import org.junit.AfterClass
import org.junit.Test

/**
 * 测试环境 ID
 */
class EnvTest: BaseTest() {

    companion object {

        @AfterClass
        @JvmStatic
        fun logout() {
            Auth.logout()
        }

        private const val TABLE_IN_TEST = "table_in_test_env"
    }

    /**
     * 设置测试环境 ID，正确的情况
     * [TABLE_IN_TEST] 在测试环境里
     */
    @Test
    fun good() {
        BaaS.init(TestConst.BAAS_CLIENT_ID, null, TestConst.BAAS_ENV_ID, app)

        val email = Util.randomEmail()
        val pwd = Util.randomString()
        try {
            Auth.signUpWithEmail(email, pwd)
        } catch (e: Exception) {}
        Auth.signInWithEmail(email, pwd)

        val table = Table(TABLE_IN_TEST)
        table.count(Query())
    }

    /**
     * 设置测试环境 ID，异常情况
     * [TABLE_IN_TEST] 不在生产环境里
     */
    @Test
    fun bad() {
        BaaS.init(TestConst.BAAS_CLIENT_ID, null, null, app)

        val email = Util.randomEmail()
        val pwd = Util.randomString()
        try {
            Auth.signUpWithEmail(email, pwd)
        } catch (e: Exception) {}
        Auth.signInWithEmail(email, pwd)

        try {
            val table = Table(TABLE_IN_TEST)
            table.count(Query())
        } catch (e: HttpException) {
            if (e.code == 404)
                return
        }
        throw Exception("$TABLE_IN_TEST should be exist in test env")
    }

}