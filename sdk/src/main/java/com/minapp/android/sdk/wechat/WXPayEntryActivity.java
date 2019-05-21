package com.minapp.android.sdk.wechat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import com.minapp.android.sdk.Global;
import com.minapp.android.sdk.util.StatusBarUtil;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.modelpay.PayResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import static com.minapp.android.sdk.wechat.WechatComponent.WECHAT_API;
import static com.minapp.android.sdk.wechat.WechatComponent.assertInit;

/**
 * 接收微信的回调
 */
public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    static final String DATA_RESULT = "DATA_RESULT";
    static final String PARAM = "PARAM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View content = new View(this);
        content.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content.setBackgroundColor(Color.TRANSPARENT);
        setContentView(content);

        StatusBarUtil.setStatusBar(Color.TRANSPARENT, false, false, this);
        sendOrder();
    }


    private void sendOrder() {
        final WechatOrder request = getIntent().getParcelableExtra(PARAM);
        if (request == null) {
            close(false, null, null);
            return;
        }

        Global.submit(new Runnable() {
            @Override
            public void run() {
                try {

                    // 让 Baas 与微信服务器交互，客户端拿到预付单
                    assertInit();
                    WechatOrderResp resp = Global.httpApi().sendWechatOrder(request).execute().body();

                    // 拉起微信支付
                    if (!isDestroyed()) {
                        PayReq req = new PayReq();
                        req.appId = resp.getAppId();
                        req.partnerId = resp.getPartnerId();
                        req.prepayId = resp.getPrepayId();
                        req.packageValue = resp.getPackageValue();
                        req.nonceStr = resp.getNonceStr();
                        req.timeStamp = resp.getTimestamp();
                        req.sign = resp.getSign();
                        WECHAT_API.sendReq(req);
                    }

                } catch (Exception e) {
                    if (!isDestroyed()) {
                        close(false, null, e);
                    }
                }
            }
        });
    }

    /**
     * 返回结果并关闭页面
     * @param success   true - {@link Activity#RESULT_OK}, false - {@link Activity#RESULT_CANCELED}
     * @param payResp   微信的响应，optional
     * @param exception 异常，optional
     */
    private void close(boolean success, PayResp payResp, Exception exception) {
        int resultCode = success ? RESULT_OK : RESULT_CANCELED;
        WechatOrderResult result = null;

        if (payResp != null) {
            if (result == null) {
                result = new WechatOrderResult();
            }
            result.setPayResp(payResp);
        }

        if (exception != null) {
            if (result == null) {
                result = new WechatOrderResult();
            }
            result.setException(exception);
        }

        Intent data = null;
        if (result != null) {
            data = new Intent();
            data.putExtra(DATA_RESULT, result);
        }
        setResult(resultCode, data);
        finish();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        try {
            if (!WechatComponent.handleIntent(intent, this)) {
                close(false, null, null);
            }
        } catch (WechatNotInitException e) {
            close(false, null, e);
        }
    }

    /**
     * 一般情况下微信不会主动请求 app，但有某些情况：小程序里打开 app...
     * 这里 request 里没有什么有用的信息来指示我们需要怎么做，所以直接打开主页即可
     * @param req
     */
    @Override
    public void onReq(BaseReq req) {
        finish();
    }

    /**
     * 这里接收微信返回的微信登录结果
     * @param resp
     */
    @Override
    public void onResp(BaseResp resp) {
        boolean success = false;
        PayResp payResp = null;

        if (resp != null) {
            success = resp.errCode == BaseResp.ErrCode.ERR_OK;

            if (resp instanceof PayResp) {
                payResp = (PayResp) resp;
            }
        }

        close(success, payResp, null);
    }

    @Override
    public void onBackPressed() {
        close(false, null, null);
    }

    /**
     * 注意目标不是 {@link WXPayEntryActivity}，而是 pkg + ".wxapi.WXPayEntryActivity"
     * @param requestCode
     * @param param
     * @param activity
     */
    static void startActivityForResult(int requestCode, WechatOrder param, Activity activity) {
        String pkg = activity.getPackageName();
        String clz = pkg + ".wxapi.WXPayEntryActivity";
        ComponentName component = new ComponentName(pkg, clz);

        Intent intent = new Intent();
        intent.putExtra(PARAM, param);
        intent.setComponent(component);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(intent, requestCode);
        }
    }
}
