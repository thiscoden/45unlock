package app.unlock45.tv.domain

/**
 * Minutes per ticket timer, left to right (spec, "Design-System", Ticket-Timer). Tickets add up on the FRITZ!Box, so
 * [remainingSec] splits into tickets of [TICKET_MINUTES]: the running one first with its minutes left, then the ones
 * added to it, full. A started minute counts as one, like on the FRITZ!Box. No time left, no timer.
 */
fun ticketTimers(remainingSec: Int): List<Int> {
    if (remainingSec <= 0) return emptyList()
    val minutes = minutesRoundedUp(remainingSec)
    val count = (minutes + TICKET_MINUTES - 1) / TICKET_MINUTES
    return listOf(minutes - (count - 1) * TICKET_MINUTES) + List(count - 1) { TICKET_MINUTES }
}
