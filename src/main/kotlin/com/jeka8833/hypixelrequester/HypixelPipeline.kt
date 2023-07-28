package com.jeka8833.hypixelrequester

import com.jeka8833.hypixelrequester.ratelimiter.AsyncHypixelRateLimiter
import java.util.*
import java.util.concurrent.*
import java.util.function.Consumer
import java.util.function.Predicate


class HypixelPipeline(val key: UUID, val rateLimiter: AsyncHypixelRateLimiter) {
    internal val tasks: BlockingQueue<RequestInterface> = PriorityBlockingQueue()

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private val thread: ExecutorService = Executors.newCachedThreadPool { r: Runnable? ->
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

    fun start(threadsCount: Int): List<Future<*>> {
        val list = arrayListOf<Future<*>>()

        for (i in 1..threadsCount) {
            val taskThread = HypixelThread(this)
            val future = thread.submit(taskThread)
            taskThread.addCurrentFuture(future)

            list.add(future)
        }

        return list
    }

    fun addTNTClientTask(user: UUID, listener: Consumer<HypixelPlayerStorage?>) {
        addTask(user, Int.MIN_VALUE, listener)
    }

    fun addTask(user: UUID, priority: Int, listener: Consumer<HypixelPlayerStorage?>) {
        tasks.add(DefaultHypixelRequest(priority, listener, user))
    }

    fun addCustomTask(priority: Int, listener: Predicate<AsyncHypixelRateLimiter?>) {
        tasks.add(CustomRequest(priority, listener))
    }

    fun getQueue(): PriorityBlockingQueue<RequestInterface> {
        return tasks as PriorityBlockingQueue<RequestInterface>
    }

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
        val listener: Predicate<AsyncHypixelRateLimiter?>,
    ) : RequestInterface(priority) {
        override fun returnNothing() {
            listener.test(null)
        }
    }
}
