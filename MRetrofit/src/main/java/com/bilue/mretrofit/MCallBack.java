package com.bilue.mretrofit;

/**
 * Created by Bilue on 2017/3/19.
 */

public interface MCallBack<T> {

    void onResponse(MCall<T> mCall, MResponse<T> response);

    void onFailure(MCall<T> mCall, Throwable throwable);
}
