package app.unlock45.tv.domain

/** What the FRITZ!Box parental control reports for this device (spec, "Einlöse-Orchestrator (`Redemption`)"). */
sealed interface LockState {
    /** Locked: a ticket can be redeemed, after [blockedSec] seconds of waiting following a failed attempt. */
    data class Locked(val blockedSec: Int) : LockState

    /**
     * A ticket is still running for [remainingSec] seconds. Another one can be added, after [blockedSec] seconds of
     * waiting following a failed attempt.
     */
    data class TicketActive(val remainingSec: Int, val blockedSec: Int = 0) : LockState {
        val remainingMinutes get() = minutesRoundedUp(remainingSec)
    }

    /** Allowed time period with online budget left, or the box shows its login page: there is nothing to unlock. */
    data object NotLocked : LockState

    /** The device has no access restriction at all, so tickets do not apply. */
    data object NotRestricted : LockState

    /** Complete device lock or a lock for abuse: tickets are not possible. */
    data object DeviceBlocked : LockState

    /** No connection to the FRITZ!Box. */
    data object Unreachable : LockState

    /** The answer holds no readable parental-control data, e.g. after a firmware change. */
    data object Unrecognized : LockState
}

/** The length of one ticket. */
const val TICKET_MINUTES = 45
const val TICKET_SECONDS = TICKET_MINUTES * 60

private const val SECONDS_PER_MINUTE = 60

/** Whole minutes, a started minute counting as one, like on the FRITZ!Box. */
fun minutesRoundedUp(sec: Int) = (sec + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
