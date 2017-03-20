package com.bilue.mretrofit;

import android.os.Build;

import java.util.concurrent.Executor;



/**
 * Created by bilue on 17/3/20.
 */

//用于生成平台的Executor 因为是retrofit 是Android/Java的请求库。
public class Platform {
    private static final Platform PLATFORM = findPlatform();

    public static Platform get(){
        return PLATFORM;
    }

    private static Platform findPlatform(){
        try {
            Class.forName("android.os.Build");
            if (Build.VERSION.SDK_INT != 0) {
                return new Platform.Android();
            }
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("java.util.Optional");
            return new Platform.Java8();
        } catch (ClassNotFoundException ignored) {
        }
        return new Platform();
    }

    Executor getDefaultExcutor(){return null;};

    MCallAdapter.Factory defaultCallAdapterFactory(Executor callbackExecutor) {
        if (callbackExecutor != null) {
            return new MExecutorCallAdapterFactory(callbackExecutor);
        }
//        return DefaultCallAdapterFactory.INSTANCE;
        return null;

    }


    static class Java8 extends Platform{

    }

    static class Android extends Platform{

    }
}
