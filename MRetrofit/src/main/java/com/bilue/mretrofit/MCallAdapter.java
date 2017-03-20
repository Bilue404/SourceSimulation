package com.bilue.mretrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


/**
 * Created by Bilue on 2017/3/19.
 */

//根据serviceMethod只是生成okhttpCall，所以还需要适配成RxJava，Android，Java等 的Observable，Call之类的
public interface MCallAdapter<R, T> {

    public T adapt(MCall<R> call);


    abstract class Factory{
        public abstract MCallAdapter<?,?> get(Type returnType, Annotation[] annotations, MRetrofit retrofit);


        protected static Class<?> getRawType(Type type) {
            return MUtils.getRawType(type);
        }
    }
}
