package com.minapp.android.sdk.storage.model;

import com.google.gson.annotations.SerializedName;

public class UploadInfoReq {

    @SerializedName("filename")
    private String fileName;            // 上传的文件名

    @SerializedName("category_id")
    private String categoryId;          // 上传文件的所属分类，格式为文件分类的 ID 数组

    /**
     * 文件大小
     * 前端请求接口时，若需要使用大文件上传方式，则需要增加请求参数 file_size（文件大小）。
     * 根据又拍云文件上传说明文档说明，文件大小超过 100M 时，推荐使用大文件上传方式。
     */
    @SerializedName("file_size")
    private Integer fileSize;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }
}
