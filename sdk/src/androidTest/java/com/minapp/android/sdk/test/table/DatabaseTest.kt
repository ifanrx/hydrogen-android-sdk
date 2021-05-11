package com.minapp.android.sdk.test.table

import com.minapp.android.sdk.BaaS
import com.minapp.android.sdk.database.Record
import com.minapp.android.sdk.database.SaveOptions
import com.minapp.android.sdk.database.query.Query
import com.minapp.android.sdk.database.query.Where
import com.minapp.android.sdk.exception.HttpException
import com.minapp.android.sdk.model.BatchOperationResp
import com.minapp.android.sdk.test.Util
import com.minapp.android.sdk.user.User
import com.minapp.android.sdk.util.BaseCallback
import org.junit.Assert.*
import org.junit.Test

class DatabaseTest: BaseTableTest() {

    /**
     * 更新 Record 时，ID 不能为空
     */
    @Test(expected = IllegalStateException::class)
    fun updateWithNullId() {
        val record = table.createRecord()
        record.put(TableContract.NAME, "Jesse")
        record.put(TableContract.AGE, 30)
        record.update(null)
    }

    /**
     * 更新 Record 时，ID 不能为空
     */
    @Test(expected = IllegalStateException::class)
    fun updateWithEmptyId() {
        val record = table.createRecord()
        record.put(TableContract.NAME, "Jesse")
        record.put(TableContract.AGE, 30)
        record.put(Record.ID, " ")
        record.update(null)
    }

    /**
     * 测试 save options 参数
     */
    @Test
    fun saveOptionsTest() {
        val record = table.createRecord()
        record.put(TableContract.NAME, "Jesse")
        record.put(TableContract.AGE, 30)
        val getCreatedByEmail: (Record) -> String? = {
            it.getJsonObject(Record.CREATED_BY)?.get(User.EMAIL)?.asString
        }

        record.save()
        assertNull(getCreatedByEmail(record))

        record.put(TableContract.NAME, "harry")
        record.save(SaveOptions().apply {
            expand = listOf(Record.CREATED_BY)
        })
        assertNotNull(getCreatedByEmail(record))
    }

    /**
     * 简单的增删改查操作
     */
    @Test
    fun simpleCurdTest() {
        val record = table.createRecord()
        record.put(TableContract.NAME, "Jesse")
        record.put(TableContract.AGE, 30)
        record.save()
        assertNotNull(record.id)

        val query = Query().apply {
            put(Where().apply {
                equalTo(Record.ID, record.id)
            })
        }
        table.query(query).objects.also {
            assert(it.size == 1)
            assert(it[0].id == record.id)
        }

        record.put(TableContract.NAME, "Harry")
        record.save()
        assertEquals(
            table.fetchRecord(record.id).getString(
                TableContract.NAME
            ), record.getString(TableContract.NAME))

        val id = record.id!!
        record.delete()
        try {
            table.fetchRecord(id)
        } catch (e: HttpException) {
            if (e.code == 404)
                return
        }
        throw IllegalStateException()
    }

    /**
     * 测试批量操作
     */
    @Test
    fun batchTest() {
        val records = listOf(
            table.createRecord().apply {
                put(TableContract.NAME, "First")
            },

            table.createRecord().apply {
                put(TableContract.NAME, "Second")
            },

            table.createRecord().apply {
                put(TableContract.NAME, "Third")
            }
        )
        val result = table.batchSave(records)
        assertEquals(result.succeed, 3)
        assertEquals(result.totalCount, 3)
        result.operationResult.all { !it.success.id.isNullOrEmpty() }
        records.all { !it.id.isNullOrEmpty() }

        val query = Query().apply {
            put(Where().containedIn(Record.ID, records.map { it.id }))
            limit(records.size)
        }
        table.batchDelete(query)
        assertEquals(table.count(Query()), 0)
    }

    /**
     * 测试异步的批量操作（更新，删除）
     */
    @Test
    fun asyncBatchTest() {

        // 新增测试数据（这里要新增 1000+ 的记录）
        val records = List(1200) {
            table.createRecord().apply { put(TableContract.NAME, Util.randomString(length = 5)) } }

        // batch create 每次最多 1K 条
        records.windowed(size = 600, step = 600).forEach {
            val result = table.batchSave(it)
            assertEquals(result.succeed.toInt(), it.size)
            assertEquals(result.totalCount.toInt(), it.size)
            result.operationResult.all { !it.success.id.isNullOrEmpty() }
            records.all { !it.id.isNullOrEmpty() }
        }

        // 测试异步更新
        val update = table.createRecord().apply {
            put(TableContract.AGE, 18)
        }
        val updateResult = table.batchUpdate(Query().apply { enableTrigger(false) }, update)
        val updateId = updateResult.operationId
        assertTrue(updateId > 0)

        val updateJob = BaaS.queryBatchOperation(updateId)
        assertEquals(BatchOperationResp.OPERATION_UPDATE, updateJob.operation)
        assertEquals(table.tableName, updateJob.schemaName)
        assertEquals(records.size, updateJob.matchedCount)

        // 测试异步删除
        val deleteResult = table.batchDelete(Query().apply { enableTrigger(false) })
        val deleteId = deleteResult.operationId
        assert(deleteId > 0)

        val deleteJob = BaaS.queryBatchOperation(deleteId)
        assertEquals(BatchOperationResp.OPERATION_DELETE, deleteJob.operation)
        assertEquals(table.tableName, deleteJob.schemaName)

        // 这里客户端只关心生成异步操作和异步任务，不关心操作结果
    }
}

