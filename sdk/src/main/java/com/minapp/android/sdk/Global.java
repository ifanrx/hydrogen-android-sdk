package com.minapp.android.sdk;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minapp.android.sdk.auth.AuthInterceptor;
import com.minapp.android.sdk.auth.CheckedCallAdapterFactory;
import com.minapp.android.sdk.database.GeoPoint;
import com.minapp.android.sdk.database.GeoPolygon;
import com.minapp.android.sdk.database.query.Condition;
import com.minapp.android.sdk.database.query.ConditionNode;
import com.minapp.android.sdk.database.query.WithinCircle;
import com.minapp.android.sdk.database.query.WithinRegion;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class Global {

    private static HttpApi HTTP_API;
    private static Gson GSON;
    private static Gson GSON_PRINT;
    private static ScheduledExecutorService EXECUTOR_SERVICE;
    private static Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static Application APP = null;
    private static OkHttpClient CLIENT = null;


    public static @Nullable Application getApplication() {
        return APP;
    }

    static void setApplicaiton(Application application) {
        APP = application;
    }


    public static void postOnMain(@NonNull Runnable runnable) {
        if (Util.isOnMain())
            runnable.run();
        else
            MAIN_HANDLER.post(runnable);
    }

    public static HttpApi httpApi() {
        if (HTTP_API == null) {
            synchronized (Global.class) {
                if (HTTP_API == null) {
                    HTTP_API = new Retrofit.Builder()
                            .client(httpClient())
                            .addConverterFactory(GsonConverterFactory.create(gson()))
                            .addCallAdapterFactory(new CheckedCallAdapterFactory())
                            .baseUrl(Config.getEndpoint())
                            .build()
                            .create(HttpApi.class);
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
                            .disableHtmlEscaping()
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

    public static void post(Runnable task) {
        postDelayed(task, 0);
    }

    public static void submit(Runnable task) {
        post(task);
    }

    public static void postDelayed(Runnable job, long delayMillis) {
        executorService().schedule(job, delayMillis, TimeUnit.MILLISECONDS);
    }

    public static ScheduledExecutorService executorService() {
        if (EXECUTOR_SERVICE == null) {
            synchronized (Global.class) {
                if (EXECUTOR_SERVICE == null) {
                    EXECUTOR_SERVICE = Executors.newScheduledThreadPool(5);
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
                .registerTypeAdapterFactory(new RecordTypeAdapterFactory())
                .registerTypeAdapter(GeoPoint.class, new GeoPointSerializer())
                .registerTypeAdapter(GeoPoint.class, new GeoPointDeserializer())
                .registerTypeAdapter(GeoPolygon.class, new GeoPolygonSerializer())
                .registerTypeAdapter(GeoPolygon.class, new GeoPolygenDeserializer())
                .registerTypeAdapter(WithinCircle.class, new WithinCircleSerializer())
                .registerTypeAdapter(WithinRegion.class, new WithinRegionSerializer())
                ;
    }

    public static OkHttpClient httpClient() {
        if (CLIENT == null) {
            synchronized (Global.class) {
                if (CLIENT == null) {
                    CLIENT = new OkHttpClient.Builder()
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
                }
            }
        }
        return CLIENT;
    }

}
