package app.unlock45.tv.domain

/** How a redemption ended (spec, "Einlöse-Orchestrator (`Redemption`)", "Ergebnistypen und Meldungstexte"). */
sealed interface RedemptionResult {
    /** Whether the ticket went through: the device is online now, for 45 minutes or more. */
    val unlocked: Boolean get() = this == Success || this is TicketAdded

    data object Success : RedemptionResult

    /** Accepted while a ticket was running: the box added the 45 minutes, [totalMinutes] are left in all. */
    data class TicketAdded(val totalMinutes: Int) : RedemptionResult

    data object InvalidOrUsedCode : RedemptionResult
    data object NoLockPageActive : RedemptionResult
    data object NotRestricted : RedemptionResult
    data object DeviceBlocked : RedemptionResult
    data object RouterUnreachable : RedemptionResult

    /** No answer within the response limit, or the whole redemption took too long. */
    data object Timeout : RedemptionResult

    /** Page or answer not readable; the FRITZ!Box adapter probably needs adapting to new firmware. */
    data object Unrecognized : RedemptionResult
}
