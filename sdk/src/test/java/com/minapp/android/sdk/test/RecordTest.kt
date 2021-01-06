package com.minapp.android.sdk.test

import com.minapp.android.sdk.Global
import com.minapp.android.sdk.database.Record
import com.minapp.android.sdk.database.Table
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

class RecordTest: BaseUnitTest() {

    companion object {
        private val TABLE = Table("fruits")

        private fun toJson(obj: Any) = Global.gson().toJson(obj)
    }

    private lateinit var record: Record

    @Before
    fun init() {
        record = TABLE.createRecord()
    }

    @Test
    fun putTest() {
        val parent = TABLE.createRecord().apply {
            put(Record.ID, "d03mv830vmsdof93f8dm")
        }
        val birthdate = Calendar.getInstance().apply {
            set(2019, 11, 19, 16, 28, 30)
            set(Calendar.MILLISECOND, 0)
        }
        with(record) {
            put("name", "harry")
            put("age", 28)
            put("friends", listOf("lion", "gate"))
            put("parent", parent)
            put("birthdate", birthdate)
        }
        assertEquals("""
            {"name":"harry","age":28,"friends":["lion","gate"],"parent":{"id":"d03mv830vmsdof93f8dm","_table":"fruits"},"birthdate":"2019-12-19T16:28:30.000000+08:00"}
        """.trimIndent(), toJson(record))
    }

    @Test
    fun putAllTest() {
        val template = TABLE.createRecord().apply {
            put("name", "apple")
            put("price", 2.5f)
            put("taxed", true)
        }
        record.putAll(template)
        assertEquals("""
            {"name":"apple","price":2.5,"taxed":true}
        """.trimIndent(), toJson(record))
    }

    @Test
    fun updateTest() {
        val template = TABLE.createRecord().apply {
            put("name", "apple")
            put("price", 2.5f)
            put("taxed", true)
        }
        val expected = """
            {"favorited_fruit":{"${Record.UPDATE}":{"name":"apple","price":2.5,"taxed":true}}}
        """.trimIndent()
        assertEquals(expected, toJson(record.patchObject("favorited_fruit", template)))
    }

    @Test
    fun removeTest() {
        val key = "name"
        val value = "harry"
        record.put(key, value)
        assertEquals(value, record.getString(key))

        record.remove(key)
        assertEquals(null, record.getString(key))
    }

    @Test
    fun removeElemTest() {
        record.remove("fruits", listOf("apple", "pear", "banana"))
        val expected = """
            {"fruits":{"${Record.REMOVE}":["apple","pear","banana"]}}
        """.trimIndent()
        assertEquals(expected, toJson(record))
    }

    @Test
    fun unsetTest() {
        val expected = """
            {"${Record.SPECIAL_UNSET}":{"name":"","age":"","address":""}}
        """.trimIndent()
        assertEquals(expected, toJson(record.unset(listOf("name", "age", "address"))))
    }

    @Test
    fun incrementByTest() {
        val expected = """
            {"hits":{"${Record.INCR_BY}":10}}
        """.trimIndent()
        assertEquals(expected, toJson(record.incrementBy("hits", 10)))
    }

    @Test
    fun appendArrayTest() {
        val expected = """
            {"friends":{"${Record.APPEND}":["lion","gate"]}}
        """.trimIndent()
        assertEquals(expected, toJson(record.append("friends", listOf("lion", "gate"))))
    }

    @Test
    fun appendUniqueTest() {
        val expected = """
            {"friends":{"${Record.APPEND_UNIQUE}":["lion","gate"]}}
        """.trimIndent()
        assertEquals(expected, toJson(record.appendUnique("friends", listOf("lion", "gate"))))
    }
}