package com.bilue.mretrofit;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

import static com.bilue.mretrofit.MServiceMethod.PARAM_URL_REGEX;

/**
 * Created by bilue on 17/3/21.
 */

public class MRequestBuilder {
    private static final char[] HEX_DIGITS =
            { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    private static final String PATH_SEGMENT_ALWAYS_ENCODE_SET = " \"<>^`{}|\\?#";

    private final String method;

    private final HttpUrl baseUrl;
    private String relativeUrl;
    private HttpUrl.Builder urlBuilder;

    private final Request.Builder requestBuilder;
    private MediaType contentType;

    private final boolean hasBody;
    private MultipartBody.Builder multipartBuilder;
    private FormBody.Builder formBuilder;
    private RequestBody body;

    MRequestBuilder(String method, HttpUrl baseUrl, String relativeUrl, Headers headers,
                   MediaType contentType, boolean hasBody, boolean isFormEncoded, boolean isMultipart) {
        this.method = method;
        this.baseUrl = baseUrl;
        this.relativeUrl = relativeUrl;
        this.requestBuilder = new Request.Builder();
        this.contentType = contentType;
        this.hasBody = hasBody;

        if (headers != null) {
            requestBuilder.headers(headers);
        }

        //如果是表单提交
        if (isFormEncoded) {
            formBuilder = new FormBody.Builder();
        } else if (isMultipart) {
            //文件提交
            multipartBuilder = new MultipartBody.Builder();
            multipartBuilder.setType(MultipartBody.FORM);
        }
    }





}
