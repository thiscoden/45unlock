package app.unlock45.tv.fritz

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

/**
 * Asks every host at once and returns the first one that answers, with its answer, or null when all of them fail.
 * The others are cancelled. An address from another network does not fail fast but hangs until the connect timeout,
 * so asking one after the other would let a wrong address block the right one.
 */
internal suspend fun <T> firstAnswer(hosts: List<String>, fetch: suspend (String) -> T): Pair<String, T>? =
    coroutineScope {
        val winner = CompletableDeferred<Pair<String, T>?>()
        val requests = hosts.map { host ->
            launch {
                try {
                    winner.complete(host to fetch(host))
                } catch (_: IOException) {
                    // This host lost; another one may still answer.
                }
            }
        }
        launch {
            requests.joinAll()
            winner.complete(null)
        }
        winner.await().also { coroutineContext.cancelChildren() }
    }
