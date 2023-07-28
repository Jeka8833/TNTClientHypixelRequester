
# TNTClient HypixelRequester

Permits TNTServer to use your Hypixel API Key for server needs.


## Run Locally

Download the latest version

```url
  https://github.com/Jeka8833/TNTClientHypixelRequester/releases
```

You run it with the parameters

```bash
  java -jar TNTClientHypixelRequester.jar -u <TNTClient User> -p <TNTClient Password> -k <Hypixel API Key>
```

You can see additional settings

```bash
  java -jar TNTClientHypixelRequester.jar --help
```
## Usage/Examples

I don't want to request anything from the Hypixel API and just want to run the code

```kotlin
val user: UUID = <TNTClient User>
val password: UUID = <TNTClient Password>
val key: UUID = <Hypixel API Key>

val resetManager = ResetManager(Duration.ofMinutes(5), Duration.ofSeconds(3))
val rateLimiter = AsyncHypixelRateLimiter(resetManager, 60, Duration.ofMillis(100),
    Duration.ofSeconds(2), Duration.ofSeconds(10))

val hypixelAPI = HypixelPipeline(key, rateLimiter)
val hypixelApiFutureList = hypixelAPI.start(2) // 2 threads

val tntServerFuture = TNTServer.connect(user, password, hypixelAPI)
tntServerFuture.get()  //This line will block the current thread because the API is asynchronous
```

I want to get information about a user

```kotlin
...

val player = UUID.fromString("6bd6e833-a80a-430e-9029-4786368811f9")

// The request with the highest number will be executed first.
val priority = 0

val hypixelAPI = HypixelPipeline(key, rateLimiter)
val hypixelApiFutureList = hypixelAPI.start(2) // 2 threads

hypixelAPI.addTask(player, priority) { response ->
  if(response == null) throw NullPointerException("Error while executing a request")

  println(response.stats.TNTGames.wins_tntrun) // Your processing of information
}

...
```

I want to make my own request to the Hypixel API

```kotlin
...

// The request with the highest number will be executed first.
val priority = 0

val hypixelAPI = HypixelPipeline(key, rateLimiter)

hypixelAPI.retryTimes = 2 // Number of repetitions after failure

val hypixelApiFutureList = hypixelAPI.start(2) // 2 threads

hypixelAPI.addCustomTask(priority) { rateLimitter ->
    HypixelResponse(rateLimitter).use { responseLimiter ->
        ...

        responseLimiter.setHeaders(response.code,
            response.header("RateLimit-Reset"),
            response.header("RateLimit-Limit"),
            response.header("RateLimit-Remaining"))

        ...

        return@addCustomTask true
    }
    return@addCustomTask false  // The operation failed and will be retried.
}

...
```

