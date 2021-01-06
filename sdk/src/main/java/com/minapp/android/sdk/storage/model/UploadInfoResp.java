package com.minapp.android.sdk.storage.model;

import com.google.gson.annotations.SerializedName;

public class UploadInfoResp {

    @SerializedName("id")
    private String id;                                // 上传的文件 ID

    @SerializedName("policy")
    private String policy;                            // 文件上传配置

    @SerializedName("authorization")
    private String authorization;                     // 文件上传凭证

    @SerializedName("path")
    private String fileLink;                          // 文件上传成功后的访问地址

    @SerializedName("upload_url")
    private String uploadUrl;                         // 上传文件的目标地址


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    public String getFileLink() {
        return fileLink;
    }

    public void setFileLink(String fileLink) {
        this.fileLink = fileLink;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }
}
