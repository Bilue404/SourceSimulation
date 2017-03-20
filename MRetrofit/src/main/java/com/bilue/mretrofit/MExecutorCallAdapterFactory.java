package com.bilue.mretrofit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import okhttp3.Call;

/**
 * Created by Bilue on 2017/3/19.
 */

public class MExecutorCallAdapterFactory extends MCallAdapter.Factory{

    final Executor callBackExector;
    //回调用的executor 这个是android 的call所以这个executor 应该是里面有个MainLoop的Handler
    MExecutorCallAdapterFactory(Executor callBackExector){
        this.callBackExector = callBackExector;
    }


    //根据返回类型 注解 生产一个callAdapter
    //TODO 还需要看这里的逻辑
    @Override
    public MCallAdapter<?, ?> get(Type returnType, Annotation[] annotations, MRetrofit retrofit) {
        //检查是否 android 需要的call类型
        if (getRawType(returnType) != MCall.class){
            return null;
        }
        final Type responseType = MUtils.getCallResponseType(returnType);
        return new MCallAdapter<Object, MCall<?>>() {
            @Override public Type responseType() {
                return responseType;
            }

            @Override public MCall<Object> adapt(MCall<Object> call) {
                return new MExecutorCallbackCall<>(callBackExector, call);
            }
        };
    }

    //执行andriod平台真正的执行者，所以他应该持有一个实现了okhttp请求的okhttpcall的call 以及一个用于回调的ui线程的Executor
    static final class MExecutorCallbackCall<T> implements MCall<T>{
        private Executor callbackExecutor;
        private MCall<T> delegate;

        MExecutorCallbackCall(Executor callbackExecutor, MCall<T> delegate) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
        }


        @Override
        public void enqueue(final MCallBack<T> callBack) {
            //参数中的callBack是UI线程传递过来的， 所以需要一个内部匿名callback 然后用uiExecutor去转化成ui线程
            delegate.enqueue(new MCallBack<T>() {
                @Override
                public void onResponse(MCall<T> mCall, final MResponse<T> response) {
                    //子线程转UI线程
                   callbackExecutor.execute(new Runnable() {
                       @Override
                       public void run() {
                           if (delegate.isCanceled()) {
                               // Emulate OkHttp's behavior of throwing/delivering an IOException on cancellation.
                               callBack.onFailure(MExecutorCallbackCall.this, new IOException("Canceled"));
                           } else {
                               callBack.onResponse(MExecutorCallbackCall.this, response);
                           }
                       }
                   });

                }

                @Override
                public void onFailure(MCall<T> mCall, final Throwable throwable) {
                    callbackExecutor.execute(new Runnable() {
                        @Override public void run() {
                            callBack.onFailure(MExecutorCallbackCall.this, throwable);
                        }
                    });
                }
            });
        }

        @Override
        public boolean isExecuted() {
            return delegate.isExecuted();
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }

        @Override
        public boolean isCanceled() {
            return delegate.isCanceled();
        }

        @Override
        public MCall<T> clone() {
            return delegate.clone();
        }

        @Override
        public MResponse<T> execute() {
            return delegate.execute();
        }
    }
}
