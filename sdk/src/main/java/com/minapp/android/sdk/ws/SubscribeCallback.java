package com.minapp.android.sdk.ws;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

public interface SubscribeCallback {

    /**
     * 回调函数将在订阅动作初始化成功时调用，一次 subscribe 订阅动作会触发一次
     */
    @WorkerThread
    void onInit();

    /**
     * 回调函数将在数据表每次有相应动作变化时调用，参数是数据表变化了的数据项
     * @param event
     */
    @WorkerThread
    void onEvent(@NonNull SubscribeEventData event);

    /**
     * 回调函数将在订阅动作出错是调用，参数是错误信息
     * 订阅被取消
     * @param tr
     */
    @WorkerThread
    void onError(@NonNull Throwable tr);

}
