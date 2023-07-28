package com.jeka8833.hypixelrequester

import com.google.gson.annotations.SerializedName
import com.jeka8833.hypixelrequester.ratelimiter.HypixelResponse
import okhttp3.Request
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.concurrent.Future

class HypixelThread(private val pipeline: HypixelPipeline) : Runnable {

    private val logger: Logger = LogManager.getLogger(HypixelThread::class.java)

    private var currentFuture: Future<*>? = null

    internal fun addCurrentFuture(future: Future<*>) {
        currentFuture = future
    }

    override fun run() {
        while (!currentFuture!!.isCancelled) {
            try {
                var task: HypixelPipeline.RequestInterface? = pipeline.tasks.take()

                for (i in 1..pipeline.retryTimes) {
                    try {
                        if (task is HypixelPipeline.DefaultHypixelRequest) {
                            if (!hypixelRequest(task)) continue
                        } else if (task is HypixelPipeline.CustomRequest) {
                            if (!task.listener.test(pipeline.rateLimiter)) continue
                        }

                        task = null
                        break
                    } catch (_: InterruptedException) {
                        // Force stop after fatal crash
                    } catch (e: Exception) {
                        logger.warn("Hypixel loop has an error, try count: $i", e)
                    }
                }

                task?.returnNothing()
            } catch (_: InterruptedException) {
                // Force stop after fatal crash
            } catch (e: Exception) {
                logger.warn("Hypixel loop has an error", e)
            }
        }
    }

    private fun hypixelRequest(task: HypixelPipeline.DefaultHypixelRequest): Boolean {
        val requestHTTP: Request = Request.Builder()
            .url("https://api.hypixel.net/player?uuid=" + task.player)
            .header("API-Key", pipeline.key.toString())
            .build()

        logger.debug("Hypixel API request: {}", requestHTTP)

        HypixelResponse(pipeline.rateLimiter).use { responseLimiter ->
            Main.client.newCall(requestHTTP).execute().use { response ->
                responseLimiter.setHeaders(response.code,
                    response.header("RateLimit-Reset"),
                    response.header("RateLimit-Limit"),
                    response.header("RateLimit-Remaining"))

                if (response.isSuccessful) {
                    response.body.use { body ->
                        body?.charStream().use { reader ->
                            val structure: HypixelParser? = Main.gson.fromJson(reader, HypixelParser::class.java)
                            if (structure?.player != null) {
                                task.listener.accept(structure.player)
                            }
                        }
                    }
                    return true
                } else if (response.code == 403) {
                    logger.warn("Hypixel API returned 403, check your key or the hypixel load balancer is throttling")
                }
            }
        }

        return false
    }

    private data class HypixelParser(@SerializedName("player") val player: HypixelPlayerStorage?)
}