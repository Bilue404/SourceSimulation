package com.bilue.mretrofit;

import okhttp3.ResponseBody;

/**
 * Created by Bilue on 2017/3/19.
 */

public class MResponse<T> {
    /**
     * 解析无返回成功
     */
    public static <T> MResponse<T> success(T body, okhttp3.Response rawResponse) {
        if (rawResponse == null) throw new NullPointerException("rawResponse == null");
        if (!rawResponse.isSuccessful()) {
            throw new IllegalArgumentException("rawResponse must be successful response");
        }
        return new MResponse<>(rawResponse, body, null);
    }


    private final okhttp3.Response rawResponse;
    private final T body;
    private final ResponseBody errorBody;

    public MResponse(okhttp3.Response rawResponse, T body, ResponseBody errorBody){
        this.rawResponse = rawResponse;
        this.body = body;
        this.errorBody = errorBody;

    }


    public int getCode(){
        return 1;
    }

    public String getMseeage(){
        return "";
    }

    public T getBody(){
        return body ;
    }




}
