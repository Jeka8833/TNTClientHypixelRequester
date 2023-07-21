package com.jeka8833.hypixelrequester

import com.jeka8833.hypixelrequester.tntclient.TNTServerSocket
import com.jeka8833.hypixelrequester.tntclient.packet.packets.ReceiveHypixelPlayerPacket
import com.jeka8833.hypixelrequester.tntclient.packet.packets.ReceiveHypixelPlayerPacket.ReceivePlayer
import com.jeka8833.hypixelrequester.tntclient.packet.packets.UpdateFreeRequestsPacket
import okhttp3.WebSocket
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*
import java.util.concurrent.*

class TNTServer {

    companion object {
        private val logger: Logger = LogManager.getLogger(TNTServer::class.java)

        var freePacketDelay: Long = 30_000
        var reconnectDelay: Long = 30_000

        private var socket: WebSocket? = null
        private var requester: HypixelPipeline? = null
        private var nextSendFreePacket: Long = 0
        private var nextReconnect: Long = 0


        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        private val thread: ExecutorService = ThreadPoolExecutor(
            0, 1, 0, TimeUnit.MILLISECONDS, SynchronousQueue()
        )
        { r: Runnable? ->
            val t = Executors.defaultThreadFactory().newThread(r)
            t.priority = Thread.MIN_PRIORITY
            t.isDaemon = true
            t.name = "TNTClient Watchdog"
            t
        }

        fun connect(user: UUID, password: UUID, hypixel: HypixelPipeline): Future<Void> {
            return thread.submit(Callable {
                try {
                    requester = hypixel

                    while (!Thread.interrupted()) {
                        try {
                            if (socket == null ||
                                (!TNTServerSocket.isConnect() && System.currentTimeMillis() > nextReconnect)
                            ) {
                                nextReconnect = System.currentTimeMillis() + reconnectDelay

                                socket = TNTServerSocket.connect(user, password)
                            }

                            if (TNTServerSocket.isConnect() && socket != null && requester != null &&
                                System.currentTimeMillis() > nextSendFreePacket
                            ) {
                                useFreePacket()

                                val free = requester!!.rateLimiter.freeAtMoment.orElse(1)
                                logger.debug("Free at moment: {}", free)


                                TNTServerSocket.send(socket!!, UpdateFreeRequestsPacket(free))
                            }

                            Thread.sleep(500)
                        } catch (_: InterruptedException) {
                            return@Callable null
                        } catch (e: Exception) {
                            logger.warn("TNTClient Watchdog has an error", e)
                        }
                    }

                    null
                } finally {
                    socket?.close(1000, null)
                }
            })
        }

        fun useFreePacket() {
            nextSendFreePacket = System.currentTimeMillis() + freePacketDelay
        }

        fun forceSendFreePacket() {
            nextSendFreePacket = 0
        }

        fun request(player: UUID) {
            logger.debug("TNTClient request: {}", player)


            if (requester == null) return

            requester!!.addTNTClientTask(player) { storage ->
                if (storage == null || socket == null) return@addTNTClientTask

                TNTServerSocket.send(
                    socket!!,
                    ReceiveHypixelPlayerPacket(
                        Collections.singletonList(
                            ReceivePlayer(
                                player,
                                ReceiveHypixelPlayerPacket.RECEIVE_GOOD,
                                storage
                            )
                        )
                    )
                )
            }
        }
    }
}