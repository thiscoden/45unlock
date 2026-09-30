package app.unlock45.tv.fritz

import app.unlock45.tv.domain.FritzGateway
import app.unlock45.tv.domain.LockState
import app.unlock45.tv.domain.RedemptionResult
import app.unlock45.tv.domain.TicketCode
import app.unlock45.tv.security.HostAllowlist
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException

/**
 * The parental-control API of the FRITZ!Box (spec, "Kindersicherungs-API"): `GET /` reads the lock state,
 * `POST /surf.lua` redeems the ticket. The box identifies the device itself; there is no account, session or cookie.
 */
class HttpFritzGateway : FritzGateway {

    /** The host that answered [prepare], so the ticket goes to the same box. */
    @Volatile
    private var host = HostAllowlist.hosts.first()

    override suspend fun prepare(): LockState {
        val byName = HostAllowlist.hosts.first()
        val (answering, response) = try {
            byName to FritzHttp.get(url(byName, "/"))
        } catch (e: IOException) {
            // `fritz.box` does not resolve or cannot be reached: try the box's addresses instead.
            if (e !is UnknownHostException && e !is ConnectException && e !is NoRouteToHostException) {
                return LockState.Unreachable
            }
            firstAnswer(HostAllowlist.fallbacks) { FritzHttp.get(url(it, "/")) } ?: return LockState.Unreachable
        }
        host = answering
        return response.parsedOr(LockState.Unrecognized, SurfPageParser::lockState)
    }

    override suspend fun submit(code: TicketCode): RedemptionResult {
        val response = try {
            FritzHttp.postForm(url(host, "/surf.lua"), "apply=apply&ticket=${code.digits}")
        } catch (_: SocketTimeoutException) {
            return RedemptionResult.Timeout
        } catch (_: IOException) {
            return RedemptionResult.RouterUnreachable
        }
        return response.parsedOr(RedemptionResult.Unrecognized, SurfPageParser::submitResult)
    }

    private fun url(host: String, path: String) = URI("http", host, path, null)

    /** Only a plain 200 carries the parental-control data; anything else, redirects included, is not readable. */
    private fun <T> FritzHttp.Response.parsedOr(unreadable: T, parse: (String) -> T): T =
        if (status == HttpURLConnection.HTTP_OK) parse(body) else unreadable
}
