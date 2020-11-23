package com.minapp.android.sdk.test.suite

import com.minapp.android.sdk.test.*
import com.minapp.android.sdk.test.table.DatabaseTest
import com.minapp.android.sdk.test.table.FieldTypeTest
import com.minapp.android.sdk.test.table.GeoTest
import com.minapp.android.sdk.test.table.QueryTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    DatabaseTest::class, FieldTypeTest::class, GeoTest::class, QueryTest::class,
    StorageTest::class, SmsTest::class, ContentTest::class, CloudFuncTest::class,
    CurrentUserTest::class, AuthTest::class, ServerDateTest::class, EnvTest::class,
    WebSocketTest::class
)
class AllTestSuite