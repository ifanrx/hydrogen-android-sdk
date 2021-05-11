package com.minapp.android.sdk.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.minapp.android.sdk.Const;
import com.minapp.android.sdk.Global;
import com.minapp.android.sdk.storage.CloudFile;
import com.minapp.android.sdk.util.Function;
import com.minapp.android.sdk.util.Util;
import com.minapp.android.sdk.util.BaseCallback;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.Callable;

public class Record {

    public static final String ID = "id";
    public static final String CREATED_AT = "created_at";
    public static final String CREATED_BY = "created_by";
    public static final String UPDATED_AT = "updated_at";
    public static final String WRITE_PERM = "write_perm";
    public static final String READ_PERM = "read_perm";
    public static final String TABLE = "_table";

    public static final String SPECIAL_UNSET = "$unset";

    /*************************** 原子操作符 ***********************************/

    public static final String INCR_BY = "$incr_by";
    public static final String APPEND = "$append";
    public static final String APPEND_UNIQUE = "$append_unique";
    public static final String REMOVE = "$remove";
    public static final String UPDATE = "$update";
    public static final String POP = "$pop";


    private @Nullable Table table;
    private @NonNull JsonObject json;


    public Record(Table table) {
        this(table, null);
    }

    public Record() {
        this(null, null);
    }

    public Record(Table table, JsonObject json) {
        this.table = table;
        this.json = json;
        if (this.json == null) {
            this.json = new JsonObject();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("tableName : ").append(getTableName())
                .append("\n").append(Global.gson().toJson(json))
                .toString();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Record) {
            Record other = (Record) obj;
            try {
                return table.getTableName().equals(other.table.getTableName())
                        && getId().equals(other.getId());
            } catch (Exception e) {}
        }
        return false;
    }

    /*************************** CURD ***********************************/


    public Record update(SaveOptions options) throws Exception {
        String id = getId();
        if (Util.isNullOrEmpty(id))
            throw new IllegalStateException("ID can not be empty in update");
        Database.save(this, options);
        return this;
    }


