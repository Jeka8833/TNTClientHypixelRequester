package com.jeka8833.hypixelrequester

import com.jeka8833.hypixelrequester.ratelimiter.AsyncHypixelRateLimiter
import com.jeka8833.hypixelrequester.ratelimiter.ResetManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import picocli.CommandLine
import java.time.Duration
import java.util.*

@CommandLine.Command(
    name = "HypixelRequester",
    version = ["HypixelRequester 1.0.2.1"],
    mixinStandardHelpOptions = true,
    description = ["A proxy client for requesting player data."]
)
class CommandWorker : Runnable {
    private val logger: Logger = LogManager.getLogger(CommandWorker::class.java)

    @CommandLine.Option(names = ["-u", "--user"], description = ["TNTClient user name"], required = true)
    lateinit var user: UUID

    @CommandLine.Option(names = ["-p", "--password"], description = ["TNTClient user password"], required = true)
    lateinit var password: UUID

    @CommandLine.Option(names = ["-k", "--key"], description = ["Hypixel API key"], required = true)
    lateinit var key: UUID

    @CommandLine.Option(names = ["--freeDelay"], description = ["FreePacket dispatch after delay (Default: PT30S)"])
    var freePacketDelay: Duration = Duration.ofSeconds(30)

    @CommandLine.Option(
        names = ["--reconnectDelay"],
        description = ["Reconnect the TNTClient server after delay (Default: PT30S)"]
    )
    var reconnectDelay: Duration = Duration.ofSeconds(30)

    @CommandLine.Option(
        names = ["-r", "--retry"],
        description = ["Number of repetitions after failure (Default: 3)"]
    )
    var retry: Int = 3

    @CommandLine.Option(names = ["--rateRefresh"], description = ["Refresh time Hypixel API key (Default: PT5M)"])
    var refreshTime: Duration = Duration.ofMinutes(5)

    @CommandLine.Option(
        names = ["--rateAdditional"], description = ["Addition of time to RefreshTime when you don't " +
                "know how long the first operation will take. (Default: PT3S)"]
    )
    var refreshTimeAdditional: Duration = Duration.ofSeconds(3)

    @CommandLine.Option(
        names = ["--rateFail"],
        description = ["Sleep time after Hypixel API failure (Default: PT10S)"]
    )
    var sleepTime: Duration = Duration.ofSeconds(10)

    @CommandLine.Option(
        names = ["--rateGroup"],
        description = ["Creates query groups to smooth out waves of queries. (Default: 60)"]
    )
    var groupSize: Int = 60

    @CommandLine.Option(
        names = ["--rateDelay"],
        description = ["Delay between requests (Default: PT0.1S)"]
    )
    var delayTime: Duration = Duration.ofMillis(100)

    @CommandLine.Option(
        names = ["--rateNoDelayZone"],
        description = ["A zone in which all requests are executed without delay, it is necessary that the last " +
                "requests have time to be executed before the timer resets. (Default: PT2S)"]
    )
    var noDelayZone: Duration = Duration.ofSeconds(2)

    @CommandLine.Option(
        names = ["--rateThread"],
        description = ["Number of threads that can simultaneously access HypixelAPI. (Default: 1)"]
    )
    var threads: Int = 1

    override fun run() {
        try {
            val resetManager = ResetManager(refreshTime, refreshTimeAdditional)
            val rateLimiter = AsyncHypixelRateLimiter(resetManager, groupSize, delayTime, noDelayZone, sleepTime)
            val hypixelAPI = HypixelPipeline(key, rateLimiter)
            hypixelAPI.retryTimes = retry

            hypixelAPI.addTask(UUID.fromString("6bd6e833-a80a-430e-9029-4786368811f9"), 0) { storage ->
                if (storage == null) {
                    logger.error("Wrong Hypixel key or internet problems. The programme is not stopped, " +
                            "but you have to solve this problem.")
                } else {
                    logger.info("The Hypixel key is working and ready to use.")
                }
            }

            hypixelAPI.start(threads)

            TNTServer.freePacketDelay = freePacketDelay.toMillis()
            TNTServer.reconnectDelay = reconnectDelay.toMillis()
            val future = TNTServer.connect(user, password, hypixelAPI)
            future.get()
        } finally {
            ResetManager.shutdown()
        }
    }
}
