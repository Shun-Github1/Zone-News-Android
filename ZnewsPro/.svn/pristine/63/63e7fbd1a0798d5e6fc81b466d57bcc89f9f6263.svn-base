package com.anssy.znewspro.utils.network

import io.reactivex.Observer
import io.reactivex.disposables.Disposable

/**
 * 自定义Observer
 *
 * @author llw
 */
abstract class BaseObserver<T> : Observer<T> {
    //开始
    override fun onSubscribe(disposable: Disposable) {
    }

    override fun onNext(t: T & Any) {
        onSuccess(t)
    }
    //异常
    override fun onError(e: Throwable) {
        onFailure(e)
    }

    //完成
    override fun onComplete() {
    }

    //成功
    abstract fun onSuccess(t: T)

    //失败
    abstract fun onFailure(e: Throwable?)
}