package com.anssy.znewspro.utils.network

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络Api
 * @author llw
 * @description NetworkApi
 */
object NetworkApi {
    /**
     * 获取APP运行状态及版本信息，用于日志打印
     */
    private var iNetworkRequiredInfo: INetworkRequiredInfo? = null

    /**
     * API访问地址
     */
    private const val BASE_URL = "https://m.anssy.com/gassa/api/"

    private var okHttpClient: OkHttpClient? = null
        /**
         * 配置OkHttp
         *
         * @return OkHttpClient
         */
        get() {
            //不为空则说明已经配置过了，直接返回即可。
            if (field == null) {
                //OkHttp构建器
                val builder = OkHttpClient.Builder()
                //设置缓存大小
                val cacheSize = 100 * 1024 * 1024
                //设置网络请求超时时长，这里设置为6s
                builder.connectTimeout(6, TimeUnit.SECONDS)
                //添加请求拦截器，如果接口有请求头的话，可以放在这个拦截器里面
                builder.addInterceptor(RequestInterceptor(iNetworkRequiredInfo))
                //添加返回拦截器，可用于查看接口的请求耗时，对于网络优化有帮助
                builder.addInterceptor(ResponseInterceptor())
                //当程序在debug过程中则打印数据日志，方便调试用。
                if (iNetworkRequiredInfo != null && iNetworkRequiredInfo!!.isDebug) {
                    //iNetworkRequiredInfo不为空且处于debug状态下则初始化日志拦截器
                    val httpLoggingInterceptor = HttpLoggingInterceptor()
                    //设置要打印日志的内容等级，BODY为主要内容，还有BASIC、HEADERS、NONE。
                    httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
                    //将拦截器添加到OkHttp构建器中
                    builder.addInterceptor(httpLoggingInterceptor)
                }
                //OkHttp配置完成
                field = builder.build()
            }
            return field
        }

    private val retrofitHashMap = HashMap<String, Retrofit?>()

    /**
     * 初始化
     */
    fun init(networkRequiredInfo: INetworkRequiredInfo?) {
        iNetworkRequiredInfo = networkRequiredInfo
    }

    /**
     * 创建serviceClass的实例
     */
    fun <T> createService(serviceClass: Class<T>): T {
        return getRetrofit(serviceClass)!!.create(serviceClass)
    }

    /**
     * 配置Retrofit
     *
     * @param serviceClass 服务类
     * @return Retrofit
     */
    private fun getRetrofit(serviceClass: Class<*>): Retrofit? {
        if (retrofitHashMap[BASE_URL + serviceClass.name] != null) {
            //刚才上面定义的Map中键是String，值是Retrofit，当键不为空时，必然有值，有值则直接返回。
            return retrofitHashMap[BASE_URL + serviceClass.name]
        }
        //初始化Retrofit  Retrofit是对OKHttp的封装，通常是对网络请求做处理，也可以处理返回数据。
        //Retrofit构建器
        val builder = Retrofit.Builder()
        //设置访问地址
        builder.baseUrl(BASE_URL)
        //设置OkHttp客户端，传入上面写好的方法即可获得配置后的OkHttp客户端。
        builder.client(okHttpClient)
        //设置数据解析器 会自动把请求返回的结果（json字符串）通过Gson转化工厂自动转化成与其结构相符的实体Bean
        builder.addConverterFactory(GsonConverterFactory.create())
        //设置请求回调，使用RxJava 对网络返回进行处理
        builder.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        //retrofit配置完成
        val retrofit = builder.build()
        //放入Map中
        retrofitHashMap[BASE_URL + serviceClass.name] = retrofit
        //最后返回即可
        return retrofit
    }

    /**
     * 配置RxJava 完成线程的切换
     *
     * @param observer 这个observer要注意不要使用lifecycle中的Observer
     * @param
     * @return Observable
     */
    fun <T> applySchedulers(observer: Observer<T>): ObservableTransformer<T, T> {
        return ObservableTransformer { upstream: Observable<T> ->
            val observable = upstream
                .subscribeOn(Schedulers.io()) //线程订阅
                .observeOn(AndroidSchedulers.mainThread()) //观察Android主线程
                .map(getAppErrorHandler()) //判断有没有500的错误，有则进入getAppErrorHandler
                .onErrorResumeNext(HttpErrorHandler()) //判断有没有400的错误
            //订阅观察者
            observable.subscribe(observer)
            observable
        }
    }

    /**
     * 错误码处理
     */
    internal fun <T> getAppErrorHandler(): Function<T, T> {
        return Function { response: T ->
            //当response返回出现500之类的错误时
            if (response is BaseResponse && (response as BaseResponse).responseCode >= 500) {
                //通过这个异常处理，得到用户可以知道的原因
                val exception = ExceptionHandle.ServerException()
                exception.code = (response as BaseResponse).responseCode
                exception.message =
                    if ((response as BaseResponse).responseError != null) (response as BaseResponse).responseError else ""
                throw exception
            }
            response
        }
    }
}