package com.bilue.mretrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import retrofit2.http.DELETE;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.HTTP;
import retrofit2.http.Multipart;
import retrofit2.http.OPTIONS;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;

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
    final okhttp3.Call.Factory callFactory;    //okhttpclient
    final MCallAdapter<T, R> callAdapter; //call的设配器

    private final HttpUrl baseUrl;
//    private final Converter<ResponseBody, R> responseConverter;
    private final String httpMethod;//请求的方法
    private final String relativeUrl;//实际链接
    private final Headers headers;
    private final MediaType contentType;
    private final boolean hasBody;
    private final boolean isFormEncoded;
    private final boolean isMultipart;
    private final MParameterHandler<?>[] parameterHandlers;//参数解析器


    MServiceMethod(Builder builder) {
        this.callFactory = builder.mRetrofit.callFactory();
        this.callAdapter = builder.callAdapter;
        this.baseUrl = builder.mRetrofit.baseUrl();
//        this.responseConverter = builder.responseConverter;
        this.httpMethod = builder.httpMethod;
        this.relativeUrl = builder.relativeUrl;
        this.headers = builder.headers;
        this.contentType = builder.contentType;
        this.hasBody = builder.hasBody;
        this.isFormEncoded = builder.isFormEncoded;
        this.isMultipart = builder.isMultipart;
        this.parameterHandlers = builder.parameterHandlers;
    }


    public Request toRequest(){
        MRequestBuilder requestBuilder = new MRequestBuilder(httpMethod, baseUrl, relativeUrl, headers,
                contentType, hasBody, isFormEncoded, isMultipart);
        MParameterHandler<Object>[] handlers = (MParameterHandler<Object>[]) parameterHandlers;

    }

    static Set<String> parsePathParameters(String path) {
        Matcher m = PARAM_URL_REGEX.matcher(path);
        Set<String> patterns = new LinkedHashSet<>();
        while (m.find()) {
            patterns.add(m.group(1));
        }
        return patterns;
    }

    static final class Builder<T,R>{
        private MRetrofit mRetrofit; //retrofit 的引用，里面有okhttpclient，baseurl之类的对象
        private Method method;  //当前运行到的方法
        private Annotation[] methodAnnotations; //当前方法的注解
        final Type[] parameterTypes;//方法的参数
        final Annotation[][] parameterAnnotationsArray; //方法参数的注解
        private String httpMethod; //注解上的解析完的方法
        private String relativeUrl;// TODO 暂时理解 解析完真正的地址
        private Headers headers;
        private MediaType contentType;
        private boolean hasBody;
        private boolean isFormEncoded;
        private boolean isMultipart;
        MParameterHandler<?>[] parameterHandlers;
        Set<String> relativeUrlParamNames;

        MCallAdapter<T, R> callAdapter; //用于将Call的 实现者 Http的代理类 转化成 所需要的 实际call

        Builder(MRetrofit retrofit, Method method) {
            this.mRetrofit = retrofit;
            this.method = method;
            this.methodAnnotations = method.getAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
            this.parameterAnnotationsArray = method.getParameterAnnotations();
        }
        public MServiceMethod build(){
            callAdapter = createCallAdapter();

            for (Annotation annotation : methodAnnotations) {
                parseMethodAnnotation(annotation);
            }
            if (!hasBody) {
                if (isMultipart) {
                    throw methodError(
                            "Multipart can only be specified on HTTP methods with request body (e.g., @POST).");
                }
                if (isFormEncoded) {
                    throw methodError("FormUrlEncoded can only be specified on HTTP methods with "
                            + "request body (e.g., @POST).");
                }
            }
            int parameterCount = parameterAnnotationsArray.length;//有多少个注解方法
            parameterHandlers = new MParameterHandler<?>[parameterCount];

            if (httpMethod == null) {
                throw methodError("HTTP method annotation is required (e.g., @GET, @POST, etc.).");
            }

            return new MServiceMethod(this);
        }
        private void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof DELETE) {
                parseHttpMethodAndPath("DELETE", ((DELETE) annotation).value(), false);
            } else if (annotation instanceof GET) {
                parseHttpMethodAndPath("GET", ((GET) annotation).value(), false);
            } else if (annotation instanceof HEAD) {
//                parseHttpMethodAndPath("HEAD", ((HEAD) annotation).value(), false);
//                if (!Void.class.equals(responseType)) {
//                    throw methodError("HEAD method must use Void as response type.");
//                }
            } else if (annotation instanceof PATCH) {
                parseHttpMethodAndPath("PATCH", ((PATCH) annotation).value(), true);
            } else if (annotation instanceof POST) {
                parseHttpMethodAndPath("POST", ((POST) annotation).value(), true);
            } else if (annotation instanceof PUT) {
                parseHttpMethodAndPath("PUT", ((PUT) annotation).value(), true);
            } else if (annotation instanceof OPTIONS) {
                parseHttpMethodAndPath("OPTIONS", ((OPTIONS) annotation).value(), false);
            } else if (annotation instanceof HTTP) {
                HTTP http = (HTTP) annotation;
                parseHttpMethodAndPath(http.method(), http.path(), http.hasBody());
            }
            else if (annotation instanceof retrofit2.http.Headers) {
                String[] headersToParse = ((retrofit2.http.Headers) annotation).value();
                if (headersToParse.length == 0) {
                    throw methodError("@Headers annotation is empty.");
                }
                headers = parseHeaders(headersToParse);
            } else if (annotation instanceof Multipart) {
                if (isFormEncoded) {
                    throw methodError("Only one encoding annotation is allowed.");
                }
                isMultipart = true;
            } else if (annotation instanceof FormUrlEncoded) {
                if (isMultipart) {
                    throw methodError("Only one encoding annotation is allowed.");
                }
                isFormEncoded = true;
            }
        }
        //解析http请求， http请求， 参数（url）， 是否有body内容
        private void parseHttpMethodAndPath(String httpMethod, String value, boolean hasBody) {
            //一次请求只会有一次http的请求
            if (this.httpMethod != null) {
                throw methodError("Only one HTTP method is allowed. Found: %s and %s.",
                        this.httpMethod, httpMethod);
            }
            this.httpMethod = httpMethod;
            this.hasBody = hasBody;

            if (value.isEmpty()) {
                return;
            }

            // Get the relative URL path and existing query string, if present.
            //获取固定的参数，如果固定的参数还带有{}动态参数，则异常
            int question = value.indexOf('?');
            if (question != -1 && question < value.length() - 1) {
                // Ensure the query string does not have any named parameters.
                String queryParams = value.substring(question + 1);
                Matcher queryParamMatcher = PARAM_URL_REGEX.matcher(queryParams);
                if (queryParamMatcher.find()) {
                    throw methodError("URL query string \"%s\" must not have replace block. "
                            + "For dynamic query parameters use @Query.", queryParams);
                }
            }

            this.relativeUrl = value;
            this.relativeUrlParamNames = parsePathParameters(value);
        }

        //解析动态参数参数
        static Set<String> parsePathParameters(String path) {
            Matcher m = PARAM_URL_REGEX.matcher(path);
            Set<String> patterns = new LinkedHashSet<>();
            while (m.find()) {
                patterns.add(m.group(1));
            }
            return patterns;
        }

        //TODO 解析头注解，具体处理还未清楚
        private Headers parseHeaders(String[] headers) {
            Headers.Builder builder = new Headers.Builder();
            for (String header : headers) {
                int colon = header.indexOf(':');
                if (colon == -1 || colon == 0 || colon == header.length() - 1) {
                    throw methodError(
                            "@Headers value must be in the form \"Name: Value\". Found: \"%s\"", header);
                }
                String headerName = header.substring(0, colon);
                String headerValue = header.substring(colon + 1).trim();
                if ("Content-Type".equalsIgnoreCase(headerName)) {
                    MediaType type = MediaType.parse(headerValue);
                    if (type == null) {
                        throw methodError("Malformed content type: %s", headerValue);
                    }
                    contentType = type;
                } else {
                    builder.add(headerName, headerValue);
                }
            }
            return builder.build();
        }
        //创建一个 设配器  用于 适配 当前请求所需要的返回类型
        private MCallAdapter<T,R> createCallAdapter(){
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
                //TODO 这里源码中是T,R 但是这里用T R 会报错
                return (MCallAdapter<T, R>) mRetrofit.callAdapter(returnType, annotations);
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
