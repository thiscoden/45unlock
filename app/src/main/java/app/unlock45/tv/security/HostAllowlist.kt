package app.unlock45.tv.security

import java.net.URI

/**
 * The only hosts the app may talk to (spec, "Sicherheit und Netzwerk"): the FRITZ!Box by name, and as a fallback
 * its address in the home network where the app was built and the factory default. Must match `res/xml/network_security_config.xml`, which allows cleartext for exactly these hosts.
 */
object HostAllowlist {

    /** The name is tried first, then all [fallbacks] at once. */
    val hosts = listOf("fritz.box", "192.168.188.1", "192.168.178.1")

    val fallbacks = hosts.drop(1)

    fun permits(url: URI): Boolean =
        url.scheme == "http" && url.userInfo == null && url.port in setOf(-1, 80) && url.host?.lowercase() in hosts
}
