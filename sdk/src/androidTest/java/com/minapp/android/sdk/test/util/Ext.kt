package com.minapp.android.sdk.test.util

import com.minapp.android.sdk.database.query.Query
import com.minapp.android.sdk.database.query.Where

fun where(configuration: Where.() -> Unit) =
    Query().apply { put(Where().apply(configuration)) }