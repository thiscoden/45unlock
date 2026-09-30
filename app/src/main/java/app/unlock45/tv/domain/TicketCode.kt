package app.unlock45.tv.domain

/**
 * A six-digit FRITZ!Box ticket code on its way to the box. It lives only in memory and is discarded once the
 * redemption ends (spec, "Datenschutz und Sicherheit"); it never shows up in `toString`, so never in a log.
 */
class TicketCode(digits: String) {
    init {
        require(digits.length == CodeEntry.LENGTH && digits.all { it in '0'..'9' }) { "Not a ticket code" }
    }

    private var value: String? = digits

    /** The digits, for sending them to the FRITZ!Box. Fails once the code is discarded. */
    val digits: String
        get() = checkNotNull(value) { "Ticket code already discarded" }

    val isDiscarded: Boolean
        get() = value == null

    /** Drops the reference to the digits. Strings are immutable, so this is the most the JVM allows. */
    fun discard() {
        value = null
    }

    override fun toString() = "TicketCode(<redacted>)"
}
