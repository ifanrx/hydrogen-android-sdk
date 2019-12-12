package com.minapp.android.sdk.wechat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.minapp.android.sdk.Const;
import com.minapp.android.sdk.Global;
import com.minapp.android.sdk.auth.Auth;
import com.minapp.android.sdk.auth.model.ThirdPartySignInReq;
import com.minapp.android.sdk.auth.model.ThirdPartySignInResp;
import com.minapp.android.sdk.exception.EmptyResponseException;
import com.minapp.android.sdk.exception.HttpException;
import com.minapp.android.sdk.exception.SessionMissingException;
import com.minapp.android.sdk.util.StatusBarUtil;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import java.io.IOException;
import java.util.logging.Logger;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    private static final String TAG = "WXEntryActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View content = new View(this);
        content.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        content.setBackgroundColor(Color.TRANSPARENT);
        setContentView(content);
        StatusBarUtil.setStatusBar(Color.TRANSPARENT, true, false, this);

        try {
            if (!WechatComponent.handleIntent(getIntent(), this)) {
                onResult(false, null);
            }
        } catch (Exception e) {
            onResult(false, e);
        }
    }

    /**
     * 服务端微信注册
     * @param tokenFromWechat
     */
    private void sendServerAuth(String tokenFromWechat) {
        Global.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ThirdPartySignInResp resp = Global.httpApi()
                            .signInByWechat(new ThirdPartySignInReq(tokenFromWechat)).execute().body();
                    if (resp == null)
                        throw new EmptyResponseException();
                    if (!resp.isOk())
                        throw new Exception(new StringBuilder()
                                .append("sign in error: ")
                                .append(resp.message).toString()
                        );

                    Auth.signIn(resp.token, String.valueOf(resp.userId), resp.expiresIn);
                    onResult(true, null);

                } catch (Exception e) {
                    if (!isDestroyed()) {
                        onResult(false, e);
                    }
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        try {
            if (!WechatComponent.handleIntent(intent, this)) {
                onResult(false, null);
            }
        } catch (Exception e) {
            onResult(false, e);
        }
    }

    @Override
    public void onReq(BaseReq baseReq) {
        onResult(false, null);
    }

    @Override
    public void onResp(BaseResp baseResp) {
        if (baseResp instanceof SendAuth.Resp) {
            SendAuth.Resp authResp = (SendAuth.Resp) baseResp;
            if (BaseResp.ErrCode.ERR_OK != authResp.errCode) {
                onResult(false, new WechatException(authResp));
            } else {
                sendServerAuth(authResp.code);
            }
        } else {
            onResult(false, null);
        }
    }

    @Override
    public void onBackPressed() {
        onResult(false, null);
    }

    private void onResult(boolean success, Exception ex) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onResultAtMain(success, ex);
        } else {
            Global.postOnMain(new Runnable() {
                @Override
                public void run() {
                    onResult(success, ex);
                }
            });
        }
    }

    private void onResultAtMain(boolean success, Exception ex) {
        if (!isDestroyed())
            finish();

        WechatSignInCallback cb = WechatComponent.WECHAT_CB;
        if (cb != null) {
            if (success) {
                cb.onSuccess();
            } else {
                cb.onFailure(ex);
            }
        }
        WechatComponent.WECHAT_CB = null;
    }

}
