package com.minapp.android.sdk.database.query;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.minapp.android.sdk.util.Util;

import java.util.*;

/**
 * 对于 filed 的查询有两种方式
 */
public class Query extends HashMap<String, String> {

    /**
     * 查询语句的 JSON String
     */
    static final String WHERE = "where";

    /**
     * 对资源进行排序
     * 每一个需要排序的字段以逗号分隔，如：name,age,created_at
     * 字段前面加「-」表示 desc，默认是 asc
     *
     */
    public static final String ORDER_BY = "order_by";

    /**
     * 返回资源的个数
     */
    public static final String LIMIT = "limit";

    /**
     * 返回资源的起始偏移值
     */
    public static final String OFFSET = "offset";

    /**
     * 支持 sum, group by 等聚合查询操作
     * 使用原生 MongoDB aggregation 语法
     * 若 query string 同时存在 where 与 aggregation，优先使用 aggregation
     * 当前支持的 pipeline 长度为 2
     * 当前的聚合查询超时时间为 10s
     * limit, offset, order_by 参数与其他版本 API 一致
     * 当前可使用的 stage: * match * group * project * sample * count
     * 当前可使用的 operator: * sum * avg * max * min * size(用于数组) * add * subtract * multiply * divide
     * 更多条件请参照最新的文档
     */
    public static final String AGGREGATION = "aggregation";

    /**
     * 需要展开的字段（把 pointer 展开，否则 pointer 只返回 table & id）
     * 每一个需要排序的字段以逗号分隔，如：name,age,created_at
     */
    public static final String EXPAND = "expand";
    /**
     * 指定输出/不输出 field（需注意，接口不支持指定输出，不输出的情况，即：?keys=-a,b）
     * 指定输出：keys=horse_name,horse_age
     * 指定不输出；keys=-horse_name,-horse_age
     */
    public static final String KEYS = "keys";


    /**
     * 是否开启触发器
     * @see #TRIGGER_ENABLE
     * @see #TRIGGER_DISABLE
     */
    public static final String ENABLE_TRIGGER = "enable_trigger";
    public static final String TRIGGER_ENABLE = "1";
    public static final String TRIGGER_DISABLE = "0";


    /**
     * 从 v2.2 开始，默认不返回 totalCount
     */
    public static final String RETURN_TOTAL_COUNT = "return_total_count";
    public static final int ENABLE_TOTAL_COUNT = 1;     // 返回 totalCount
    public static final int DISABLE_TOTAL_COUNT = 0;    // 不返回 totalCount




    private Map<String, String> queryMap = new HashMap<>();


    public Query() {}

    public Query(Query q) {
        if (q != null) {
            queryMap.putAll(q.queryMap);
        }
    }

    /*************************  open method  ******************************/

    /**
     * @see #OFFSET
     * @see #LIMIT
     * @see #ORDER_BY
     * @see #EXPAND
     * @see #KEYS
     * @see #AGGREGATION
     * @return
     */
    @Nullable
    @Override
    public String put(String key, String value) {
        if (key != null) {
            if (value == null) {
                remove(key);
            } else {
                return super.put(key, value);
            }
        }
        return null;
    }

    public void put(String key, Number number) {
        put(key, number != null ? number.toString() : null);
    }


    public Query put(Where where) {
        String json = where.toString();
        if (TextUtils.isEmpty(json) || "null".equalsIgnoreCase(json)) {
            remove(WHERE);
        } else {
            put(WHERE, json);
        }
        return this;
    }

    public void put(Operator operator, String key, String value) {
        if (key != null) {
            key = key + "__" + operator.value;
            if (value == null) {
                remove(key);
            } else {
                put(key, value);
            }
        }
    }

    public @Nullable String get(Operator operator, String key) {
        if (key != null) {
            key = key + "__" + operator.value;
            return get(key);
        }
        return null;
    }


    /*************************  convenient method  ******************************/


    /**
     * 开启/关闭触发器
     * @param enable
     * @return
     */
    public Query enableTrigger(boolean enable) {
        put(ENABLE_TRIGGER, enable ? TRIGGER_ENABLE : TRIGGER_DISABLE);
        return this;
    }


    public Query limit(@NonNull Number limit) {
        put(LIMIT, limit);
        return this;
    }

    public Query offset(@NonNull Number offset) {
        put(OFFSET, offset);
        return this;
    }

    public Query orderBy(@NonNull Collection<String> orderBy) {
        put(ORDER_BY, Util.joinToNull(orderBy));
        return this;
    }

    public Query orderBy(String... orderBy) {
        if (orderBy != null) {
            orderBy(Arrays.asList(orderBy));
        }
        return this;
    }

    /**
     * 使用 select 来控制请求返回的字段
     *
     * select('field') select(['field_a', 'field_b']) 为“规定返回”，
     * select('-field') select(['-field_a', '-field_b']) 为“规定不返回”，
     * “规定返回”和“规定不返回”不能同时使用，否则该操作不生效，接口将会直接返回所有字段。
     *
     * @param keys
     * @return
     * @see Query#KEYS
     */
    public Query select(@NonNull Collection<String> keys) {
        put(KEYS, Util.joinToNull(keys));
        return this;
    }

    /**
     * 使用 select 来控制请求返回的字段
     *
     * select('field') select(['field_a', 'field_b']) 为“规定返回”，
     * select('-field') select(['-field_a', '-field_b']) 为“规定不返回”，
     * “规定返回”和“规定不返回”不能同时使用，否则该操作不生效，接口将会直接返回所有字段。
     *
     * @param keys
     * @return
     * @see Query#select(Collection)
     */
    public Query select(String... keys) {
        if (keys != null) {
            select(Arrays.asList(keys));
        }
        return this;
    }

    public Query expand(Collection<String> fields) {
        put(EXPAND, Util.joinToNull(fields));
        return this;
    }

    public Query expand(String... fields) {
        if (fields != null) {
            expand(Arrays.asList(fields));
        }
        return this;
    }

    public Query returnTotalCount(boolean enable) {
        put(RETURN_TOTAL_COUNT, enable ? ENABLE_TOTAL_COUNT : DISABLE_TOTAL_COUNT);
        return this;
    }
}
