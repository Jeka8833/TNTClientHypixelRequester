package com.jeka8833.hypixelrequester

import com.jeka8833.hypixelrequester.util.HypixelRateLimiter
import picocli.CommandLine
import java.time.Duration
import java.util.*

@CommandLine.Command(
    name = "HypixelRequester",
    version = ["HypixelRequester 1.0.1"],
    mixinStandardHelpOptions = true,
    description = ["A proxy client for requesting player data."]
)
class CommandWorker : Runnable {
    @CommandLine.Option(names = ["-u", "--user"], description = ["TNTClient user name"], required = true)
    lateinit var user: UUID

    @CommandLine.Option(names = ["-p", "--password"], description = ["TNTClient user password"], required = true)
    lateinit var password: UUID

    @CommandLine.Option(names = ["-k", "--key"], description = ["Hypixel API key"], required = true)
    lateinit var key: UUID

    @CommandLine.Option(names = ["-dr", "--refresh"], description = ["Refresh time Hypixel API key (Default: PT5M)"])
    var refreshTime: Duration = Duration.ofMinutes(5)

    @CommandLine.Option(
        names = ["-df", "--sleep"],
        description = ["Sleep time after Hypixel API failure (Default: PT10S)"]
    )
    var sleepTime: Duration = Duration.ofSeconds(10)

    @CommandLine.Option(names = ["--freeDelay"], description = ["FreePacket dispatch after delay (Default: PT30S)"])
    var freePacketDelay: Duration = Duration.ofSeconds(30)

    @CommandLine.Option(
        names = ["--reconnectDelay"],
        description = ["Reconnect the TNTClient server after delay (Default: PT30S)"]
    )
    var reconnectDelay: Duration = Duration.ofSeconds(30)

    @CommandLine.Option(
        names = ["-g", "--group"],
        description = ["Creates query groups to smooth out waves of queries. (Default: 60)"]
    )
    var groupSize: Int = 60

    @CommandLine.Option(
        names = ["-r", "--retry"],
        description = ["Number of repetitions after failure (Default: 3)"]
    )
    var retry: Int = 3

    override fun run() {
        val rateLimiter = HypixelRateLimiter(refreshTime, groupSize, sleepTime)
        val hypixelAPI = HypixelPipeline(key, rateLimiter)
        hypixelAPI.retryTimes = retry

        hypixelAPI.start()

        TNTServer.freePacketDelay = freePacketDelay.toMillis()
        TNTServer.reconnectDelay = reconnectDelay.toMillis()
        val future = TNTServer.connect(user, password, hypixelAPI)
        future.get()
    }
}
