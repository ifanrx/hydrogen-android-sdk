package com.minapp.android.sdk.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.minapp.android.sdk.Assert;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

public class InputStreamRequestBody extends RequestBody {

    private MediaType contentType;
    private InputStream in;

    /**
     *
     * @param contentType
     * @param in caller 负责关闭
     */
    public InputStreamRequestBody(@Nullable MediaType contentType, @NonNull InputStream in) {
        Assert.notNull(in, "InputStream");
        this.contentType = contentType;
        this.in = in;
    }

    public InputStreamRequestBody(@NonNull InputStream in) {
        this(null, in);
    }

    @Override
    public MediaType contentType() {
        return contentType;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Okio.buffer(Okio.source(in)).readAll(sink);
    }

    @Override
    public long contentLength() throws IOException {
        return in.available();
    }
}
