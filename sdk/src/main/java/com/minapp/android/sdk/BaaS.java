package com.minapp.android.sdk;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.minapp.android.sdk.auth.Auth;
import com.minapp.android.sdk.exception.EmptyResponseException;
import com.minapp.android.sdk.exception.HttpException;
import com.minapp.android.sdk.exception.SessionMissingException;
import com.minapp.android.sdk.model.*;
import com.minapp.android.sdk.util.BaseCallback;
import com.minapp.android.sdk.util.Retrofit2CallbackAdapter;
import com.minapp.android.sdk.util.Util;
import com.minapp.android.sdk.wechat.WechatComponent;
import com.minapp.android.sdk.weibo.WeiboComponent;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Callable;

import io.crossbar.autobahn.Autobahn;
import okhttp3.ResponseBody;

public class BaaS {

    private BaaS() {}

    /**
     * 当用户批量操作时，当操作的数据条数操作超过批量操作限制时，则会转换成异步操作，
     * 通过此接口可以查询到批量操作的最终结果
     * @param id
     * @return
     * @throws Exception
     */
    public static BatchOperationResp queryBatchOperation(int id) throws Exception {
        return Global.httpApi().queryBatchOperation(id).execute().body();
    }

    /**
     *
     * @param id
     * @param cb
     * @see #queryBatchOperation(int)
     */
    public static void queryBatchOperationInBackground(int id, BaseCallback<BatchOperationResp> cb) {
        Util.inBackground(cb, new Callable<BatchOperationResp>() {
            @Override
            public BatchOperationResp call() throws Exception {
                return Global.httpApi().queryBatchOperation(id).execute().body();
            }
        });
    }

    /**
     * 完成 sdk 的初始化
     * @param clientId      ID 为知晓云应用的 ClientID，可通过知晓云管理后台进行获取
     * @see #init(String, String, Application)
     */
    public static void init(String clientId, @NonNull Application application) {
        init(clientId, null, application);
    }

    /**
     * 完成 sdk 的初始化
     * @param clientId      ID 为知晓云应用的 ClientID，可通过知晓云管理后台进行获取
     * @param host      设置自定义域名
     */
    public static void init(String clientId, String host, @NonNull Application application) {
        init(clientId, host, null, application);
    }

    /**
     * 完成 sdk 的初始化
     * @param clientId  ID 为知晓云应用的 ClientID，可通过知晓云管理后台进行获取
     * @param host      设置自定义域名
     * @param envId     环境 ID，比如测试环境的 ID
     */
    public static void init(String clientId, String host, String envId, @NonNull Application application) {
        Util.assetNotNull(application);
        Config.setClientId(clientId);
        Config.setEndpoint(host);
        Config.setEnvId(envId);
        Global.setApplicaiton(application);
        Auth.init();
    }

    /**
     * 如果要调用微信相关的 api，则需要初始化微信组件
     */
    public static void initWechatComponent(String appId, Context ctx) {
        WechatComponent.initWechatComponent(appId, ctx);
    }

    /**
     * 如果要调用微博相关的 api，则需要初始化微博组件
     */
    public static void initWeiboComponent(Context ctx, String key, String redirectUrl, String scope) {
        WeiboComponent.init(ctx, key, redirectUrl, scope);
    }


    /**
     * 发送短信验证码
     * http status code：
     * 200:	成功
     * 400:	失败（rate limit 或参数错误）
     * 402:	当前应用已欠费
     * 500:	服务错误
     * @param phone
     * @param cb
     */
    public static void sendSmsCode(String phone, BaseCallback<StatusResp> cb) {
        Global.httpApi().sendSmsCode(new SendSmsCodeReq(phone)).enqueue(new Retrofit2CallbackAdapter<StatusResp>(cb));
    }

    /**
     * sync of [sendSmsCode]
     */
    public static boolean sendSmsCode(String phone)
            throws HttpException, SessionMissingException, EmptyResponseException, IOException {
        return Global.httpApi().sendSmsCode(new SendSmsCodeReq(phone)).execute().body().isOk();
    }

    /**
     * 验证短信验证码
     * http status code:
     * 200:	成功
     * 400:	验证码错误 / 参数错误
     * @param phone
     * @param code
     * @param cb
     */
    public static void verifySmsCode(String phone, String code, BaseCallback<StatusResp> cb) {
        Global.httpApi().verifySmsCode(new VerifySmsCodeReq(phone, code)).enqueue(new Retrofit2CallbackAdapter<StatusResp>(cb));
    }

    /**
     * sync of verifySmsCode
     */
    public static boolean verifySmsCode(String phone, String code)
            throws HttpException, SessionMissingException, EmptyResponseException, IOException {
        return Global.httpApi().verifySmsCode(new VerifySmsCodeReq(phone, code)).execute().body().isOk();
    }

    /**
     * 触发云函数的执行
     * @param funcName 要运行的云函数名
     * @param data     要传进云函数的 event.data
     * @param sync     true 表示同步，false 表示异步
     * @param cb       返回云函数的执行结果
     */
    public static void invokeCloudFunc(String funcName, String data, Boolean sync, BaseCallback<CloudFuncResp> cb) {
        JsonElement json = null;
        try {
            json = Global.gson().fromJson(data, JsonElement.class);
        } catch (Exception e) {}
        Global.httpApi().invokeCloudFunc(new CloudFuncReq(funcName, json, sync)).enqueue(new Retrofit2CallbackAdapter<CloudFuncResp>(cb));
    }

    /**
     * sync of [invokeCloudFunc]
     */
    public static CloudFuncResp invokeCloudFunc(String funcName, String data, Boolean sync)
            throws HttpException, SessionMissingException, EmptyResponseException, IOException {
        JsonElement json = null;
        try {
            json = Global.gson().fromJson(data, JsonElement.class);
        } catch (Exception e) {}
        return Global.httpApi().invokeCloudFunc(new CloudFuncReq(funcName, json, sync)).execute().body();
    }

    /**
     * 通过该接口获取服务器时间，主要有以下应用场景：
     * 1. 用于时间校准
     * 2. 用于数据查
     */
    public static @Nullable Calendar getServerDate() throws Exception {
        ServerDateResp body = Global.httpApi().getServerDate().execute().body();
        return body != null ? body.getTime() : null;
    }

    /**
     * @see #getServerDate()
     * @param cb
     */
    public static void getServerDateInBackground(BaseCallback<Calendar> cb) {
        Util.inBackground(cb, new Callable<Calendar>() {
            @Override
            public Calendar call() throws Exception {
                return getServerDate();
            }
        });
    }

}
