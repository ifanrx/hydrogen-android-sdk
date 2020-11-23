package com.minapp.android.sdk.test

import com.minapp.android.sdk.auth.Auth
import com.minapp.android.sdk.auth.CurrentUser
import com.minapp.android.sdk.auth.model.UpdateUserReq
import com.minapp.android.sdk.exception.HttpException
import com.minapp.android.sdk.test.base.BaseAndroidTest
import com.minapp.android.sdk.test.util.Util
import org.junit.After
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class CurrentUserTest: BaseAndroidTest() {

    companion object {

        private const val TAG = "CurrentUserTest"

        private lateinit var email: String
        private lateinit var pwd: String
        private lateinit var user: CurrentUser

        @BeforeClass
        @JvmStatic
        fun signIn() {
            email = Util.randomEmail()
            pwd = Util.randomString()
            Auth.signUpWithEmail(email, pwd)
            user = Auth.currentUser()!!
        }

    }

    @After
    fun after() {
        Thread.sleep(2000)
    }

    /**
     * 更新用户基本资料
     */
    @Test
    fun updateUserInfo() {
        val newEmail = Util.randomEmail()
        val newUsername = Util.randomString()
        val newPwd = Util.randomString()
        val newPhone = "13690339045"

        user.updateUser(UpdateUserReq().apply {
            username = newUsername
            email = newEmail
            newPassword = newPwd
            password = pwd
            phone = newPhone
        })
        Thread.sleep(2000)

        user = Auth.currentUser()!!
        assertEquals(user.email, newEmail)
        assertEquals(user.username, newUsername)
        assertEquals(user.phone, newPhone)
    }

    /**
     * 发送密码重置邮件
     */
    @Test
    fun resetPwdTest() {
        if (user.isEmailVerified) {
            assert(user.resetPwd(user.email))

            // 邮箱未验证的情况下，重置密码应该抛出 HttpException(400)
        } else {
            try {
                user.resetPwd(user.email)
            } catch (e: HttpException) {
                if (e.code == 400)
                    return
            }
            throw IllegalStateException()
        }
    }

    /**
     * 发送验证邮件
     */
    @Test
    fun emailVerifyTest() {
        assert(user.emailVerify())
    }

    /**
     * 使用短信验证码校验手机号码
     */
    @Test(expected = HttpException::class)
    fun smsPhoneVerificationTest() {
        val newPhone = "13690339045"
        user.updateUser(UpdateUserReq().apply {
            phone = newPhone
        })
        Thread.sleep(2000)

        // 错误的验证码，需抛出异常
        user.smsPhoneVerification("456789")
    }
}