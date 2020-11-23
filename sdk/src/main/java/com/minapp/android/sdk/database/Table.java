package com.minapp.android.sdk.database;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.minapp.android.sdk.database.query.Query;
import com.minapp.android.sdk.util.PagedList;
import com.minapp.android.sdk.util.BaseCallback;
import com.minapp.android.sdk.util.Util;
import com.minapp.android.sdk.ws.SessionManager;
import com.minapp.android.sdk.ws.SubscribeCallback;
import com.minapp.android.sdk.ws.SubscribeEvent;
import com.minapp.android.sdk.ws.WampSubscription;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

public class Table {

    private String tableName;


    public Table(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public boolean equals( Object obj) {
        if (obj instanceof Table) {
            Table other = (Table) obj;
            return TextUtils.equals(this.tableName, other.tableName);
        }
        return false;
    }


    /******************************** simple curd **************************************/


    public void countInBackground(final Query query, BaseCallback<Integer> cb) {
        Util.inBackground(cb, new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                List list = query(query).getObjects();
                return list != null ? list.size() : 0;
            }
        });
    }

    public int count(Query query) throws Exception {
        List list = query(query).getObjects();
        return list != null ? list.size() : 0;
    }

    /**
     * 通过此 api 创建一条新记录
     * @return
     */
    public Record createRecord() {
        return new Record(this);
    }

    /**
     * 通过此 api 获取一条记录
     * @return
     */
    public Record fetchRecord(String recordId, Query query) throws Exception {
        return Database.fetch(this, recordId, query);
    }

    public Record fetchRecord(
            String recordId,
            @Nullable Collection<String> expands,
            @Nullable Collection<String> keys) throws Exception {

        Query query = new Query();
        query.put(Query.EXPAND, Util.joinToNull(expands));
        query.put(Query.KEYS, Util.joinToNull(keys));
        return fetchRecord(recordId, query);
    }

    public Record fetchRecord(String recordId) throws Exception {
        return fetchRecord(recordId, null);
    }

    public void fetchRecordInBackground(final String recordId, final Query query, @NonNull final BaseCallback<Record> cb) {
        Util.inBackground(cb, new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                return fetchRecord(recordId, query);
            }
        });
    }

    public void fetchRecordInBackground(
            final String recordId,
            @Nullable final Collection<String> expands,
            @Nullable final Collection<String> keys,
            @NonNull BaseCallback<Record> cb) {

        Util.inBackground(cb, new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                return fetchRecord(recordId, expands, keys);
            }
        });
    }

    public void fetchRecordInBackground(final String recordId, @NonNull BaseCallback<Record> cb) {
        Util.inBackground(cb, new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                return fetchRecord(recordId);
            }
        });
    }

    /**
     * 查询
     * @param query
     * @return
     * @throws Exception
     */
    public PagedList<Record> query(Query query) throws Exception {
        return Database.query(this, query);
    }

    /**
     * 查询
     * @see #query(Query)
     * @param query
     * @param cb
     */
    public void queryInBackground(final Query query, @NonNull final BaseCallback<PagedList<Record>> cb) {
        Util.inBackground(cb, new Callable<PagedList<Record>>() {
            @Override
            public PagedList<Record> call() throws Exception {
                return query(query);
            }
        });
    }

    /**
     * 根据 id 构造一条记录，但不去抓取这条记录的内容，所以这条记录只有 table 和 id 信息
     * @param id
     * @return
     */
    public Record fetchWithoutData(String id) {
        Record record = createRecord();
        record.put(Record.ID, id);
        return record;
    }


    /******************************** batch operation **************************************/

    /**
     * 批量删除，通过 where 查询条件
     * @param query
     * @return
     * @throws Exception
     */
    public BatchResult batchDelete(Query query) throws Exception {
        return Database.batchDelete(this, query);
    }

    /**
     * 批量保存
     * @param records
     * @return
     * @throws Exception
     */
    public BatchResult batchSave(List<Record> records) throws Exception {
        return Database.batchSave(this, records, new Query());
    }

    /**
     * 批量保存
     * @param records
     * @return
     * @throws Exception
     */
    public BatchResult batchSave(List<Record> records, Query query) throws Exception {
        return Database.batchSave(this, records, query);
    }

    /**
     * 批量更新
     * @param query
     * @param update
     * @return
     * @throws Exception
     */
    public BatchResult batchUpdate(Query query, Record update) throws Exception {
        return Database.batchUpdate(this, query, update);
    }


    public void batchDeleteInBackground(final Query query, @NonNull final BaseCallback<BatchResult> cb) {
        Util.inBackground(cb, new Callable<BatchResult>() {
            @Override
            public BatchResult call() throws Exception {
                return batchDelete(query);
            }
        });
    }

    public void batchSaveInBackground(final List<Record> records, @NonNull final BaseCallback<BatchResult> cb) {
        Util.inBackground(cb, new Callable<BatchResult>() {
            @Override
            public BatchResult call() throws Exception {
                return batchSave(records);
            }
        });
    }

    public void batchSaveInBackground(final List<Record> records, final Query query, @NonNull final BaseCallback<BatchResult> cb) {
        Util.inBackground(cb, new Callable<BatchResult>() {
            @Override
            public BatchResult call() throws Exception {
                return batchSave(records, query);
            }
        });
    }

    public void batchUpdateInBackground(final Query query, final Record update, @NonNull final BaseCallback<BatchResult> cb) {
        Util.inBackground(cb, new Callable<BatchResult>() {
            @Override
            public BatchResult call() throws Exception {
                return batchUpdate(query, update);
            }
        });
    }


    public WampSubscription subscribe(@NonNull Query query, @NonNull SubscribeEvent event,
                                      @NonNull SubscribeCallback cb) {
        return SessionManager.get().subscribe(this, query, event, cb);
    }

    public WampSubscription subscribe(@NonNull SubscribeEvent event, @NonNull SubscribeCallback cb) {
        return subscribe(new Query(), event, cb);
    }
}
