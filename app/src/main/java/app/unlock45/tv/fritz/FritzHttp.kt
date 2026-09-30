package app.unlock45.tv.fritz

import app.unlock45.tv.security.HostAllowlist
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Minimal HTTP client for the FRITZ!Box. Every request is checked against [HostAllowlist], and redirects are never
 * followed: a redirect is returned like any other non-200 answer (spec, "Sicherheit und Netzwerk").
 */
internal object FritzHttp {

    class Response(val status: Int, val body: String)

    suspend fun get(url: URI): Response = request(url) { }

    suspend fun postForm(url: URI, form: String): Response = request(url) {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val bytes = form.toByteArray(Charsets.UTF_8)
        setFixedLengthStreamingMode(bytes.size)
        outputStream.use { it.write(bytes) }
    }

    /**
     * Runs the blocking exchange on an IO thread. Cancelling the caller (the orchestrator's time limits) disconnects
     * at once, which aborts a pending connect or read instead of waiting for the socket timeouts.
     */
    private suspend fun request(url: URI, send: HttpURLConnection.() -> Unit): Response {
        if (!HostAllowlist.permits(url)) throw IOException("Host not allowed")
        val connection = url.toURL().openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { connection.disconnect() }
            Dispatchers.IO.asExecutor().execute {
                val response = runCatching { connection.exchange(send) }
                connection.disconnect()
                continuation.resumeWith(response)
            }
        }
    }

    private fun HttpURLConnection.exchange(send: HttpURLConnection.() -> Unit): Response {
        send()
        val status = responseCode
        val body = (if (status < 400) inputStream else errorStream)
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            .orEmpty()
        return Response(status, body)
    }

    // Backstops only: the orchestrator's limits end a request first (spec, "Zeitregeln").
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 25_000
}
