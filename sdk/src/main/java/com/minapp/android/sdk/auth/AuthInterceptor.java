package com.minapp.android.sdk.auth;

import com.minapp.android.sdk.BuildConfig;
import com.minapp.android.sdk.Config;
import com.minapp.android.sdk.Const;
import com.minapp.android.sdk.exception.UninitializedException;
import okhttp3.Interceptor;
import okhttp3.Request;

import java.io.IOException;

/**
 * 在 okhttp request 中加入认证头
 */
public class AuthInterceptor implements Interceptor {
    @Override
    public okhttp3.Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();

        String clientId = Config.getClientId();
        if (clientId == null) {
            throw new IOException(new UninitializedException());
        }
        builder.header(Const.HTTP_HEADER_CLIENT_ID, clientId);

        String token = Auth.token();
        if (token != null) {
            builder.header(Const.HTTP_HEADER_AUTH, Const.HTTP_HEADER_AUTH_PREFIX + token);
        }

        String envId = Config.getEnvId();
        if (envId != null) {
            builder.header(Const.HTTP_HEADER_ENV, envId);
        }

        builder.header(Const.HTTP_HEADER_PLATFORM, Const.SDK_PLATFORM);
        builder.header(Const.HTTP_HEADER_VERSION, BuildConfig.VERSION_NAME);
        return chain.proceed(builder.build());
    }
}
