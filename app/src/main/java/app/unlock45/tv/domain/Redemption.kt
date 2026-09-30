package app.unlock45.tv.domain

import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Port to the FRITZ!Box parental control (spec, "Einlöse-Orchestrator (`Redemption`)"). */
interface FritzGateway {
    /** Reads the current lock state; connection failures are [LockState.Unreachable]. */
    suspend fun prepare(): LockState

    /**
     * Redeems the ticket: [RedemptionResult.Success], [RedemptionResult.InvalidOrUsedCode] or
     * [RedemptionResult.Unrecognized]; connection failures are [RedemptionResult.RouterUnreachable] or
     * [RedemptionResult.Timeout].
     */
    suspend fun submit(code: TicketCode): RedemptionResult
}

/** Port that passes status changes on to notification, banner and result store. */
fun interface StatusSink {
    fun report(status: RedemptionStatus)
}

sealed interface RedemptionStatus {
    data object Redeeming : RedemptionStatus
    data class Finished(val result: RedemptionResult) : RedemptionStatus
}

class Redemption(
    private val gateway: FritzGateway,
    private val clock: TimeSource,
) {
    /** The lock state read ahead of the code, or null once a redemption took it. */
    private val preparation = AtomicReference<Deferred<Reading?>?>(null)

    /**
     * Starts reading the lock state as soon as the entry screen opens, so the code can go out right after Enter.
     * [scope] must outlive the screen, because the reading goes on after the code is handed over. Replaces an
     * earlier reading that no redemption took.
     *
     * Returns the state once it is read, for the ticket timers; null if the box did not answer in time. No further
     * request is made for them (spec, "Ticket-Timer").
     */
    fun prepare(scope: CoroutineScope): Deferred<LockState?> {
        val reading = scope.async { read() }
        preparation.getAndSet(reading)?.cancel()
        return scope.async { reading.await()?.state }
    }

    /**
     * Runs one redemption and reports [RedemptionStatus.Finished] as soon as the box answers. The code is discarded
     * in every end state, including cancellation.
     */
    suspend fun redeem(code: TicketCode, status: StatusSink) {
        status.report(RedemptionStatus.Redeeming)
        val deadline = clock.markNow() + OVERALL_LIMIT
        val result = try {
            withTimeoutOrNull(OVERALL_LIMIT) {
                val state = preparation.getAndSet(null)?.await()?.current()
                    ?: read()?.state
                    ?: return@withTimeoutOrNull RedemptionResult.Timeout
                redeemPrepared(state, code, deadline)
            } ?: RedemptionResult.Timeout
        } finally {
            code.discard()
        }
        status.report(RedemptionStatus.Finished(result))
    }

    /** Reads the lock state; null if the box does not answer in time. */
    private suspend fun read(): Reading? = answerWithinLimit { Reading(gateway.prepare(), clock.markNow()) }

    /** A lock state and when it was read. */
    private class Reading(val state: LockState, val at: TimeMark) {
        /**
         * The state as it is now, with the time passed since the reading taken off the waiting and the ticket time;
         * null if the reading is too old to go by, did not reach the box, or the ticket has run out since.
         */
        fun current(): LockState? {
            val age = at.elapsedNow()
            if (age > MAX_AGE) return null
            // Whole seconds only, so the waiting time is never cut short.
            val passedSec = age.inWholeSeconds.toInt()
            return when (state) {
                is LockState.Locked -> LockState.Locked(blockedSec = (state.blockedSec - passedSec).coerceAtLeast(0))
                is LockState.TicketActive -> LockState.TicketActive(
                    remainingSec = state.remainingSec - passedSec,
                    blockedSec = (state.blockedSec - passedSec).coerceAtLeast(0),
                ).takeIf { it.remainingSec > 0 }
                LockState.Unreachable -> null
                LockState.NotLocked, LockState.NotRestricted, LockState.DeviceBlocked, LockState.Unrecognized -> state
            }
        }
    }

    /**
     * A locked box gets the code, and so does one with a running ticket: the box adds the 45 minutes to it (spec,
     * "Ticket vormerken", measured 41 + 45 = 86 min). In every other state the code would be wasted or rejected.
     */
    private suspend fun redeemPrepared(state: LockState, code: TicketCode, deadline: TimeMark): RedemptionResult = when (state) {
        is LockState.Locked -> submit(code, state.blockedSec, deadline)
        is LockState.TicketActive -> submit(code, state.blockedSec, deadline)
        LockState.NotLocked -> RedemptionResult.NoLockPageActive
        LockState.NotRestricted -> RedemptionResult.NotRestricted
        LockState.DeviceBlocked -> RedemptionResult.DeviceBlocked
        LockState.Unreachable -> RedemptionResult.RouterUnreachable
        LockState.Unrecognized -> RedemptionResult.Unrecognized
    }

    private suspend fun submit(code: TicketCode, blockedSec: Int, deadline: TimeMark): RedemptionResult {
        // After a failed attempt the box rejects tickets for a few seconds. Wait, unless the box would then have
        // too little time left to answer: a ticket sent at the last moment is likely used up, yet reported as Timeout.
        val wait = blockedSec.seconds
        if ((deadline - wait - ANSWER_RESERVE).hasPassedNow()) return RedemptionResult.Timeout
        delay(wait)
        return try {
            answerWithinLimit { gateway.submit(code) } ?: RedemptionResult.Timeout
        } finally {
            // A reading taken meanwhile may predate the box counting this attempt, and so miss its waiting time.
            preparation.getAndSet(null)?.cancel()
        }
    }

    private suspend fun <T> answerWithinLimit(request: suspend () -> T): T? = withTimeoutOrNull(RESPONSE_LIMIT) { request() }

    private companion object {
        /** Per request to the box; measured answers take about 2 s (spec, "Zeitregeln"). */
        val RESPONSE_LIMIT = 20.seconds

        /** For the whole redemption, waiting time included. */
        val OVERALL_LIMIT = 25.seconds

        /** Time the box must have left to answer the POST; it answers within about 2 s. */
        val ANSWER_RESERVE = 5.seconds

        /** How long a state read ahead of the code is trusted; entering six digits takes a few seconds. */
        val MAX_AGE = 30.seconds
    }
}
