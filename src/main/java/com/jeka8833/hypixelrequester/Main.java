package com.jeka8833.hypixelrequester;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "HypixelRequester", version = "HypixelRequester 2.0.0", mixinStandardHelpOptions = true,
        description = "A proxy client for requesting player data.")
public class Main implements Callable<Integer> {
    @CommandLine.Option(names = {"-u", "--user"}, description = "TNTClient user name", required = true)
    public UUID botName;

    @CommandLine.Option(names = {"-p", "--password"}, description = "TNTClient user password", required = true)
    public UUID botPassword;

    @CommandLine.Option(names = {"-k", "--key"}, description = "Hypixel API key", required = true)
    public UUID hypixelKey;

    @CommandLine.Option(names = "--availableInterval", description = "AvailableCount Packet interval (Default: PT5S)")
    public Duration sendAvailableCountInterval = Duration.ofSeconds(5);

    @CommandLine.Option(names = "--reconnectDelay",
            description = "Reconnect the TNTClient server after delay (Default: PT30S)")
    public Duration reconnectDelay = Duration.ofSeconds(30);

    @CommandLine.Option(names = {"-r", "--hypixelRetryCount"},
            description = "Number of repetitions after Hypixel request failure (Default: 3)")
    public int requestRetry = 3;

    @CommandLine.Option(names = "--hypixelFailDelay", description = "Sleep time after Hypixel API failure (Default: PT10S)")
    public Duration failSleep = Duration.ofSeconds(10);

    @CommandLine.Option(names = "--hypixelBetweenDelay", description = "Delay between Hypixel Requests (Default: PT0.1S)")
    public Duration delayBetweenCalls = Duration.ofMillis(100);

    @CommandLine.Option(names = "--hypixelThreads",
            description = "Number of threads that can simultaneously access HypixelAPI. (Default: 2)")
    public int threads = 2;

    @CommandLine.Option(names = "--hypixelRequestLimit",
            description = "Maximum number of Hypixel requests per 5 minutes. (Default: 300)")
    public int maxRequests = 300;

    @CommandLine.Option(names = "--tntServerIP",
            description = "IP address of the TNTClient server. (Default: wss://tnthypixel.jeka8833.pp.ua)")
    public String tntServerIP = "wss://tnthypixel.jeka8833.pp.ua";

    @Override
    public Integer call() throws Exception {
        RequesterManager requesterManager = new RequesterManager(this);
        requesterManager.start();

        Thread.currentThread().join();
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
