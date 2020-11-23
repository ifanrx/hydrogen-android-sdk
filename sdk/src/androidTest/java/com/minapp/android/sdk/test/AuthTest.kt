package com.minapp.android.sdk.test

import com.minapp.android.sdk.auth.Auth
import com.minapp.android.sdk.auth.model.SignInWithPhoneRequest
import com.minapp.android.sdk.exception.HttpException
import com.minapp.android.sdk.test.base.BaseAndroidTest
import com.minapp.android.sdk.test.util.Util
import org.junit.*
import org.junit.Assert.*
import org.junit.runners.MethodSorters

/**
 * 这里要对执行顺序进行排序，先执行 signUp 后执行 signin
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AuthTest: BaseAndroidTest() {


    companion object {

        private lateinit var email: String
        private lateinit var username: String
        private lateinit var pwd: String


        @BeforeClass
        @JvmStatic
        fun genParams() {
            email = Util.randomEmail()
            username = Util.randomString()
            pwd = Util.randomString()
        }
    }



    @Before
    fun init() {
        Auth.logout()
    }

    /**
     * 邮箱注册
     */
    @Test
    fun signUpByEmailTest() {
        val user = Auth.signUpWithEmail(email, pwd)
        assertEquals(Auth.currentUserWithoutData()!!.userId, user.userId)
    }

    /**
     * 用户名注册
     */
    @Test
    fun signUpByUsername() {
        val user = Auth.signUpWithUsername(username, pwd)
        assertEquals(Auth.currentUserWithoutData()!!.userId, user.userId)
    }

    /**
     * 邮箱注册
     */
    @Test
    fun signinByEmail() {
        val user = Auth.signInWithEmail(email, pwd)
        assertEquals(Auth.currentUserWithoutData()!!.userId, user.userId)
    }

    /**
     * 用户名登录
     */
    @Test
    fun signinByUsername() {
        val user = Auth.signInWithUsername(username, pwd)
        assertEquals(Auth.currentUserWithoutData()!!.userId, user.userId)
    }

    /**
     * 匿名登录
     */
    @Test
    fun signinAnonymous() {
        Auth.signInAnonymous()
        assertTrue(Auth.isAnonymous())
    }

    /**
     * 登出
     */
    @Test
    fun signoutTest() {
        signinByEmail()
        Auth.logout()
        assertFalse(Auth.signedIn())
    }

    /**
     * 判断是否登录
     */
    @Test
    fun signedInTest() {
        signinByEmail()
        assertTrue(Auth.signedIn())
    }

    /**
     * 使用手机号 + 短信验证码登录
     * 测试无效的验证码抛出 HttpException
     */
    @Test(expected = HttpException::class)
    fun signInByPhoneTest() {
        val req = SignInWithPhoneRequest(
            "12345678912",
            "987678"
        )
        Auth.signInWithPhone(req)
    }
}