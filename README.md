
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

val rateLimiter = HypixelRateLimiter(Duration.ofMinutes(5), 60, Duration.ofSeconds(30))

val hypixelAPI = HypixelPipeline(key, rateLimiter)
val hypixelApiFuture = hypixelAPI.start()

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
val hypixelApiFuture = hypixelAPI.start()

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

val hypixelApiFuture = hypixelAPI.start()

hypixelAPI.addCustomTask(priority) { rateLimitter ->
    var responseStatus: HypixelRateLimiter.ServerResponse? = null
    try {
        ...

        responseStatus = HypixelRateLimiter.ServerResponse.create(
            response.header("RateLimit-Reset"),
            response.header("RateLimit-Limit"),
            response.header("RateLimit-Remaining")
        )

        ...

        return@addCustomTask true
    } finally {
        rateLimiter.receiveAndLock(responseStatus)
    }
    return@addCustomTask false  // The operation failed and will be retried.
}

...
```

