package com.minapp.android.sdk.test

import android.util.Log
import com.minapp.android.sdk.Global
import com.minapp.android.sdk.auth.Auth
import com.minapp.android.sdk.database.Record
import com.minapp.android.sdk.database.Table
import com.minapp.android.sdk.exception.SessionMissingException
import com.minapp.android.sdk.exception.UnauthorizedException
import com.minapp.android.sdk.test.base.BaseAuthedTest
import com.minapp.android.sdk.test.util.SimpleCondition
import com.minapp.android.sdk.test.util.Util
import com.minapp.android.sdk.test.util.where
import com.minapp.android.sdk.util.BaseCallback
import com.minapp.android.sdk.util.LazyProperty
import com.minapp.android.sdk.ws.*
import com.minapp.android.sdk.ws.exceptions.SubscribeErrorException
import io.crossbar.autobahn.websocket.WebSocketConnection
import io.crossbar.autobahn.websocket.interfaces.IWebSocketConnectionHandler
import org.junit.After
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class WebSocketTest: BaseAuthedTest() {

    companion object {
        private const val TAG = "WebSocketTest"
        private const val COL_NAME = "name"
    }

    private val cond by lazy { SimpleCondition() }
    private val table by lazy { Table("danmu") }
    private val sm by lazy { SessionManager.get() }

    private val exception by lazy {
        AtomicReference<Throwable>(null) }

    private val event by lazy {
        AtomicReference<SubscribeEventData>(null) }

    private val subscriptions by lazy {
        ArrayList<WampSubscription>()
    }

    private val nameAfter: String?
        get() = event.get()?.after?.getString(COL_NAME)

    private val nameBefore: String?
        get() = event.get()?.before?.getString(COL_NAME)

    @After
    fun after() {
        SessionManager.ENABLE_RECONNECT = true
        subscriptions.clear()
        sm.clearSubscribers()
        cond.await()
    }

    /**
     * 测试 create event
     */
    @Test
    fun createEvent() {
        val name = Util.randomString()
        subscribeAndWait(nameEqualTo = name)

        saveRecord(name = "$name${System.currentTimeMillis()}")
        Assert.assertNull(nameAfter)

        saveRecord(name = name, notifyOnSuccess = false)
        Assert.assertTrue(nameAfter == name)
    }

    /**
     * 测试 update event
     */
    @Test
    fun updateEvent() {
        val name = Util.randomString()
        val nameUpdated = Util.randomString()
        subscribeAndWait(nameEqualTo = nameUpdated, event = SubscribeEvent.UPDATE)

        val record = saveRecord(name = name)
        Assert.assertNull(event.get())

        record.put(COL_NAME, nameUpdated)
        saveRecord(record = record)
        Assert.assertEquals(nameUpdated, nameAfter)
        Assert.assertEquals(name, nameBefore)
    }

    /**
     * 测试 delete event
     */
    @Test
    fun deleteEvent() {
        val name = Util.randomString()
        subscribeAndWait(nameEqualTo = name, event = SubscribeEvent.DELETE)

        val record = saveRecord(name = name)
        Assert.assertNull(event.get())

        deleteRecord(record)
        Assert.assertEquals(name, nameBefore)
    }


    /**
     * 一个简单的测试
     * 订阅 ON_CREATE/ON_UPDATE/ON_DELETE 三个事件
     * 然后执行 create/update/delete 操作
     * 最后判断是否收到这三个事件
     */
    @Test
    fun curd() {
        var initCount = 0
        val updateList = CopyOnWriteArrayList<String>()
        var exception: Throwable? = null
        val cb = object: SubscribeCallback {

            override fun onInit() {
                initCount++;
                Log.d(TAG, "init: $initCount")

                // create/update/delete 三个订阅者都订阅成功后，开个线程执行 CURD 操作
                if (initCount == 3) {
                    thread(start = true) {
                        try {
                            val record = table.createRecord()
                            record.put(COL_NAME, SubscribeEvent.CREATE.event)
                            record.save()
                            record.put(COL_NAME, SubscribeEvent.UPDATE.event)
                            record.save()
                            record.delete()
                        } catch (e: Exception) {
                            exception = e
                            cond.signalAll()
                        }
                    }
                }
            }

            /**
             * 另一个线程会执行 CURD 操作，这里应该会按顺序收到 create/update/delete 三个事件
             */
            override fun onEvent(event: SubscribeEventData) {
                Log.d(TAG, "onEvent: ${event.event}")
                when (event.event) {
                    SubscribeEvent.CREATE -> {
                        updateList.add(event.after?.getString(COL_NAME))
                    }

                    SubscribeEvent.UPDATE -> {
                        updateList.add(event.after?.getString(COL_NAME))
                    }

                    SubscribeEvent.DELETE -> {
                        updateList.add(SubscribeEvent.DELETE.event)
                        cond.signalAll()
                    }
                }
            }

            // 把异常记录起来，恢复测试线程
            override fun onError(tr: Throwable) {
                Log.d(TAG, "onError: ${tr.message}")
                exception = tr
                cond.signalAll()
            }
        }

        // 订阅后，阻塞测试线程
        table.subscribe(SubscribeEvent.CREATE, cb);
        table.subscribe(SubscribeEvent.UPDATE, cb);
        table.subscribe(SubscribeEvent.DELETE, cb);
        cond.await()

        // 测试线程恢复
        exception?.also { throw it }
        val expectedUpdateList = arrayOf(
            SubscribeEvent.CREATE.event, SubscribeEvent.UPDATE.event, SubscribeEvent.DELETE.event)
        Assert.assertArrayEquals(
            "update list not the same", expectedUpdateList, updateList.toTypedArray())
    }

    /**
     * 测试多个订阅的情况
     */
    @Test
    fun multiSubscriber() {
        val init = AtomicInteger(0)
        val event = AtomicInteger(0)
        val onInit = {
            init.incrementAndGet()
            cond.signalAll()
        }
        val onEvent: (SubscribeEventData) -> Unit = {
            event.incrementAndGet()
        }

        // 订阅一次
        subscribeAndWait(
            nameNotEqualTo = Util.randomString(),
            onInit = onInit,
            onEvent = onEvent)
        Assert.assertEquals(1, init.get())

        // 订阅两次
        subscribeAndWait(
            nameNotEqualTo = Util.randomString(),
            onInit = onInit,
            onEvent = onEvent)
        Assert.assertEquals(2, init.get())

        // 订阅三次
        subscribeAndWait(
            nameNotEqualTo = Util.randomString(),
            onInit = onInit,
            onEvent = onEvent)
        Assert.assertEquals(3, init.get())
        Assert.assertEquals(3, subscriptions.size)

        // 收到三次订阅事件
        saveRecord(name = Util.randomString(), notifyOnSuccess = false)
        Assert.assertEquals(3, event.get())

        // 移除一个订阅，收到两个订阅事件
        subscriptions[0].unsubscribe()
        event.set(0)
        cond.await(millisecond = 1000)
        saveRecord(name = Util.randomString(), notifyOnSuccess = false)
        Assert.assertEquals(2, event.get())

        // 再次移除一个订阅，收到一个订阅事件
        subscriptions[1].unsubscribe()
        event.set(0)
        cond.await(millisecond = 1000)
        saveRecord(name = Util.randomString(), notifyOnSuccess = false)
        Assert.assertEquals(1, event.get())

        // 移除全部订阅，不会收到订阅事件；而且自动释放 session
        subscriptions[2].unsubscribe()
        event.set(0)
        cond.await(millisecond = 1000)
        saveRecord(name = Util.randomString(), notifyOnSuccess = false)
        Assert.assertEquals(0, event.get())
        Assert.assertFalse(SessionManager.get().isConnected)
    }


    /**
     * 未登录的情况
     */
    @Test
    fun unSignIn() {
        try {
            Auth.logout()
            subscribeAndWait(exceptedException = SessionMissingException::class.java)
        } finally {
            signIn()
        }
    }

    /**
     * 当发生异常时（[SubscribeCallback.onError]），request 应该被 unsubscribe
     * 1）写入错误的 token，导致 onError
     * 2）断言有 exception 发生
     * 3）等待一会（unsubscribe 发生在 wamp thread），断言 request 已被 unsubscribe
     */
    @Test
    fun unsubscribeWhenOnError() {
        try {
            Auth.logout()
            Auth.signIn("000", "000", Long.MAX_VALUE)

            subscribeAndWait(exceptedException = UnauthorizedException::class.java)
            cond.await()
            Assert.assertFalse(subscriptions.firstOrNull()?.alive() == true)
        } finally {
            signIn()
        }
    }

    /**
     * 当所有的订阅请求都被取消后，自动关闭 session
     */
    @Test
    fun autoDisconnect() {
        subscribeAndWait(event = SubscribeEvent.CREATE)
        subscribeAndWait(event = SubscribeEvent.UPDATE)
        subscribeAndWait(event = SubscribeEvent.DELETE)

        Assert.assertTrue(sm.isConnected)
        subscriptions.forEach { it.unsubscribe() }
        cond.await()
        Assert.assertFalse(sm.isConnected)
    }

    /**
     * 测试订阅异常：表不存在
     */
    @Test
    fun invalidTable() {
        subscribeAndWait(
            tableName = Util.randomString(),
            exceptedException = SubscribeErrorException::class.java)
    }

    /**
     * reconnect 机制的测试
     * 1）正常订阅，然后发送 CLOSE 消息触发 reconnect
     * 2）等待个 2s，执行 CREATE，应该会收到事件
     * 3）作为对比，增加一个 disable reconnect 的对比项
     */
    @Test
    fun reconnect() {

        // disable reconnect 的情况
        SessionManager.ENABLE_RECONNECT = false
        subscribeAndWait()

        // 发送伪装的 CLOSE_CONNECTION_LOST，在 reconnect disable 的情况下会执行 onError
        postCloseConnection()
        cond.await()
        Assert.assertNotNull(exception.get())
        exception.set(null)

        // 开启 reconnect 后重新订阅
        SessionManager.ENABLE_RECONNECT = true
        subscribeAndWait()

        // 断开网络，等待一会待它恢复，这时不会收到异常
        postCloseConnection()
        cond.await()
        Assert.assertNull(exception.get())
        Assert.assertNull(event.get())

        // 执行 CURD，依然会收到订阅消息
        val content = Util.randomString()
        saveRecord(name = content, notifyOnSuccess = false)
        Assert.assertEquals(content, nameAfter)
    }

    private fun postCloseConnection(delay: Long = 1000) {
        Global.postDelayed({ closeConnection() }, delay)
    }

    private fun closeConnection() {
        val session = SessionManager::class.java.getDeclaredField("session")
            .apply { isAccessible = true }
            .let { (it.get(SessionManager.get()) as LazyProperty<*>).get() }

        val transport = session::class.java.getDeclaredField("transport")
            .apply { isAccessible = true }
            .get(session)

        val connection = transport::class.java.getDeclaredField("mConnection")
            .apply { isAccessible = true }
            .get(transport) as WebSocketConnection

        val onClose = WebSocketConnection::class.java.getDeclaredMethod(
            "onClose", Int::class.java, String::class.java)
            .apply { isAccessible = true }
        onClose.invoke(connection, IWebSocketConnectionHandler.CLOSE_CONNECTION_LOST, "")
    }

    private fun subscribeAndWait(
        tableName: String? = null,
        nameEqualTo: String? = null,
        nameNotEqualTo: String? = null,
        exceptedException: Class<*>? = null,
        event: SubscribeEvent = SubscribeEvent.CREATE,
        onInit: (() -> Unit)? = null,
        onEvent: ((SubscribeEventData) -> Unit)? = null
    ) {
        val query = where {
            if (nameEqualTo != null)
                equalTo(COL_NAME, nameEqualTo)
            else if (nameNotEqualTo != null)
                notEqualTo(COL_NAME, nameNotEqualTo)
        }
        val table = if (tableName.isNullOrEmpty()) this.table else Table(tableName)
        val subscription = table.subscribe(query, event, object : SubscribeCallback {
            override fun onInit() {
                if (onInit == null)
                    cond.signalAll()
                else
                    onInit.invoke()
            }

            override fun onEvent(event: SubscribeEventData) {
                if (onEvent == null) {
                    this@WebSocketTest.event.set(event)
                    cond.signalAll()
                } else {
                    onEvent.invoke(event)
                }
            }

            override fun onError(tr: Throwable) {
                exception.set(tr)
                cond.signalAll()
            }
        })
        subscriptions.add(subscription)
        cond.await()

        if (exceptedException == null)
            Assert.assertNull(exception.get())
        else
            Assert.assertTrue(exception.get()?.javaClass == exceptedException)
    }

    private fun saveRecord(name: String, notifyOnSuccess: Boolean = true): Record {
        val record = table.createRecord()
        record.put(COL_NAME, name).saveInBackground(object : BaseCallback<Record> {
            override fun onSuccess(t: Record?) {
                if (notifyOnSuccess)
                    cond.signalAll()
            }

            override fun onFailure(e: Throwable?) {
                exception.set(e)
                cond.signalAll()
            }
        })
        cond.await()
        Assert.assertNull(exception.get())
        return record
    }

    private fun saveRecord(record: Record) {
        record.saveInBackground(object : BaseCallback<Record> {
            override fun onSuccess(t: Record?) {}

            override fun onFailure(e: Throwable?) {
                exception.set(e)
                cond.signalAll()
            }
        })
        cond.await()
        Assert.assertNull(exception.get())
    }

    private fun deleteRecord(record: Record) {
        record.deleteInBackground(object : BaseCallback<Record> {
            override fun onSuccess(t: Record?) {}

            override fun onFailure(e: Throwable?) {
                exception.set(e)
                cond.signalAll()
            }
        })
        cond.await()
        Assert.assertNull(exception.get())
    }
}