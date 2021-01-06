package com.minapp.android.sdk.test.table

import com.google.gson.JsonObject
import com.minapp.android.sdk.auth.Auth
import com.minapp.android.sdk.database.Record
import com.minapp.android.sdk.database.Table
import com.minapp.android.sdk.database.query.Query
import com.minapp.android.sdk.database.query.Where
import com.minapp.android.sdk.test.Util
import com.minapp.android.sdk.test.base.BaseAuthedTest
import com.minapp.android.sdk.user.User
import com.minapp.android.sdk.util.PagedList
import org.junit.BeforeClass
import org.junit.Test
import org.junit.Assert.*

class QueryTest: BaseAuthedTest() {

    companion object {
        lateinit var table: Table
        lateinit var pear: Record
        lateinit var apple: Record
        lateinit var peach: Record
        lateinit var orange: Record
        lateinit var passionFruit: Record
        lateinit var watermelon: Record
        lateinit var avocado: Record

        @BeforeClass
        @JvmStatic
        fun prepare() {
            table = Table(TableContract.TABLE_NAME)
            table.batchDelete(Query())

            val email = Util.randomEmail()
            val pwd = Util.randomString()
            try {
                Auth.signUpWithEmail(email, pwd)
            } catch (e: Exception) {}
            val neighborhood = Auth.signInWithEmail(email, pwd)

            pear = table.createRecord().apply {
                put(TableContract.NAME, "pear")
                put(TableContract.AGE, 8)
                put(TableContract.BIRTHDATE, Util.calendar(2019, 4, 23, 9, 35, 45))
                save()
            }

            apple = table.createRecord().apply {
                put(TableContract.NAME, "apple")
                put(TableContract.AGE, 12)
                put(TableContract.CHILDREN, listOf("mario", "yoshi", "ada"))
                save()
            }

            peach = table.createRecord().apply {
                put(TableContract.NAME, "peach")
                put(TableContract.AGE, 21)
                put(TableContract.BIRTHDATE, Util.calendar(2000, 5, 1, 14, 25, 39))
                save()
            }

            orange = table.createRecord().apply {
                put(TableContract.NAME, "orange")
                put(TableContract.AGE, 30)
                put(TableContract.MESSAGE, JsonObject().apply {
                    addProperty("length", 100)
                })
                save()
            }

            passionFruit = table.createRecord().apply {
                put(TableContract.NAME, "passion fruit")
                put(TableContract.AGE, 45)
                put(TableContract.NEIGHBORHOOD, neighborhood)
                put(TableContract.CHILDREN, listOf("mario", "yoshi"))
                save()
            }

            watermelon = table.createRecord().apply {
                put(TableContract.NAME, "water melon")
                put(TableContract.AGE, 50)
                put(TableContract.BIRTHDATE, Util.calendar(1998, 12, 12, 12, 12, 12))
                save()
            }

            avocado = table.createRecord().apply {
                put(TableContract.NAME, "avocado")
                put(TableContract.AGE, 50)
                put(TableContract.MESSAGE, JsonObject().apply {
                    addProperty("country", "USA")
                })
                save()
            }
        }
    }

    @Test
    fun orderByTest() {
        val result = table.query(Query().orderBy(TableContract.AGE, TableContract.NAME))
        assertEquals(result.objects.firstOrNull(), pear)
        assertEquals(result.objects.lastOrNull(), watermelon)
    }

    @Test
    fun offsetTest() {
        val result = table.query(Query().orderBy(TableContract.AGE).offset(3))
        assertEquals(result.objects.firstOrNull(), orange)
    }

    @Test
    fun limitTest() {
        val result = table.query(Query().orderBy(TableContract.AGE).limit(6))
        assertEquals(result.objects.lastOrNull(), watermelon)
    }

    @Test
    fun selectTest() {
        var result = table.query(Query().select(TableContract.NAME))
        assertTrue(result.objects!!.all {
            it.getInt(TableContract.AGE) == null && it.getString(TableContract.NAME) != null
        })

        result = table.query(Query().select("-${TableContract.NAME}"))
        assertTrue(result.objects!!.all { it.getString(TableContract.NAME) == null })
    }

    /**
     * 测试过滤 expand field 里的字段如，expand_by.email
     */
    @Test
    fun selectTest2() {
        val id = passionFruit.id!!
        var query = Query().apply {
            put(Where().apply {
                equalTo(Record.ID, id)
            })
            offset(0)
            limit(1)
            expand(TableContract.NEIGHBORHOOD)
        }
        val getEmail: ( List<Record>? ) -> String? = {
            it?.firstOrNull()?.getJsonObject(TableContract.NEIGHBORHOOD)?.get(User.EMAIL)?.asString
        }
        val getId: ( List<Record>? ) -> String? = {
            it?.firstOrNull()?.getJsonObject(TableContract.NEIGHBORHOOD)?.get(Record.ID)?.asString
        }

        var result = table.query(query).objects
        assertTrue(!getEmail(result).isNullOrEmpty())

        query = query.select("${TableContract.NEIGHBORHOOD}.${User.ID}")
        result = table.query(query).objects
        assertTrue(getEmail(result) == null && !getId(result).isNullOrEmpty())
    }

