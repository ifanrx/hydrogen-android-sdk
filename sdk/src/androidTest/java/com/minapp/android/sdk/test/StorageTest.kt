package com.minapp.android.sdk.test

import android.content.ContentResolver
import android.net.Uri
import android.os.Process
import com.minapp.android.sdk.BaaS
import com.minapp.android.sdk.database.query.Query
import com.minapp.android.sdk.exception.HttpException
import com.minapp.android.sdk.storage.CloudFile
import com.minapp.android.sdk.storage.FileCategory
import com.minapp.android.sdk.storage.Storage
import com.minapp.android.sdk.test.base.BaseAuthedTest
import okio.Okio
import org.junit.Assert
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.sin

/**
 * 文件模块测试
 * requirement:
 * 1）需要有个名为 [CATEGORY_NAME] 的文件目录
 */
class StorageTest: BaseAuthedTest() {

    companion object {
        private const val CATEGORY_NAME = "android_test_category"
        private const val FILE_NAME = "cloud.jpg"

        private val FILE_DATA: ByteArray
        get() {
            val uri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(ctx.packageName)
                .path(R.raw.cloud.toString())
                .build()
            return ctx.contentResolver.openInputStream(uri).let { it.readBytes() }
        }

        private lateinit var category: FileCategory
        private lateinit var cloudFile: CloudFile

        fun isCategoryInit(): Boolean {
            return this::category.isInitialized
        }

        fun isCloudFileInit(): Boolean {
            return this::cloudFile.isInitialized
        }
    }

    /**
     * 测试目录列表
     */
    @Test
    fun categoryListTest() {
        category = Storage.categories(Query()).objects.find { it.name == CATEGORY_NAME }!!
    }

    /**
     * 根据 id 获取目录
     */
    @Test
    fun categorybyIdTest() {
        if (!isCategoryInit()) categoryListTest()
        assertEquals(Storage.category(category.id).id, category.id)
    }

    /**
     * 上传文件
     */
    @Test
    fun uploadFileTest() {
        if (!isCategoryInit()) categoryListTest()
        cloudFile = Storage.uploadFile(FILE_NAME, category.id, FILE_DATA)!!
    }

    /**
     * 文件列表
     */
    @Test
    fun fileListTest() {
        if (!isCategoryInit()) categoryListTest()
        val query = Query().apply {
            put(CloudFile.QUERY_CATEGORY_ID, category.id)
        }
        assertNotNull(Storage.files(query).objects.find { it.name == FILE_NAME })
    }

    /**
     * 根据 id 获取文件
     */
    @Test
    fun fileByIdTest() {
        if (!isCloudFileInit()) uploadFileTest()
        assertEquals(Storage.file(cloudFile.id).name, cloudFile.name)
    }

    /**
     * 删除文件
     */
    @Test
    fun deleteFileTest() {
        if (!isCloudFileInit()) uploadFileTest()
        Storage.deleteFiles(listOf(cloudFile.id))

        // 删除后，应该返回 404 才正常
        try {
            Storage.file(cloudFile.id)
        } catch (e: HttpException) {
            if (e.code == 404)
                return
        }
        throw IllegalStateException()
    }

    /**
     * 测试上传大文件（待服务端上线后测试）
     * 太耗时间和流量了，平时一般不测试此情况
     */
    fun uploadBigFile() {
        if (!isCategoryInit()) categoryListTest()

        // 创建 1GB 的大文件
        val f = File(ctx.externalCacheDir, "big_file_test_upload")
        if (!f.exists()) {
            f.createNewFile()
            val sink = Okio.buffer(Okio.sink(f))
            val buffer = ByteBuffer.wrap(ByteArray(1024 * 1024) { 0 })
            repeat(1000) {
                buffer.clear()
                sink.write(buffer)
            }
            sink.flush()
        }

        var cloudFile: CloudFile? = null
        f.inputStream().use { cloudFile = Storage.uploadFile(f.name, category.id, it) }
        assertFalse(cloudFile?.path.isNullOrEmpty())
    }
}
