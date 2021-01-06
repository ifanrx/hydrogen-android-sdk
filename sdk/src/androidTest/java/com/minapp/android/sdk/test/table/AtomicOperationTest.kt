package com.minapp.android.sdk.test.table

import com.minapp.android.sdk.test.TestConst
import org.junit.Assert
import org.junit.Test

class AtomicOperationTest: BaseTableTest() {

    @Test
    fun popTest() {
        val children = mutableListOf("petter", "harry", "hermione", "ron")
        val record = table.createRecord().apply {
            put(TableContract.CHILDREN, children)
            save()
        }
        Assert.assertArrayEquals(children.toTypedArray(),
            record.getArray(TableContract.CHILDREN, String::class.java)?.toTypedArray())

        children.removeAt(children.lastIndex)
        record.pop(TableContract.CHILDREN)
        record.save()
        Assert.assertArrayEquals(children.toTypedArray(),
            record.getArray(TableContract.CHILDREN, String::class.java)?.toTypedArray())
    }

    @Test
    fun shiftTest() {
        val children = mutableListOf("petter", "harry", "hermione", "ron")
        val record = table.createRecord().apply {
            put(TableContract.CHILDREN, children)
            save()
        }
        Assert.assertArrayEquals(children.toTypedArray(),
            record.getArray(TableContract.CHILDREN, String::class.java)?.toTypedArray())

        children.removeAt(0)
        record.shift(TableContract.CHILDREN)
        record.save()
        Assert.assertArrayEquals(children.toTypedArray(),
            record.getArray(TableContract.CHILDREN, String::class.java)?.toTypedArray())
    }
}