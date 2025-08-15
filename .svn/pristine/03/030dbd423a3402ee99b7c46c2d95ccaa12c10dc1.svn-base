package com.anssy.znewspro.utils.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer

import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class CustomLoggingInterceptor : Interceptor {

    companion object {
        private const val TAG = "CustomLoggingInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 打印请求日志
        logRequest(request, request.bodyToString())
        val startNs = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            throw e
        }
        val tookMs = (System.nanoTime() - startNs) / 1_000_000
        val responseBodyString = response.bodyToString()
        // 打印响应日志
        logResponse(
            request, response, responseBodyString, request.bodyToString(), tookMs
        )
        // 重新构建响应以确保内容可再次读取
        return response.newBuilder()
            .body(ResponseBody.create(response.body?.contentType(), responseBodyString ?: ""))
            .build()
    }

    private fun logRequest(request: Request, requestBody: String?) {
        Log.d(
            TAG,
            "发送请求：" + "\nURL: ${request.url}" + "\nMethod: ${request.method}" + "\nHeaders: ${request.headers}" + // 打印请求头
                    (requestBody?.let { "\nBody: $it" } ?: ""))
    }

    private fun logResponse(
        request: Request,
        response: Response,
        responseBody: String?,
        requestBody: String?,
        tookMs: Long
    ) {
        Log.d(
            TAG,
            "收到响应：" + "\nURL: ${response.request.url}" + "\nMethod: ${response.request.method}" + "\nResponse Code: ${response.code}" + "\nResponse Message: ${response.message}" + "\nResponse Headers: ${response.headers}" + "\nRequest Body: ${requestBody ?: "None"}" + "\nResponse Body: ${responseBody ?: "None"}" + "\nTime Taken: ${tookMs}ms"
        )
    }

    private fun Request.bodyToString(): String? {
        if (body == null || method == "GET" || method == "HEAD") return null

        return try {
            val buffer = Buffer()
            body?.writeTo(buffer)
            val charset: Charset =
                body?.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            buffer.readString(charset)
        } catch (e: IOException) {
            "无法读取请求体: ${e.message}"
        }
    }

    private fun Response.bodyToString(): String? {
        if (body == null || body!!.source().exhausted()) return null
        val source = body!!.source()
        source.request(Long.MAX_VALUE)
        val buffer = source.buffer
        val charset: Charset =
            body!!.contentType()?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
        if (!buffer.isPlaintext) return "响应体不是纯文本格式"
        return buffer.clone().readString(charset)
    }

    private val Buffer.isPlaintext: Boolean
        get() {
            return try {
                val prefix = Buffer()
                val byteCount = if (size < 64) size else 64
                copyTo(prefix, 0, byteCount)
                for (i in 0 until 16) {
                    if (prefix.exhausted()) break
                    val codePoint = prefix.readUtf8CodePoint()
                    if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                        return false
                    }
                }
                true
            } catch (e: EOFException) {
                false
            }
        }
}

object PrettyLogger {

    fun logBoxed(tag: String = "NetworkLog", message: String) {
        val border = "═".repeat(60)
        val boxedMsg = buildString {
            append("╔$border\n")
            message.lines().forEach {
                append("║ $it\n")
            }
            append("╚$border")
        }

    }
}

