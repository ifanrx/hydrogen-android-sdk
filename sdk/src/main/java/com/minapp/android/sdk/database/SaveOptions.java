package com.minapp.android.sdk.database;

import java.util.List;

public class SaveOptions {

    /**
     * 是否返回对应字段扩展
     * 创建/更新 Record 时可用
     */
    public List<String> expand;

    /**
     * 是否返回 total_count
     * 更新 Record 时可用
     */
    public Boolean withCount;

    /**
     * 是否触发触发器
     * 更新 Record 时可用
     */
    public Boolean enableTrigger;

}
