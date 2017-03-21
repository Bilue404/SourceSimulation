package com.bilue.mretrofit;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;

/**
 * Created by Bilue on 2017/3/19.
 */

public class MOkHttpCall<T> implements MCall<T> {

    //当前的serviceApi方法执行者。里面有okhttpclient的引用等 可以用来创建call
    private final MServiceMethod serviceMethod;
    private final Object[] args; //servicesApi.xxx(userName); 传递过来的参数  后续用于在serviceMethod里面生成一个request
    private okhttp3.Call rawCall;//当前执行的Call 可以用来取消请求等
    private volatile boolean canceled;//可能其他线程中取消了请求，所以需要用volatile 立马更新缓存中canceled的值，让其他线程查询的时候得到最新的值。
    private boolean executed;//一个call只能执行一次 用于判断是否已经执行过了

    public MOkHttpCall(MServiceMethod serviceMethod,Object[] args) {
        this.serviceMethod = serviceMethod;
        this.args = args;
    }

    public void enqueue(final MCallBack<T> callBack) {
        if (callBack == null) {
            throw new NullPointerException("callBack should be null");
        }

        okhttp3.Call call;
//        Throwable throwAble;
        synchronized (this){
            if (executed) throw new IllegalStateException("Already executed.");

            executed = true;
            //TODO 上面有个request的方法还不知道什么时候调用到， 调用 之后rowCall应该就是不是空了
            call = rawCall;
            if (call == null) {
                call = rawCall = creatCall();
            }
            //请求之前检查是否取消了请求
            if (canceled) return;

            call.enqueue(new Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    callBack.onFailure(MOkHttpCall.this,e);
                }

                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                    callBack.onResponse(MOkHttpCall.this,parseResponse(response));
                }
            });
        }

    }



    @Override
    public MResponse<T> execute() throws IOException {
        okhttp3.Call call;

        synchronized (this) {
            if (executed) throw new IllegalStateException("Already executed.");
            executed = true;

            //TODO 这个exception为什么是全局的原因暂时未知
//            if (creationFailure != null) {
//                if (creationFailure instanceof IOException) {
//                    throw (IOException) creationFailure;
//                } else {
//                    throw (RuntimeException) creationFailure;
//                }
//            }

            call = rawCall;
            if (call == null) {
                try {
                    call = rawCall = creatCall();
                } catch (RuntimeException e) {
//                    creationFailure = e;
                    throw e;
                }
            }
        }

        if (canceled) {
            call.cancel();
        }

        return parseResponse(call.execute());

    }


    private okhttp3.Call creatCall(){
//        Request request = new Request.Builder().url("https://api.github.com/users/octocat/repos").build();

        Request request = serviceMethod.toRequest();
        //用户配置可配置httpclient 所以这里的okhttpclient是 serviceMethod里面的retrofit对象里面的okhttpclient
        okhttp3.Call call = serviceMethod.callFactory.newCall(request);
        if (call == null) {
            throw new NullPointerException("Call.Factory returned null.");
        }
        return call;
    }

    //有可能是执行executed的时候检查，也有可能是enqueue的时候检查 所以需要加锁
    public synchronized  boolean isExecuted(){
        return executed;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public MCall<T> clone() {
        return null;
    }


    //将OkHttp的response 解析转换成封装好了之后的response
    private MResponse<T> parseResponse(okhttp3.Response rawResponse){
        ResponseBody rawBody = rawResponse.body();
        //TODO 暂时不知道这个自定义返回body的意义
        rawResponse = rawResponse.newBuilder()
                .body(new NoContentResponseBody(rawBody.contentType(),rawBody.contentLength()))
                .build();
        int code = rawResponse.code();
        //如果返回失败，比如403无权限之类的
        if (code < 200 || code >= 300) {
            try {
                //TODO 返回一个失败的response
//                ResponseBody bufferedBody = Utils.buffer(rawBody);
//                return MResponse.error(bufferedBody, rawResponse);
            } finally {
                rawBody.close();
            }
        }
        //处理成功 204（无返回内容）， 205（重置成功，但是无返回内容）
        if (code == 204 || code == 205) {
            rawBody.close();
            return MResponse.success(null, rawResponse);
        }

        //TODO 意义暂时未知
        ExceptionCatchingRequestBody catchBody = new ExceptionCatchingRequestBody(rawBody);
        //TODO 转换的过程。
        T body = null;
        return MResponse.success(body,rawResponse);

    }

    //自定义ResponseBody的格式， 用于方便后续解析成封装的类型
    private final class NoContentResponseBody extends ResponseBody{
        private final MediaType contentType;
        private final long contentLength;

        NoContentResponseBody(MediaType contentType, long contentLength) {
            this.contentType = contentType;
            this.contentLength = contentLength;
        }
        @Override
        public MediaType contentType() {
            return contentType;
        }

        @Override
        public long contentLength() {
            return contentLength;
        }

        //TODO 解析失败？ 需要先去看自定义ResponseBody有关的东西
        @Override
        public BufferedSource source() {
            throw new IllegalStateException("Cannot read raw response body of a converted body.");
        }
    }

    //TODO 重新定义responseBody 意义暂时未知
    static final class ExceptionCatchingRequestBody extends ResponseBody {
        private final ResponseBody delegate;
        IOException thrownException;

        ExceptionCatchingRequestBody(ResponseBody delegate) {
            this.delegate = delegate;
        }

        @Override public MediaType contentType() {
            return delegate.contentType();
        }

        @Override public long contentLength() {
            return delegate.contentLength();
        }

        //TODO 这个方法作用未知
        @Override public BufferedSource source() {
            return Okio.buffer(new ForwardingSource(delegate.source()) {
                @Override public long read(Buffer sink, long byteCount) throws IOException {
                    try {
                        return super.read(sink, byteCount);
                    } catch (IOException e) {
                        thrownException = e;
                        throw e;
                    }
                }
            });
        }

        @Override public void close() {
            delegate.close();
        }

        void throwIfCaught() throws IOException {
            if (thrownException != null) {
                throw thrownException;
            }
        }
    }
}
