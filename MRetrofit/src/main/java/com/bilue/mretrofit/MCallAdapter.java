package com.bilue.mretrofit;

/**
 * Created by Bilue on 2017/3/19.
 */

//根据serviceMethod只是生成okhttpCall，所以还需要适配成RxJava，Android，Java等 的Observable，Call之类的
public interface MCallAdapter<R,T> {

    public T adapt(MCall<R> call);
}
