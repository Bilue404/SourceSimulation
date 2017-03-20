package com.bilue.mretrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

/**
 * Created by Bilue on 2017/3/19.
 */

public class MExecutorCallAdapterFactory extends MCallAdapter.Factory{

    //回调用的executor 这个是android 的call所以这个executor 应该是里面有个MainLoop的Handler
    MExecutorCallAdapterFactory(Executor callBackExector){

    }

    @Override
    public MCallAdapter<?, ?> get(Type returnType, Annotation[] annotations, MRetrofit retrofit) {
        return null;
    }
}