    public void updateInBackground(SaveOptions options, @NonNull final BaseCallback<Record> callback) {
        Util.inBackground(callback, new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                update(options);
                return Record.this;
            }
        });
    }


    public Record save() throws Exception {
        return save(null);
    }

    public Record save(SaveOptions options) throws Exception {
        Database.save(this, options);
        return this;
    }

    public void saveInBackground(@NonNull final BaseCallback<Record> callback) {
        saveInBackground(null, callback);
    }

    public void saveInBackground(SaveOptions options, @NonNull final BaseCallback<Record> callback) {
        Util.inBackground(callback, new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                save(options);
                return Record.this;
            }
        });
    }

    public void delete() throws Exception {
        Database.delete(this);
    }

    public void deleteInBackground(@NonNull final BaseCallback<Record> callback) {
        Util.inBackground(callback, new Callable<Record>() {
            @Override
            public Record call() throws Exception {
                delete();
                return Record.this;
            }
        });
    }


    /*************************** private method ***********************************/


    public JsonObject _getJson() {
        return json;
    }


    public void _setJson(JsonObject json) {
        if (json == null) {
            json = new JsonObject();
        }
        this.json = json;
    }

    public Table _getTable() {
        return table;
    }

    public void _setTable(Table table) {
        this.table = table;
    }


    public Record _deepClone() {
        Record clone = new Record();
        clone._setTable(table);
        clone._setJson(json.deepCopy());
        return clone;
    }

    private @Nullable <T> List<T> getArray(@NonNull String key, Function<JsonElement, T> transform) {
        Util.assetNotNull(key);
        try {
            JsonArray array = json.get(key).getAsJsonArray();
            List<T> list = new ArrayList<>(array.size());
            for (JsonElement elem : array) {
                list.add(transform.on(elem));
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }


    /*************************** misc ***********************************/


    /**
     * 删除字段
     * @param fields
     * @return
     */
    public Record unset(Collection<String> fields) {
        if (fields == null || fields.isEmpty()) {
            json.remove(SPECIAL_UNSET);
        } else {
            JsonObject map = new JsonObject();
            for (String field: fields) {
                map.addProperty(field, "");
            }
            json.add(SPECIAL_UNSET, map);
        }
        return this;
    }

    public Record remove(@NonNull String key) {
        json.remove(key);
        return this;
    }


    /*************************** meta info ***********************************/



    public @Nullable String getId() {
        return getString(ID);
    }

    public @Nullable Long getCreatedBy() {
        return getLong(CREATED_BY);
    }

    public @Nullable Long getCreatedAt() {
        return getLong(CREATED_AT);
    }

    public @Nullable Long getUpdatedAt() {
        return getLong(UPDATED_AT);
    }

    public @Nullable List<String> getWritePerm() {
        return getArray(WRITE_PERM, String.class);
    }

    public @Nullable List<String> getReadPerm() {
        return getArray(READ_PERM, String.class);
    }

    public @Nullable String getTableName() {
        return table != null ? table.getTableName() : null;
    }



    /*************************** 原子操作 ***********************************/

    /**
     * 从原数组 key 中删除最后一项
     * @param key
     * @return
     */
    public Record pop(@NonNull String key) {
        return put(key, Util.singleMap(POP, 1));
    }

    /**
     * 从原数组 key 中删除第一项
     * @param key
     * @return
     */
    public Record shift(@NonNull String key) {
        return put(key, Util.singleMap(POP, -1));
    }

    /**
     * 对数字类型的键进行增减操作
     * @param by
     * @return
     */
    public Record incrementBy(@NonNull String key, int by) {
        return put(key, Util.singleMap(INCR_BY, by));
    }

    /**
     * 将 append 里的值追加到数组类型的字段中
     * @param key
     * @param append
     * @param <T>
     * @return
     */
    public <T> Record append(@NonNull String key, @NonNull Collection<T> append) {
        return put(key, Util.singleMap(APPEND, append));
    }


    /**
     * 将 appendUnique 里的值追加到数组类型的字段中, 且仅当该值不在数组内才增加
     * @param key
     * @param appendUnique
     * @param <T>
     * @return
     */
    public <T> Record appendUnique(@NonNull String key, @NonNull Collection<T> appendUnique) {
        return put(key, Util.singleMap(APPEND_UNIQUE, appendUnique));
    }

    /**
     * 从数组类型的字段中删除 removes 里的每个值,
     * @param key
     * @param removes
     * @param <T>
     * @return
     */
    public <T> Record remove(@NonNull String key, @NonNull Collection<T> removes) {
        return put(key, Util.singleMap(REMOVE, removes));
    }

    /**
     * 以 update 中的各字段值更新 object 中的相应字段
     * @param key
     * @param update
     * @return
     */
    public Record patchObject(@NonNull String key, @NonNull Record update) {
        return put(key, Util.singleMap(UPDATE, update));
    }


    /*************************** public setter ***********************************/


    /**
     * put json field
     * @param key
     * @param value 可以是：
     *              1，基本类型及其包装类，{@link String}
     *              2，{@link JsonObject}, {@link JsonArray}, {@link JsonNull} 等 Gson 类型
     *              3，集合
     *              4，时间日期用 {@link Calendar}
     *              5，自定义类型，注意不要混淆 properties，或者加上 {@link SerializedName}
     *              6，{@link CloudFile}
     *              7，如果列类型是 pointer，还可以是 {@link Record} 及其子类
     * @return
     */
    public Record put(@NonNull String key, @Nullable Object value) {
        JsonElement elem = Global.gson().toJsonTree(value);

        /**
         * 如果是 {@link Record}，则是 pointer 类型，这里要把 table 放入 json 里面
         */
        if (value instanceof Record && elem instanceof JsonObject) {
            Record record = (Record) value;
            JsonObject obj = (JsonObject) elem;

            String table = record.getTableName();
            if (table != null) {
                obj.addProperty(Record.TABLE, table);
            }
        }
        json.add(key, elem);
        return this;
    }

    /**
     * 这个方法相当于批量设置 field
     * 它用其他 {@link Record} 里的值覆盖此对象的值
     * @param other
     * @return
     */
    public Record putAll(@NonNull Record other) {
        for (Map.Entry<String, JsonElement> entry : other.json.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        return this;
    }


    /*************************** public getter ***********************************/


    /**
     * 直接作为 {@link JsonObject} 返回
     * @param key
     * @return
     */
    public @Nullable JsonObject getJsonObject(@NonNull String key) {
        Util.assetNotNull(key);
        try {
            return json.getAsJsonObject(key);
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable GeoPoint getGeoPoint(String key) {
        return getObject(key, GeoPoint.class);
    }

    public @Nullable GeoPolygon getGeoPolygon(String key) {
        return getObject(key, GeoPolygon.class);
    }


    /**
     * 1，对应 date 列类型（日期时间，ISO8601 格式的日期字符串，例如："2018-09-01T18:31:02.631000+08:00"）
     * 2，或者它本身就是 {@link Calendar}
     * @param key
     * @return
     */
    public @Nullable Calendar getCalendar(String key) {
        return getObject(key, Calendar.class);
    }


    /**
     * 对应 file 列类型
     * @param key
     * @return
     */
    public @Nullable CloudFile getFile(String key) {
        return getObject(key, CloudFile.class);
    }


    public @Nullable <T> List<T> getArray(@NonNull String key, final Class<T> clz) {
        return getArray(key, new Function<JsonElement, T>() {
            @Override
            public T on(JsonElement elem) {
                return Global.gson().fromJson(elem, clz);
            }
        });
    }

    public <T> T getObject(@NonNull String key, Class<T> clz) {
        Util.assetNotNull(key);
        try {
            return Global.gson().fromJson(json.get(key), clz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *
     * @param key
     * @param type eg: new TypeToken<List<String>>(){}.getType()
     * @param <T>
     * @return
     */
    public <T> T getObject(@NonNull String key, Type type) {
        Util.assetNotNull(key);
        try {
            return Global.gson().fromJson(json.get(key), type);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    public @Nullable Boolean getBoolean(@NonNull String key) {
        Util.assetNotNull(key);
        try {
            return json.get(key).getAsBoolean();
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable Number getNumber(@NonNull String key) {
        Util.assetNotNull(key);
        try {
            return json.get(key).getAsNumber();
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable Integer getInt(@NonNull String key) {
        try {
            return getNumber(key).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable Long getLong(@NonNull String key) {
        try {
            return getNumber(key).longValue();
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable Float getFloat(@NonNull String key) {
        try {
            return getNumber(key).floatValue();
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable Double getDouble(@NonNull String key) {
        try {
            return getNumber(key).doubleValue();
        } catch (Exception e) {
            return null;
        }
    }

    public @Nullable String getString(@NonNull String key) {
        Util.assetNotNull(key);
        try {
            return json.get(key).getAsString();
        } catch (Exception e) {
            return null;
        }
    }

}

