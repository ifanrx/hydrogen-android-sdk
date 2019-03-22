package com.minapp.android.example

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class BaseActivity: AppCompatActivity() {

    private val job = Job()
    protected val activityScope = CoroutineScope(job + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}