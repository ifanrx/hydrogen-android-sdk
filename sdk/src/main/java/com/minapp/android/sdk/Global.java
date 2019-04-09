package com.minapp.android.sdk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minapp.android.sdk.auth.AuthInterceptor;
import com.minapp.android.sdk.auth.CheckedCallAdapterFactory;
import com.minapp.android.sdk.database.query.Condition;
import com.minapp.android.sdk.database.query.ConditionNode;
import com.minapp.android.sdk.typeadapter.*;
import com.minapp.android.sdk.util.*;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public abstract class Global {

    private static HttpApi HTTP_API;
    private static Gson GSON;
    private static Gson GSON_PRINT;
    private static ExecutorService EXECUTOR_SERVICE;

    public static HttpApi httpApi() {
        if (HTTP_API == null) {
            synchronized (Global.class) {
                if (HTTP_API == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .connectTimeout(Const.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .readTimeout(Const.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .writeTimeout(Const.HTTP_TIMEOUT, TimeUnit.MILLISECONDS)
                            .cookieJar(new MemoryCookieJar())
                            .retryOnConnectionFailure(true)
                            .addNetworkInterceptor(new AuthInterceptor())
                            .addNetworkInterceptor(new ContentTypeInterceptor())
                            .build();

                    if (GSON == null) {
                        gson();
                    }

                    Retrofit retrofit = new Retrofit.Builder()
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create(GSON))
                            .addCallAdapterFactory(new CheckedCallAdapterFactory())
                            .baseUrl(Const.HTTP_HOST)
                            .build();

                    HTTP_API = retrofit.create(HttpApi.class);
                }
            }
        }
        return HTTP_API;
    }

    public static Gson gson() {
        if (GSON == null) {
            synchronized (Global.class) {
                if (GSON == null) {
                    GSON = createGson()
                            .create();
                }
            }
        }
        return GSON;
    }

    public static Gson gsonPrint() {
        if (GSON_PRINT == null) {
            synchronized (Global.class) {
                if (GSON_PRINT == null) {
                    GSON_PRINT = createGson()
                            .setPrettyPrinting()
                            .create();
                }
            }
        }
        return GSON_PRINT;
    }

    public static Future<?> submit(Runnable task) {
        return executorService().submit(task);
    }

    static ExecutorService executorService() {
        if (EXECUTOR_SERVICE == null) {
            synchronized (Global.class) {
                if (EXECUTOR_SERVICE == null) {
                    EXECUTOR_SERVICE = Executors.newFixedThreadPool(5);
                }
            }
        }
        return EXECUTOR_SERVICE;
    }

    private static GsonBuilder createGson() {
        return new GsonBuilder()
                .setLenient()
                .registerTypeAdapter(Condition.class, new Condition.Serializer())
                .registerTypeAdapter(ConditionNode.class, new ConditionNode.Serializer())
                .registerTypeAdapter(Calendar.class, new CalendarSerializer())
                .registerTypeAdapter(Calendar.class, new CalendarDeserializer())
                .registerTypeAdapter(GregorianCalendar.class, new GregorianCalendarSerializer())
                .registerTypeAdapter(GregorianCalendar.class, new GregorianCalendarDeserializer())
                .registerTypeAdapterFactory(new RecordTypeAdapterFactory());
    }

}
