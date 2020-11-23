package com.minapp.android.sdk.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minapp.android.sdk.Const;
import com.minapp.android.sdk.Global;
import com.minapp.android.sdk.database.Record;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public abstract class Util {

    private static final String[] POINTER_FEATURE = new String[]{
            Record.ID, Record.TABLE
    };

    /**
     * 网络开关已打开，并且连上了网络
     * @param ctx
     * @return
     */
    @SuppressLint("MissingPermission")
    public static boolean isNetworkAvailable(Context ctx) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isNullOrEmpty(String str) {
        if (str == null)
            return true;
        return str.isEmpty();
    }

    public static @Nullable String parseString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    public static @Nullable Integer parseInteger(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof Long) {
            return ((Long) obj).intValue();
        }
        return null;
    }

    public static boolean isOnMain() {
        return Looper.myLooper() == Looper.getMainLooper();
    }


    public static <T> void inBackground(@NonNull final BaseCallback<T> cb, @NonNull final Callable<T> callable) {
        Global.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final T t = callable.call();
                    Global.postOnMain(new Runnable() {
                        @Override
                        public void run() {
                            cb.onSuccess(t);
                        }
                    });

                } catch (final Exception e) {
                    Global.postOnMain(new Runnable() {
                        @Override
                        public void run() {
                            cb.onFailure(e);
                        }
                    });
                }
            }
        });
    }

    public static Map singleMap(@NonNull Object key, Object value) {
        Map retVal = new HashMap(1);
        retVal.put(key, value);
        return retVal;
    }


    /**
     * 判断一个 filed 是否是 Pointer（包含字段 id 和 _table）
     * @param elem
     * @return 如果是 Pointer，则返回其 ID；否则返回 null
     */
    public static String getPointerId(JsonElement elem) {
        if (elem != null && elem.isJsonObject()) {
            JsonObject obj = (JsonObject) elem;
            for (String filed : POINTER_FEATURE) {
                if (!obj.has(filed)) {
                    return null;
                }
            }
            try {
                return obj.get(Record.ID).getAsString();
            } catch (Exception e) {
                Log.e(Const.TAG, e.getMessage(), e);
                return null;
            }
        }
        return null;
    }

    public static <T> void each(Collection<T> collection, Action<T> action) {
        if (collection != null && collection.size() > 0 && action != null) {
            for (T aCollection : collection) {
                action.on(aCollection);
            }
        }
    }

    public static <R, T> PagedListResponse<R> transform(PagedListResponse<T> response, Function<T, R> func) {
        PagedListResponse<R> data = new PagedListResponse<>();
        data.setMeta(response.getMeta());
        List<T> objects = response.getObjects();
        if (objects != null && func != null) {
            data.setObjects(new ArrayList<R>(objects.size()));
            for (T item : objects) {
                data.getObjects().add(func.on(item));
            }
        }
        return data;
    }

    public static @Nullable String readString(InputStream input) {
        return readString(input, "utf-8");
    }

    public static @Nullable String readString(InputStream input, String charset) {
        try {
            byte[] bytes = new byte[input.available()];
            input.read(bytes);
            return new String(bytes, charset);
        } catch (Exception e) {
            try {
                input.close();
            } catch (Exception ignored) {}
            return null;
        }
    }

    public static void assetNotNull(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
    }

    /**
     * copy from okhttp3.internal.Util
     * Quick and dirty pattern to differentiate IP addresses from hostnames. This is an approximation
     * of Android's private InetAddress#isNumeric API.
     *
     * <p>This matches IPv6 addresses as a hex string containing at least one colon, and possibly
     * including dots after the first colon. It matches IPv4 addresses as strings containing only
     * decimal digits and dots. This pattern matches strings like "a:.23" and "54" that are neither IP
     * addresses nor hostnames; they will be verified as IP addresses (which is a more strict
     * verification).
     */
    private static final Pattern VERIFY_AS_IP_ADDRESS = Pattern.compile(
            "([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");

    /**
     * copy from okhttp3.internal.Util
     * Returns true if {@code host} is not a host name and might be an IP address.
     * */
    private static boolean verifyAsIpAddress(String host) {
        return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
    }


    /**
     * copy from okhttp3.Cookie
     */
    public static boolean domainMatch(String urlHost, String domain) {
        if (urlHost.equals(domain)) {
            return true; // As in 'example.com' matching 'example.com'.
        }

        if (urlHost.endsWith(domain)
                && urlHost.charAt(urlHost.length() - domain.length() - 1) == '.'
                && !verifyAsIpAddress(urlHost)) {
            return true; // As in 'example.com' matching 'www.example.com'.
        }

        return false;
    }



    public static @NonNull String join(Collection data, String separator) {
        if (data != null && !data.isEmpty()) {
            if (separator == null) {
                separator = ",";
            }
            StringBuilder sb = new StringBuilder();
            Iterator iterator = data.iterator();
            Object item = null;
            while (iterator.hasNext()) {
                item = iterator.next();
                if (item != null) {
                    sb.append(item.toString()).append(separator);
                }
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }
        return "";
    }

    public static @NonNull String join(Collection data) {
        return join(data, Const.COMMA);
    }

    /**
     * 如果集合为 null or empty，返回 null
     * @param data
     * @return
     */
    public static @Nullable String joinToNull(Collection data) {
        String result = join(data);
        return TextUtils.isEmpty(result) ? null : result;
    }

    public static @Nullable String trimToNull(String source) {
        if (source == null)
            return null;

        source = source.trim();
        return source.isEmpty() ? null : source;
    }
}
