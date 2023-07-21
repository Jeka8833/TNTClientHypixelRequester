package com.jeka8833.hypixelrequester

import com.google.gson.Gson
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import picocli.CommandLine
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class Main {
    companion object {
        private val logger: Logger = LogManager.getLogger(Main::class.java)

        val gson = Gson()
        val client = createSocket()

        private fun createSocket(): OkHttpClient {
            val newBuilder = OkHttpClient.Builder()
            try {
                val trustAllCerts = arrayOf<TrustManager>(
                    object : X509TrustManager {
                        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return arrayOf()
                        }
                    }
                )
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, SecureRandom())
                newBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                newBuilder.hostnameVerifier { _: String?, _: SSLSession? -> true }
            } catch (e: Exception) {
                logger.error("Fail create noSSL socket", e)
            }

            val dispatcher = Dispatcher()
            dispatcher.maxRequestsPerHost = 1
            newBuilder.dispatcher(dispatcher)
            newBuilder.connectionPool(ConnectionPool(1, 30, TimeUnit.SECONDS))
            return newBuilder.build()
        }
    }
}

fun main(args: Array<String>) {
    CommandLine(CommandWorker()).execute(*args)
}