package com.jeka8833.hypixelrequester

import com.google.gson.annotations.SerializedName
import com.jeka8833.hypixelrequester.util.HypixelRateLimiter
import okhttp3.Request
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.annotations.Blocking
import java.util.*
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.function.Predicate


class HypixelPipeline(val key: UUID, val rateLimiter: HypixelRateLimiter) : Runnable {
    private val logger: Logger = LogManager.getLogger(HypixelPipeline::class.java)

    private val tasks: BlockingQueue<RequestInterface> = PriorityBlockingQueue()

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private val thread: ExecutorService = ThreadPoolExecutor(
        0, 1, 0, TimeUnit.MILLISECONDS, SynchronousQueue()
    )
    { r: Runnable? ->
        val t = Executors.defaultThreadFactory().newThread(r)
        t.priority = Thread.MIN_PRIORITY
        t.isDaemon = true
        t.name = "Hypixel Requester Thread"
        t
    }

    @Suppress("MemberVisibilityCanBePrivate")
    var retryTimes: Int = 3
        set(value) {
            if (value < 1) throw IllegalArgumentException("Value is <= 0")

            field = value
        }

    fun start(): Future<*> {
        return thread.submit(this::run)
    }

    @Blocking
    override fun run() {
        while (!Thread.interrupted()) {
            try {
                var task: RequestInterface? = tasks.take()

                for (i in 1..retryTimes) {
                    try {
                        if (task is DefaultHypixelRequest) {
                            if (!hypixelRequest(task)) continue
                        } else if (task is CustomRequest) {
                            if (!task.listener.test(rateLimiter)) continue
                        }

                        task = null
                        break
                    } catch (_: InterruptedException) {
                        return
                    } catch (e: Exception) {
                        logger.warn("Hypixel loop has an error, try count: $i", e)
                    }
                }

                task?.returnNothing()
            } catch (_: InterruptedException) {
                return
            } catch (e: Exception) {
                logger.warn("Hypixel loop has an error", e)
            }
        }
    }

    private fun hypixelRequest(task: DefaultHypixelRequest): Boolean {
        val requestHTTP: Request = Request.Builder()
            .url("https://api.hypixel.net/player?uuid=" + task.player)
            .header("API-Key", key.toString())
            .build()

        logger.debug("Hypixel API request: {}", requestHTTP)

        var responseStatus: HypixelRateLimiter.ServerResponse? = null
        try {
            Main.client.newCall(requestHTTP).execute().use { response ->
                responseStatus = HypixelRateLimiter.ServerResponse.create(
                    response.header("RateLimit-Reset"),
                    response.header("RateLimit-Limit"),
                    response.header("RateLimit-Remaining")
                )
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
        } finally {
            rateLimiter.receiveAndLock(responseStatus)
        }

        return false
    }

    fun addTNTClientTask(user: UUID, listener: Consumer<HypixelPlayerStorage?>) {
        addTask(user, Int.MIN_VALUE, listener)
    }

    fun addTask(user: UUID, priority: Int, listener: Consumer<HypixelPlayerStorage?>) {
        tasks.add(DefaultHypixelRequest(priority, listener, user))
    }

    fun addCustomTask(priority: Int, listener: Predicate<HypixelRateLimiter?>) {
        tasks.add(CustomRequest(priority, listener))
    }

    fun getQueue(): PriorityBlockingQueue<RequestInterface> {
        return tasks as PriorityBlockingQueue<RequestInterface>
    }

    private data class HypixelParser(@SerializedName("player") val player: HypixelPlayerStorage?)

    abstract class RequestInterface(open val priority: Int) : Comparable<RequestInterface> {
        override fun compareTo(other: RequestInterface): Int {
            return other.priority.compareTo(priority)
        }

        abstract fun returnNothing()
    }

    data class DefaultHypixelRequest(
        override val priority: Int,
        val listener: Consumer<HypixelPlayerStorage?>,
        val player: UUID
    ) : RequestInterface(priority) {
        override fun returnNothing() {
            listener.accept(null)
        }
    }

    data class CustomRequest(
        override val priority: Int,
        val listener: Predicate<HypixelRateLimiter?>,
    ) : RequestInterface(priority) {
        override fun returnNothing() {
            listener.test(null)
        }
    }
}
