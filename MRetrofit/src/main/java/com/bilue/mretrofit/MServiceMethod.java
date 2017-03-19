package com.bilue.mretrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Converter;

/**
 * Created by Bilue on 2017/3/19.
 *
 * 当前理解 一个services API  对应一个 serviceMethon 。
 */

public class MServiceMethod<R,T>{
    //用于正则匹配占位符{}
    static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");
    static final Pattern PARAM_NAME_REGEX = Pattern.compile(PARAM);
    //okhttpclient
    final okhttp3.Call.Factory callFactory;
    final MCallAdapter<R, T> callAdapter;

    private final HttpUrl baseUrl;
//    private final Converter<ResponseBody, R> responseConverter;
    private final String httpMethod;
    private final String relativeUrl;
    private final Headers headers;
    private final MediaType contentType;
    private final boolean hasBody;
    private final boolean isFormEncoded;
    private final boolean isMultipart;
//    private final ParameterHandler<?>[] parameterHandlers;


    MServiceMethod(Builder builder) {
        this.callFactory = builder.mRetrofit.callFactory();
//        this.callAdapter = builder.callAdapter;
        this.baseUrl = builder.mRetrofit.baseUrl();
//        this.responseConverter = builder.responseConverter;
        this.httpMethod = builder.httpMethod;
        this.relativeUrl = builder.relativeUrl;
        this.headers = builder.headers;
        this.contentType = builder.contentType;
        this.hasBody = builder.hasBody;
        this.isFormEncoded = builder.isFormEncoded;
        this.isMultipart = builder.isMultipart;
//        this.parameterHandlers = builder.parameterHandlers;
        callAdapter = null;
    }



    static final class Builder<R,T>{
        private MRetrofit mRetrofit; //retrofit 的引用，里面有okhttpclient，baseurl之类的对象
        private Method method;  //当前运行到的方法
        private Annotation[] methodAnnotations; //当前方法的注解
        final Type[] parameterTypes;//方法的参数
        final Annotation[][] parameterAnnotationsArray; //方法参数的注解
        private String httpMethod; //注解上的解析完的方法
        private String relativeUrl;// TODO 暂时理解 解析完正真的地址
        private Headers headers;
        private MediaType contentType;
        private boolean hasBody;
        private boolean isFormEncoded;
        private boolean isMultipart;
        MCallAdapter<R, T> callAdapter; //用于将Call的 实现者 Http的代理类 转化成 所需要的 实际call

        Builder(MRetrofit retrofit, Method method) {
            this.mRetrofit = retrofit;
            this.method = method;
            this.methodAnnotations = method.getAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
            this.parameterAnnotationsArray = method.getParameterAnnotations();
        }
        public MServiceMethod build(){
            callAdapter = createCallAdapter();
            return new MServiceMethod(this);
        }

        //创建一个 设配器  用于 适配 当前请求所需要的返回类型
        private MCallAdapter<R,T> createCallAdapter(){
            Type returnType = method.getGenericReturnType();
            //检查返回类型是否是可以处理的值 数组，公共接口？ 通配符 不可以处理 TODO 此处还需要系统的学习一下反射
            if (MUtils.hasUnresolvableType(returnType)) {
                throw methodError(
                        "Method return type must not include a type variable or wildcard: %s", returnType);
            }
            if (returnType == void.class) {
                throw methodError("Service methods cannot return void.");
            }
            Annotation[] annotations = method.getAnnotations();
            try {
                //因为设配器支持用户配置， 所以需要绕回去 通过retrofit拿到设配器。
                return (MCallAdapter<T, R>) MRetrofit.callAdapter(returnType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw methodError(e, "Unable to create call adapter for %s", returnType);
            }
        }

        private RuntimeException methodError(String message, Object... args) {
            return methodError(null, message, args);
        }

        private RuntimeException methodError(Throwable cause, String message, Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message
                    + "\n    for method "
                    + method.getDeclaringClass().getSimpleName()
                    + "."
                    + method.getName(), cause);
        }

    }



}
