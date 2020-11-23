package com.minapp.android.sdk.auth;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minapp.android.sdk.BaaS;
import com.minapp.android.sdk.Config;
import com.minapp.android.sdk.Const;
import com.minapp.android.sdk.Global;
import com.minapp.android.sdk.HttpApi;
import com.minapp.android.sdk.auth.model.*;
import com.minapp.android.sdk.user.User;
import com.minapp.android.sdk.user.Users;
import com.minapp.android.sdk.util.*;
import com.minapp.android.sdk.util.BaseCallback;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public abstract class Auth {

    static final String TOKEN = "TOKEN";                            // String
    static final String USER_ID = "USER_ID";                        // String
    static final String SIGN_IN_ANONYMOUS = "SIGN_IN_ANONYMOUS";    // Boolean
    static final String EXPIRES_IN = "EXPIRES_IN";                  // Long，时间戳，单位毫秒

    private static final MemoryCookieJar COOKIE_JAR = new MemoryCookieJar();
    private static HttpApi API;
    private static final Object API_LOCK = new Object();
    public static final Map<Object, Object> AUTH_INFO = new HashMap<>();


    static @Nullable Long getExpiresAt() {
        return (Long) AUTH_INFO.get(EXPIRES_IN);
    }

    /**
     * 当 sdk 初始化时 {@link BaaS#init(String, Application)}，必须调用此方法初始化 auth 模块
     */
    public static void init() {
        restoreAuthData();
    }


    /**
     * 登出
     */
    public static void logout() {
        synchronized (AUTH_INFO) {
            AUTH_INFO.clear();
            storeAuthData();
        }
    }

    /**
     * 是否已登录
     * @return
     */
    public static boolean signedIn() {
        boolean signedIn = true;

        Long expiresAt = getExpiresAt();
        if (expiresAt != null && expiresAt > 0) {
            Calendar now = DateUtil.pekingCalendar();
            Calendar expiresIn = DateUtil.pekingCalendar();
            expiresIn.setTimeInMillis(expiresAt);
            if (now.after(expiresIn)) {
                signedIn = false;
            }
        }

        if (AUTH_INFO.get(TOKEN) == null) {
            signedIn = false;
        }

        if (!signedIn) {
            logout();
        }

        return signedIn;
    }

    /**
     * 当前登录用户的信息；它会执行一次网络请求，获取到的用户信息总是最新的
     * @return 当未登录时，返回 null
     */
    public static @Nullable CurrentUser currentUser() throws Exception {
        String userId = (String) AUTH_INFO.get(USER_ID);
        if (userId != null) {
            return new CurrentUser(Users.user(userId));
        }
        return null;
    }

    public static @Nullable CurrentUser currentUserWithoutData() {
        String userId = (String) AUTH_INFO.get(USER_ID);
        if (userId != null) {
            CurrentUser user = new CurrentUser();
            user.put(CurrentUser.ID, userId);
            return user;
        }
        return null;
    }

    /**
     * @see #currentUser()
     * @param cb
     */
    public static void currentUserInBackground(@NonNull final BaseCallback<CurrentUser> cb) {
        Util.inBackground(cb, new Callable<CurrentUser>() {
            @Override
            public CurrentUser call() throws Exception {
                return Auth.currentUser();
            }
        });
    }

    /**
     * 判断用户是否是匿名用户
     * @return true 已登录并且是匿名登录
     */
    public static boolean isAnonymous() {
        return Boolean.TRUE.equals(AUTH_INFO.get(SIGN_IN_ANONYMOUS));
    }

    /**
     * 使用手机号 + 短信验证码登录（注册）
     * @param req
     * @return
     * @throws Exception
     */
    public static User signInWithPhone(SignInWithPhoneRequest req) throws Exception {
        User user = Global.httpApi().signInWithPhone(req).execute().body();
        signIn(user);
        return user;
    }

    /**
     * 使用手机号 + 短信验证码登录（注册）
     * @param req
     * @param cb
     */
    public static void signInWithPhoneInBackground(SignInWithPhoneRequest req, @NonNull BaseCallback<User> cb) {
        Util.inBackground(cb, new Callable<User>() {
            @Override
            public User call() throws Exception {
                return Auth.signInWithPhone(req);
            }
        });
    }


    /**
     * 邮箱注册
     * @param email
     * @param pwd
     * @return
     * @throws Exception
     */
    public static User signUpWithEmail(String email, String pwd) throws Exception {
        User user = Global.httpApi().signUpWithEmail(new SignUpInWithEmailReq(email, pwd)).execute().body();
        signIn(user);
        return user;
    }

    public static void signUpWithEmailInBackground(final String email, final String pwd, @NonNull BaseCallback<User> cb) {
        Util.inBackground(cb, new Callable<User>() {
            @Override
            public User call() throws Exception {
                return Auth.signUpWithEmail(email, pwd);
            }
        });
    }

    /**
     * 用户名注册
     * @param username
     * @param pwd
     * @return
     * @throws Exception
     */
    public static User signUpWithUsername(String username, String pwd) throws Exception {
        User user = Global.httpApi().signUpWithUsername(new SignUpInWithUsernameReq(username, pwd)).execute().body();
        signIn(user);
        return user;
    }

    public static void signUpWithUsernameInBackground(final String username, final String pwd, @NonNull BaseCallback<User> cb) {
        Util.inBackground(cb, new Callable<User>() {
            @Override
            public User call() throws Exception {
                return Auth.signUpWithUsername(username, pwd);
            }
        });
    }

    /**
     * 邮箱登录
     * @param email
     * @param pwd
     * @return
     * @throws Exception
     */
    public static User signInWithEmail(String email, String pwd) throws Exception {
        User info = Global.httpApi().signInWithEmail(new SignUpInWithEmailReq(email, pwd)).execute().body();
        signIn(info);
        return info;
    }


    public static void signInWithEmailInBackground(final String email, final String pwd, @NonNull BaseCallback<User> cb) {
        Util.inBackground(cb, new Callable<User>() {
            @Override
            public User call() throws Exception {
                return Auth.signInWithEmail(email, pwd);
            }
        });
    }


    /**
     * 用户名登录
     * @param username
     * @param pwd
     * @return
     * @throws Exception
     */
    public static User signInWithUsername(String username, String pwd) throws Exception {
        User info = Global.httpApi().signInWithUsername(new SignUpInWithUsernameReq(username, pwd)).execute().body();
        signIn(info);
        return info;
    }

    public static void signInWithUsernameInBackground(final String username, final String pwd, @NonNull BaseCallback<User> cb) {
        Util.inBackground(cb, new Callable<User>() {
            @Override
            public User call() throws Exception {
                return Auth.signInWithUsername(username, pwd);
            }
        });
    }

    /**
     * 匿名登录
     * @return
     * @throws Exception
     */
    public static void signInAnonymous() throws Exception {
        User info = Global.httpApi().signInAnonymous(new Object()).execute().body();
        signIn(info, true);
    }

    public static void signInAnonymousInBackground(@NonNull BaseCallback<Void> cb) {
        Util.inBackground(cb, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Auth.signInAnonymous();
                return null;
            }
        });
    }

    /**
     * 登录成功后，保存用户信息
     * @param info
     * @see #signIn(String, String, long)
     */
    private static void signIn(User info) {
        signIn(info, false);
    }

    /**
     * 登录成功后，保存用户信息
     * @param info
     * @see #signIn(String, String, long)
     */
    private static void signIn(User info, boolean anonymous) {
        synchronized (AUTH_INFO) {
            if (info != null) {
                AUTH_INFO.clear();

                String token = info.getString(User.TOKEN);
                if (token != null) {
                    AUTH_INFO.put(TOKEN, token);
                }

                String userId = info.getString("user_id");
                if (userId != null) {
                    AUTH_INFO.put(USER_ID, userId);
                }

                try {
                    AUTH_INFO.put(EXPIRES_IN,
                            Long.valueOf(info.getString(User.EXPIRES_IN)) * 1000 +
                                    System.currentTimeMillis());
                } catch (Exception ignored) {}

                AUTH_INFO.put(SIGN_IN_ANONYMOUS, anonymous);
            }
            storeAuthData();
        }
    }

    /**
     * 登录成功后，保存用户信息
     * @param token
     * @param userId
     * @param expiresIn 秒
     * @see #signIn(User)
     */
    public static void signIn(String token, String userId, long expiresIn) {
        synchronized (AUTH_INFO) {
            AUTH_INFO.clear();
            if (token != null) {
                AUTH_INFO.put(TOKEN, token);
            }
            if (userId != null) {
                AUTH_INFO.put(USER_ID, userId);
            }
            try {
                AUTH_INFO.put(EXPIRES_IN, expiresIn * 1000 + System.currentTimeMillis());
            } catch (Exception ignored) {}
            storeAuthData();
        }
    }


    /**
     * 把登录信息持久化
     */
    static void storeAuthData() {
        SharedPreferences sp = getGlobalSP();
        if (sp != null) {
            Boolean anonymous = (Boolean) AUTH_INFO.get(SIGN_IN_ANONYMOUS);
            Long expiresIn = (Long) AUTH_INFO.get(EXPIRES_IN);
            sp.edit()
                    .putString(TOKEN, (String) AUTH_INFO.get(TOKEN))
                    .putString(USER_ID, (String) AUTH_INFO.get(USER_ID))
                    .putBoolean(SIGN_IN_ANONYMOUS, anonymous != null ? anonymous : false)
                    .putLong(EXPIRES_IN, expiresIn != null ? expiresIn : 0)
                    .apply();
        }
    }

    /**
     * 从持久化数据源中恢复登录信息
     */
    static void restoreAuthData() {
        SharedPreferences sp = getGlobalSP();
        if (sp != null) {
            AUTH_INFO.put(TOKEN, sp.getString(TOKEN, null));
            AUTH_INFO.put(USER_ID, sp.getString(USER_ID, null));
            AUTH_INFO.put(SIGN_IN_ANONYMOUS, sp.getBoolean(SIGN_IN_ANONYMOUS, false));
            AUTH_INFO.put(EXPIRES_IN, sp.getLong(EXPIRES_IN, 0));
        }
    }

    static @Nullable SharedPreferences getGlobalSP() {
        SharedPreferences sp = null;
        Application app = Global.getApplication();
        if (app != null) {
            sp = app.getSharedPreferences(Const.SP_NAME, Context.MODE_PRIVATE);
        }
        return sp;
    }


    public static @Nullable String token() {
        return (String) AUTH_INFO.get(TOKEN);
    }

    private static HttpApi httpApi() {
        if (API == null) {
            synchronized (API_LOCK) {
                if (API == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .connectTimeout(Const.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .readTimeout(Const.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .writeTimeout(Const.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .cookieJar(new MemoryCookieJar())
                            .retryOnConnectionFailure(true)
                            .addNetworkInterceptor(new ContentTypeInterceptor())
                            .build();

                    Gson gson = new GsonBuilder()
                            .setLenient()
                            .create();
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(Config.getEndpoint())
                            .addConverterFactory(GsonConverterFactory.create(gson))
                            .client(client)
                            .build();
                    API = retrofit.create(HttpApi.class);
                }
            }
        }
        return API;
    }
}
