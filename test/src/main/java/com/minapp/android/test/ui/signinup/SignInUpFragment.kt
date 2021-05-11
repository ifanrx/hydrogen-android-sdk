package com.minapp.android.test.ui.signinup

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.minapp.android.sdk.Global
import com.minapp.android.sdk.auth.Auth
import com.minapp.android.sdk.user.User
import com.minapp.android.test.R
import com.minapp.android.test.ext.launchWithIO
import com.minapp.android.test.ext.toast
import com.minapp.android.test.ext.withMainIfActive
import com.minapp.android.test.ui.base.BaseFragment
import com.minapp.android.test.util.Auths
import kotlinx.android.synthetic.main.fragment_signinup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * 登入登出
 */
class SignInUpFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_signinup, container, false)
        return root

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        signInBtn.setOnClickListener {
            signIn(signTypeSpinner.signType, nameTv.text.toString(), pwdTv.text.toString())
        }

        signUpBtn.setOnClickListener {
            signUp(signTypeSpinner.signType, nameTv.text.toString(), pwdTv.text.toString())
        }

        emailVerifyBtn.setOnClickListener { emailVerify() }

        resetPwdBtn.setOnClickListener { resetPwd(nameTv.text.toString()) }

        signOutBtn.setOnClickListener { signOut() }

        printUserBtn.setOnClickListener { printCurrentUser() }
    }

    /**
     * 登出
     */
    private fun signOut() {
        Auth.logout()
        toast("已退出登录")
    }

    /**
     * 打印当前用户信息
     */
    private fun printCurrentUser() {
        launchWithIO {
            try {
                val user = Auths.currentUser
                withMainIfActive { printJson(user) }
            } catch (e: Exception) {
                withMainIfActive {
                    toast("获取个人信息失败，${e.message}")
                    printJson("")
                }
            }
        }
    }

    /**
     * 发送验证邮件
     */
    private fun emailVerify() {
        launchWithIO {
            try {
                check(Auths.currentUser.emailVerify()) { "" }
                withMainIfActive { toast("已发送验证邮件") }
            } catch (e: Exception) {
                withMainIfActive { toast("发送验证邮件失败，${e.message}") }
            }
        }
    }

    /**
     * 发送重置密码邮件
     */
    private fun resetPwd(newEmail: String) {
        launchWithIO {
            try {
                check(Auths.currentUser.resetPwd(newEmail)) { "" }
                withMainIfActive { toast("已发送重置密码邮件") }
            } catch (e: Exception) {
                withMainIfActive { toast("发送重置密码邮件失败，${e.message}") }
            }
        }
    }

    /**
     * 登录
     */
    private fun signIn(type: SignType, name: String, pwd: String) {
        val block: () -> User? = {
            when (type) {
                SignType.EMAIL -> Auth.signInWithEmail(name, pwd)
                SignType.USERNAME -> Auth.signInWithUsername(name, pwd)
                SignType.ANONYMOUS -> {
                    Auth.signInAnonymous()
                    null
                }
            }
        }

        launchWithIO {
            try {
                val user = block.invoke()
                withMainIfActive {
                    toast("登录成功")
                    user?.also { printJson(it) }
                }
            } catch (e: Exception) {
                withMainIfActive {
                    toast("登录失败，${e.message}")
                    printJson("")
                }
            }
        }
    }

    /**
     * 注册
     */
    private fun signUp(type: SignType, name: String, pwd: String) {
        val block: () -> User? = {
            when (type) {
                SignType.EMAIL -> Auth.signUpWithEmail(name, pwd)
                SignType.USERNAME -> Auth.signUpWithUsername(name, pwd)
                SignType.ANONYMOUS -> {
                    throw IllegalArgumentException("无法匿名注册")
                }
            }
        }

        launchWithIO {
            try {
                val user = block.invoke()
                withMainIfActive {
                    toast("注册成功")
                    user?.also { printJson(it) }
                }
            } catch (e: Exception) {
                withMainIfActive {
                    toast("注册失败，${e.message}")
                    printJson("")
                }
            }
        }
    }

    private fun printJson(any: Any) {
        infoTv.text = Global.gsonPrint().toJson(any)
    }

    companion object {
        private const val TAG = "SignInUpFragment"
    }
}