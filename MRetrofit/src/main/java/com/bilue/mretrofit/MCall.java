package com.bilue.mretrofit;

/**
 * Created by Bilue on 2017/3/19.
 */

//此处继承可复制接口的原因是一个call只能调用一次，所以在调用需要用clone的对象去调用。
//TODO 为什么只能调用一次 原因未知
public interface MCall<T> extends Cloneable {

    //异步操作所以需要回掉
    public void enqueue(MCallBack<T> callBack);


    //同步操作 可以直接把数据返回
    public MResponse<T> execute();
}
