package app.unlock45.tv.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import app.unlock45.tv.domain.CodeEntry
import app.unlock45.tv.domain.LockState
import app.unlock45.tv.domain.ResultKind
import app.unlock45.tv.domain.TicketCode
import app.unlock45.tv.notification.openLastResult
import app.unlock45.tv.service.RedemptionService
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Deliberately not saved in the instance state: the code must not be persisted anywhere.
    private var entry by mutableStateOf(CodeEntry.empty())
    private var nudgeSignal by mutableIntStateOf(0)
    private var rejectSignal by mutableIntStateOf(0)

    /**
     * When the running ticket's time runs out, on the [SystemClock.elapsedRealtime] clock, from the reading on
     * opening; null until the box answers or without a running ticket.
     */
    private var ticketEndsAt by mutableStateOf<Long?>(null)

    /** Waits for this opening's reading; cancelled on the next start, so an older reading never lands late. */
    private var awaitingReading: Job? = null

    /** The last redemption's result, shown once on opening; [statusSignal] counts each time one is taken. */
    private var status by mutableStateOf<ResultKind?>(null)
    private var statusSignal by mutableIntStateOf(0)

    /** The confirmed code while the confirm sequence plays, until it is handed to the service. */
    private var pending: TicketCode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CodeEntryScreen(entry, ticketEndsAt, status, statusSignal, nudgeSignal, rejectSignal, onHandover = ::handover, onClosed = ::finish) }
    }

    /** Reads the lock state while the code is being entered, so the redemption can start right after Enter. */
    override fun onStart() {
        super.onStart()
        // The timers only show what this opening read, never an older state.
        ticketEndsAt = null
        val reading = RedemptionService.prepare()
        awaitingReading?.cancel()
        awaitingReading = lifecycleScope.launch {
            val state = reading.await() as? LockState.TicketActive ?: return@launch
            ticketEndsAt = SystemClock.elapsedRealtime() + state.remainingSec.seconds.inWholeMilliseconds
        }
        showLastResult()
    }

    /**
     * Remote keys go straight to [CodeEntry], before Compose focus handling or the default Back behaviour.
     * Both the down and the up event of a mapped key are consumed, so Back only closes the app via [CodeEntry].
     *
     * RestrictedApi: androidx.core's ComponentActivity re-declares this with @RestrictTo, but it overrides the public
     * framework method Activity.dispatchKeyEvent.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val input = codeInputFor(event.keyCode) ?: return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN && (event.repeatCount == 0 || input.acceptsKeyRepeat())) {
            handle(input)
        }
        return true
    }

    private fun handle(input: CodeEntry.Input) {
        val transition = entry.on(input)
        entry = transition.entry
        when (transition.event) {
            CodeEntry.Event.Dismiss -> finish()
            CodeEntry.Event.Incomplete -> nudgeSignal++
            // The screen plays the confirm sequence and calls handover() halfway through.
            is CodeEntry.Event.Submit -> pending = TicketCode(transition.event.code)
            null -> Unit
        }
    }

    /**
     * Starts the service with the pending code. The service must start while this activity is still visible; only
     * after the handover does the screen close. Returns false if the system refused, then the entry starts over.
     */
    private fun handover(): Boolean {
        val code = pending ?: return startOver()
        pending = null
        try {
            RedemptionService.start(this, code)
        } catch (_: RuntimeException) {
            // The code is already discarded.
            return startOver()
        }
        return true
    }

    private fun startOver(): Boolean {
        entry = CodeEntry.empty()
        rejectSignal++
        return false
    }

    /**
     * Shows the last redemption's result, if there is a recent one, and starts the entry over at field 1 (spec,
     * "Status beim nächsten Öffnen"). Counted rather than compared, so the same text shows again.
     */
    private fun showLastResult() {
        val kind = openLastResult(this).take() ?: return
        status = kind
        statusSignal++
        if (!entry.isSubmitting) entry = CodeEntry.empty()
    }

    /**
     * Leaving before the handover (e.g. Home during the confirm sequence) drops the code: it is never redeemed
     * later from the background, and the entry starts empty on return.
     */
    override fun onStop() {
        pending?.let {
            it.discard()
            pending = null
            entry = CodeEntry.empty()
        }
        super.onStop()
    }
}
