package com.bilue.mretrofit;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;


/**
 * Created by bilue on 17/3/20.
 */

//用于生成平台的Executor 因为是retrofit 是Android/Java的请求库。
public class MPlatform {
    private static final MPlatform PLATFORM = findPlatform();

    public static MPlatform get(){
        return PLATFORM;
    }

    private static MPlatform findPlatform(){
        try {
            Class.forName("android.os.Build");
            if (Build.VERSION.SDK_INT != 0) {
                return new MPlatform.Android();
            }
        } catch (ClassNotFoundException ignored) {
        }
        try {
            Class.forName("java.util.Optional");
            return new MPlatform.Java8();
        } catch (ClassNotFoundException ignored) {
        }
        return new MPlatform();
    }

    Executor getDefaultExcutor(){return null;};

    MCallAdapter.Factory defaultCallAdapterFactory(Executor callbackExecutor) {
        if (callbackExecutor != null) {
            return new MExecutorCallAdapterFactory(callbackExecutor);
        }
//        return DefaultCallAdapterFactory.INSTANCE;
        return null;

    }

    Executor defaultCallbackExecutor() {
        return null;
    }


    static class Java8 extends MPlatform {

    }

    static class Android extends MPlatform {
        @Override
        Executor defaultCallbackExecutor() {
            return new MainExecutor();
        }


        class MainExecutor implements Executor{
            private final Handler handler = new Handler(Looper.getMainLooper());
            @Override
            public void execute(Runnable command) {
                handler.post(command);
            }
        }
    }
}
