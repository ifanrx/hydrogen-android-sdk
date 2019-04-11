package com.minapp.android.sdk.user;

import androidx.annotation.NonNull;
import com.minapp.android.sdk.Global;
import com.minapp.android.sdk.database.query.Query;
import com.minapp.android.sdk.util.Callback;
import com.minapp.android.sdk.util.PagedList;
import com.minapp.android.sdk.util.Util;

import java.util.concurrent.Callable;

public abstract class Users {

    /**
     * 用户列表
     * @return
     * @throws Exception
     */
    public static PagedList<User> users(Query query) throws Exception {
        return Global.httpApi().users(query != null ? query : new Query()).execute().body().readonly();
    }

    public static void usersInBackground(final Query query, @NonNull Callback<PagedList<User>> cb) {
        Util.inBackground(cb, new Callable<PagedList<User>>() {
            @Override
            public PagedList<User> call() throws Exception {
                return Users.users(query);
            }
        });
    }

    /**
     * 用户明细
     * @param id
     * @return
     * @throws Exception
     */
    public static User use(String id) throws Exception {
        return Global.httpApi().user(id).execute().body();
    }


    public static void useInBackground(final String id, @NonNull Callback<User> cb) {
        Util.inBackground(cb, new Callable<User>() {
            @Override
            public User call() throws Exception {
                return Users.use(id);
            }
        });
    }
}
