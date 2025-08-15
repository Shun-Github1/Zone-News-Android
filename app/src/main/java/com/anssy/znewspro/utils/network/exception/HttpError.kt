package com.anssy.znewspro.utils.network.exception

data class HttpError(val httpCode:Int, val errorMsg:String?, val exception: Throwable?)