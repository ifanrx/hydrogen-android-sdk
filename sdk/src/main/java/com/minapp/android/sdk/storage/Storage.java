package com.minapp.android.sdk.storage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.minapp.android.sdk.Global;
import com.minapp.android.sdk.database.query.Query;
import com.minapp.android.sdk.exception.HttpException;
import com.minapp.android.sdk.storage.model.BatchDeleteReq;
import com.minapp.android.sdk.storage.model.UploadInfoReq;
import com.minapp.android.sdk.storage.model.UploadInfoResp;
import com.minapp.android.sdk.util.BaseCallback;
import com.minapp.android.sdk.util.InputStreamRequestBody;
import com.minapp.android.sdk.util.PagedList;
import com.minapp.android.sdk.util.Util;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;

public abstract class Storage {

    static final String PART_AUTHORIZATION = "authorization";
    static final String PART_POLICY = "policy";
    static final String PART_FILE = "file";

    /**
     * 文件上传，分两步：<br />
     * 1. 获取上传文件所需授权凭证和上传地址<br />
     * 2. 使用上一步获取的授权凭证和上传地址，进行文件上传
     */
    private static UploadInfoResp _uploadFile(
            String filename,
            String categoryId,
            InputStream in
    ) throws Exception {
        UploadInfoReq body = new UploadInfoReq();
        body.setFileName(filename);
        body.setCategoryId(categoryId);

        int size = in.available();
        if (size / 1024 / 1024 > 100) {
            body.setFileSize(size);
        }

        UploadInfoResp meta = Global.httpApi().getUploadMeta(body).execute().body();

        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(PART_AUTHORIZATION, meta.getAuthorization())
                .addFormDataPart(PART_POLICY, meta.getPolicy())
                .addFormDataPart(PART_FILE, filename, new InputStreamRequestBody(in))
                .build();
        Global.uploadHttpApi().uploadFile(meta.getUploadUrl(), multipartBody).execute();
        return meta;
    }

    /**
     * 文件上传
     * @return {@link CloudFile#getId()}
     */
    public static String uploadFileWithoutFetch(
            String filename,
            String categoryId,
            InputStream in
    ) throws Exception {
        return _uploadFile(filename, categoryId, in).getId();
    }

    /**
     * 文件上传
     * @return {@link CloudFile#getId()}
     */
    public static String uploadFileWithoutFetch(
            String filename,
            String categoryId,
            byte[] data
    ) throws Exception {
        InputStream in = null;
        try {
            in = new ByteArrayInputStream(data);
            return uploadFileWithoutFetch(filename, categoryId, in);
        } finally {
            Util.closeQuietly(in);
        }
    }

    /**
     * 文件上传
     * @return {@link CloudFile#getId()}
     */
    public static String uploadFileWithoutFetch(String filename, byte[] data) throws Exception {
        return uploadFileWithoutFetch(filename, null, data);
    }

    /**
     * 文件上传
     * @param cb 拿到 {@link CloudFile#getId()}
     */
    public static void uploadFileWithoutFetchInBackground(
            final String filename, final String categoryId, final byte[] data, @NonNull final BaseCallback<String> cb) {
        Util.inBackground(cb, new Callable<String>() {
            @Override
            public String call() throws Exception {
                return Storage.uploadFileWithoutFetch(filename, categoryId, data);
            }
        });
    }

    public static @Nullable CloudFile uploadFile(
            String filename,
            String categoryId,
            InputStream in
    ) throws Exception {
        UploadInfoResp meta = _uploadFile(filename, categoryId, in);
        while (true) {
            try {
                return file(meta.getId());
            } catch (HttpException e) {
                if (e.getCode() == 404) {
                    Thread.sleep(500);
                } else {
                    throw e;
                }
            }
        }
    }

    public static CloudFile uploadFile(
            String filename,
            String categoryId,
            byte[] data
    ) throws Exception {
        InputStream in = null;
        try {
            in = new ByteArrayInputStream(data);
            return uploadFile(filename, categoryId, in);
        } finally {
            Util.closeQuietly(in);
        }
    }

    public static CloudFile uploadFile(String filename, byte[] data) throws Exception {
        return uploadFile(filename, null, data);
    }

    /**
     * 文件上传
     * @param cb 拿到 {@link CloudFile}
     */
    public static void uploadFileAndFetchInBackground(
            final String filename, final String categoryId, final byte[] data, @NonNull final BaseCallback<CloudFile> cb) {
        Util.inBackground(cb, new Callable<CloudFile>() {
            @Override
            public CloudFile call() throws Exception {
                return Storage.uploadFile(filename, categoryId, data);
            }
        });
    }


    /**
     * 文件信息
     * @param id
     * @return
     * @throws Exception
     */
    public static CloudFile file(String id) throws Exception {
        return Global.httpApi().file(id).execute().body();
    }

    public static void fileInBackground(final String id, @NonNull BaseCallback<CloudFile> cb) {
        Util.inBackground(cb, new Callable<CloudFile>() {
            @Override
            public CloudFile call() throws Exception {
                return Storage.file(id);
            }
        });
    }


    /**
     * 查询文件列表
     * @throws Exception
     */
    public static PagedList<CloudFile> files(Query query) throws Exception {
        return Global.httpApi().files(query != null ? query : new Query()).execute().body().readonly();
    }


    public static void filesInBackground(final Query query, @NonNull BaseCallback<PagedList<CloudFile>> cb) {
        Util.inBackground(cb, new Callable<PagedList<CloudFile>>() {
            @Override
            public PagedList<CloudFile> call() throws Exception {
                return Storage.files(query);
            }
        });
    }

    /**
     * 批量删除文件
     * @param ids
     * @throws Exception
     */
    public static void deleteFiles(Collection<String> ids) throws Exception {
        if (ids == null || ids.size() == 0) {
            return;
        }

        if (ids.size() == 1) {
            Global.httpApi().deleteFile(ids.iterator().next()).execute();
        } else {
            Global.httpApi().deleteFiles(new BatchDeleteReq(ids)).execute();
        }
    }

    public static void deleteFilesInBackground(final Collection<String> ids, @NonNull BaseCallback<Void> cb) {
        Util.inBackground(cb, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Storage.deleteFiles(ids);
                return null;
            }
        });
    }

    /**
     * 获取分类
     * @param id
     * @return
     * @throws Exception
     */
    public static FileCategory category(String id) throws Exception {
        return Global.httpApi().fileCategory(id).execute().body();
    }

    public static void categoryInBackground(final String id, @NonNull BaseCallback<FileCategory> cb) {
        Util.inBackground(cb, new Callable<FileCategory>() {
            @Override
            public FileCategory call() throws Exception {
                return Storage.category(id);
            }
        });
    }

    /**
     * 列表查询分类
     * @return
     * @throws Exception
     */
    public static PagedList<FileCategory> categories(Query query) throws Exception {
        return Global.httpApi().fileCategories(query).execute().body().readonly();
    }

    public static void categoriesInBackground(final Query query, @NonNull BaseCallback<PagedList<FileCategory>> cb) {
        Util.inBackground(cb, new Callable<PagedList<FileCategory>>() {
            @Override
            public PagedList<FileCategory> call() throws Exception {
                return Storage.categories(query);
            }
        });
    }

}
