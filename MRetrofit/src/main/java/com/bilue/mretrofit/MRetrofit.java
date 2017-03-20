package com.bilue.mretrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.internal.platform.Platform;
import retrofit2.CallAdapter;

import static com.bilue.mretrofit.MUtils.checkNotNull;
import static java.util.Collections.unmodifiableList;

/**
 * Created by Bilue on 2017/3/19.
 */

public class MRetrofit {

    //存放了 serviceMethod的缓存
    private final Map<Method, MServiceMethod> serviceMethodCache = new ConcurrentHashMap<>();


    private Call.Factory callFactory;//生产Call 的工厂， okhttpclient实现了这个接口，所以在这里其实他就是okhttpclient

    private HttpUrl baseUrl;    //okhttp的url
    final List<MCallAdapter.Factory> adapterFactories; //适配器 用于将请求后的OkHttpCall 转化成android Call，Rx的Observable，或者用户自己定义的类型
    private boolean  validateEagerly;  //TODO 暂时不知道意义  大概是  是否提前对业务接口中的注解进行验证转换的标志位
    final Executor callbackExecutor;//可供用户配置的Exector ，如果用户没有配置 则直接使用Platform里面的exector


    MRetrofit(okhttp3.Call.Factory callFactory, HttpUrl baseUrl, List<MCallAdapter.Factory> adapterFactories, Executor callbackExecutor,boolean validateEagerly) {
        this.callFactory = callFactory;
        this.baseUrl = baseUrl;
//        this.converterFactories = unmodifiableList(converterFactories); // Defensive copy at call site.
        this.adapterFactories = unmodifiableList(adapterFactories); // Defensive copy at call site.
//        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
        this.callbackExecutor = callbackExecutor;
    }

    public Call.Factory callFactory() {
        return callFactory;
    }

    public HttpUrl baseUrl(){
        return baseUrl;
    }

    public <T>T create(final Class<T> serives){
        MUtils.validateServiceInterface(serives);
        if (validateEagerly) {
//            eagerlyValidateMethods(service);
        }
        //这里的逻辑就是拦截接口的方法，然后根据方法的注解，参数等， 生成一个OkHttpCall 然后将这个OKHttpCall适配成 android的Call（在main线程的call） 或者 RxJava的call 并返回出去
        return (T)Proxy.newProxyInstance(serives.getClassLoader(), new Class[]{serives}, new InvocationHandler() {
            //用于判断是JAVA 还是android 平台
            private final MPlatform platform = MPlatform.get();
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(this, args);
                }
//                if (platform.isDefaultMethod(method)) {
//                    return platform.invokeDefaultMethod(method, service, proxy, args);
//                }
                //加载 当前api方法的 serviceMethod 并解析他的请求，参数，注解等一系列动作
                MServiceMethod serviceMethod = loadServiceMethod(method);
                //当前是httpCall 需要在进行一次适配， 将httpCall转化成外部需要的 接口请求， 因为Android的T是Call但是RxJava的T的是Observable
                MOkHttpCall okHttpCall = new MOkHttpCall(serviceMethod,args);
                //因为serviceMethod 里面有解析完的参数 以及返回参数等。所以是在他里面适配
                return serviceMethod.callAdapter.adapt(okHttpCall);
            }
        });
    }


    //一个请求 会从用户配的适配器列表拿出设配器出来
    public MCallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }


    public MCallAdapter<?, ?> nextCallAdapter(CallAdapter.Factory skipPast, Type returnType,
                                             Annotation[] annotations) {
        checkNotNull(returnType, "returnType == null");
        checkNotNull(annotations, "annotations == null");

        //TODO skipPath暂时不知道作用。
        int start = adapterFactories.indexOf(skipPast) + 1;
        //遍历适配器工厂， 在需要的时候才会生成adapter出来
        for (int i = start, count = adapterFactories.size(); i < count; i++) {
            MCallAdapter<?, ?> adapter = adapterFactories.get(i).get(returnType, annotations, this);
            if (adapter != null) {
                return adapter;
            }
        }
        //报错的信息
        StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
                .append(returnType)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(adapterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = adapterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(adapterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }


    //根据当前方法加载 serviceMethod
    private MServiceMethod loadServiceMethod(Method method){
        MServiceMethod result = serviceMethodCache.get(method);
        if (result != null) {
            return result;
        }
        //TODO 此处不知道为何要加锁 需要检查是否有其他线程也load了
        synchronized (serviceMethodCache) {
            result = serviceMethodCache.get(method);
            if (result == null) {
                //需要把retrofit 传递过去，因为需要它的baseUrl等多个参数
                result = new MServiceMethod.Builder(this, method).build();
                serviceMethodCache.put(method, result);
            }
        }
        return result;
    }




    public final static class Builder{
        //平台 Java8 之类的
        private final MPlatform platform = MPlatform.get();
        private okhttp3.Call.Factory callFactory;
        private HttpUrl baseUrl;
        private Executor callbackExecutor;
        private final List<MCallAdapter.Factory> adapterFactories = new ArrayList<>();

        public Builder client(OkHttpClient client) {
            return callFactory(checkNotNull(client, "client == null"));
        }
        public Builder callFactory(okhttp3.Call.Factory factory) {
            this.callFactory = checkNotNull(factory, "factory == null");
            return this;
        }
        public Builder baseUrl(String baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            HttpUrl httpUrl = HttpUrl.parse(baseUrl);
            if (httpUrl == null) {
                throw new IllegalArgumentException("Illegal URL: " + baseUrl);
            }
            return baseUrl(httpUrl);
        }
        public Builder baseUrl(HttpUrl baseUrl) {
            checkNotNull(baseUrl, "baseUrl == null");
            List<String> pathSegments = baseUrl.pathSegments();
            if (!"".equals(pathSegments.get(pathSegments.size() - 1))) {
                throw new IllegalArgumentException("baseUrl must end in /: " + baseUrl);
            }
            this.baseUrl = baseUrl;
            return this;
        }

        //可以添加其他的call类型  用以支持其他类型
        public Builder addCallAdapterFactory(MCallAdapter.Factory factory) {
            adapterFactories.add(checkNotNull(factory, "factory == null"));
            return this;
        }
        public MRetrofit build(){

            if (baseUrl == null) {
                throw new IllegalStateException("Base URL required.");
            }
            Call.Factory callFactory = this.callFactory;
            if (callFactory == null) {
                callFactory = new OkHttpClient();
            }
            // Make a defensive copy of the adapters and add the default Call adapter.
            List<MCallAdapter.Factory> adapterFactories = new ArrayList<>(this.adapterFactories);
//            adapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));
            //TODO 这里应该和平台有关才对，暂时先用固定的
            Executor callbackExecutor = this.callbackExecutor;
            if (callbackExecutor == null) {
                callbackExecutor = platform.defaultCallbackExecutor();
            }
            adapterFactories.add(platform.defaultCallAdapterFactory(callbackExecutor));
            return new MRetrofit(callFactory,baseUrl,adapterFactories,callbackExecutor,false);
        }

    }



}