    @Test
    fun expandTest() {
        var result = table.query(Query())
        assertNull(result.objects.find { it == passionFruit }!!.getObject(TableContract.NEIGHBORHOOD, User::class.java)!!.email)

        result = table.query(Query().expand(TableContract.NEIGHBORHOOD))
        assertEquals(Auth.currentUser()!!.email!!,
            result.objects.find { it == passionFruit }!!.getObject(TableContract.NEIGHBORHOOD, User::class.java)!!.email!!)
    }

    /**
     * 等于，不等于
     */
    @Test
    fun eqNeTest() {
        val result = table.query(Query().put(
            Where().equalTo(TableContract.AGE, 50).notEqualTo(TableContract.NAME, "water melon")))
        assert(result.totalCount == 1L)
        assertEquals(result.objects.firstOrNull(), avocado)
    }

    /**
     * 小于，小于等于
     */
    @Test
    fun lteTest() {
        var result = table.query(Query().put(
            Where().lessThan(TableContract.AGE, 8)))
        assert(result.totalCount == 0L)

        result = table.query(Query().put(
            Where().lessThanOrEqualTo(TableContract.AGE, 8)))
        assert(result.totalCount == 1L)
        assertEquals(result.objects.firstOrNull(), pear)
    }

    /**
     * 大于，大于等于
     */
    @Test
    fun gteTest() {
        var result = table.query(Query().put(
            Where().lessThan(TableContract.AGE, 50)))
        assert(result.totalCount == 0L)

        result = table.query(Query()
            .put(Where().lessThanOrEqualTo(TableContract.AGE, 50))
            .orderBy(TableContract.AGE))
        assert(result.totalCount == 2L)
        assert(result.objects == listOf(watermelon, avocado))
    }

    @Test
    fun containsTest() {
        val result = table.query(Query().put(
            Where().contains(TableContract.NAME, "melon")))
        assert(result.totalCount == 1L)
        assertEquals(result.objects.firstOrNull(), watermelon)
    }

    @Test
    fun inTest() {
        val result = table.query(Query()
            .put(Where().containedIn(TableContract.NAME, listOf("water melon", "pear")))
            .orderBy(TableContract.AGE))
        assert(result.totalCount == 2L)
        assertEquals(result.objects, listOf(pear, watermelon))
    }

    @Test
    fun ninTest() {
        val result = table.query(Query()
            .put(Where().notContainedIn(TableContract.NAME, listOf("passion fruit", "avocado", "apple")))
            .orderBy(TableContract.AGE))
        assertEquals(result.objects, listOf(pear, peach, orange, watermelon))
    }

    @Test
    fun isNullTest() {
        val result = table.query(Query()
            .put(Where().isNull(TableContract.BIRTHDATE))
            .orderBy(TableContract.AGE))
        assertEquals(result.objects, listOf(apple, orange, passionFruit, avocado))
    }

    @Test
    fun existTest() {
        val result = table.query(Query()
            .put(Where().exists(TableContract.NEIGHBORHOOD))
            .orderBy(TableContract.AGE))
        assertEquals(result.objects, listOf(passionFruit))
    }

    @Test
    fun hasKeyTest() {
        val result = table.query(Query()
            .put(Where().hasKey(TableContract.MESSAGE, "country"))
            .orderBy(TableContract.AGE))
        assertEquals(result.objects, listOf(avocado))
    }

    @Test
    fun allTest() {
        val result = table.query(Query()
            .put(Where().arrayContains(TableContract.CHILDREN, listOf("mario", "yoshi")))
            .orderBy(TableContract.AGE))
        assertEquals(result.objects, listOf(apple, passionFruit))
    }

    @Test
    fun regexTest() {
        val result = table.query(Query()
            .put(Where().matchs(TableContract.NAME, "pea.+"))
            .orderBy(TableContract.AGE))
        assertEquals(result.objects, listOf(pear, peach))
    }

    /**
     * 多个 where 条件
     */
    @Test
    fun whereTest() {
        val result = table.query(Query()
            .put(Where().greaterThan(TableContract.AGE, 20)
                .contains(TableContract.NAME, "e")
                .isNotNull(TableContract.BIRTHDATE))
            .orderBy(TableContract.AGE))
        assertEquals(result.objects, listOf(peach, watermelon))
    }

    @Test
    fun totalCountTest() {
        // 默认情况下，不返回 totalCount
        assertEquals(0, table.query(Query()).totalCount ?: 0)

        // 开启 totalCount
        assertEquals(7, table.query(Query().returnTotalCount(true)).totalCount)

        // 关闭 totalCount
        assertEquals(0, table.query(Query().returnTotalCount(false)).totalCount ?: 0)
    }
}