package com.minapp.android.sdk.test.util

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

class SimpleCondition {

    private val lock by lazy { ReentrantLock() }
    private val cond by lazy { lock.newCondition() }

    fun await(millisecond: Long = 5000) {
        try {
            lock.lock()
            if (millisecond <= 0)
                cond.awaitUninterruptibly()
            else
                cond.await(millisecond, TimeUnit.MILLISECONDS)
        } finally {
            lock.unlock()
        }
    }

    fun signalAll() {
        lock.lock()
        cond.signalAll()
        lock.unlock()
    }

}