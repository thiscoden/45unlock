package app.unlock45.tv.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import app.unlock45.tv.domain.LockState
import app.unlock45.tv.domain.Redemption
import app.unlock45.tv.domain.RedemptionResult
import app.unlock45.tv.domain.RedemptionStatus
import app.unlock45.tv.domain.TicketCode
import app.unlock45.tv.fritz.HttpFritzGateway
import app.unlock45.tv.notification.RedemptionNotifications
import app.unlock45.tv.notification.ResultBanner
import app.unlock45.tv.notification.openLastResult
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Short-lived foreground service (type short service) that runs one redemption per start, shows its result banner and
 * stops itself as soon as no redemption or banner is left (spec, "Hintergrunddienst und Benachrichtigung",
 * "Ergebnis-Banner").
 */
class RedemptionService : Service() {

    private val scope = MainScope()
    private var running = 0

    /**
     * Whether an error shows since the notification last said "redeeming". An error stays after the service ends;
     * a success alone is removed with it.
     */
    private var keepNotification = false
    private lateinit var notifications: RedemptionNotifications
    private lateinit var banner: ResultBanner
    private val lastResult by lazy { openLastResult(this) }

    override fun onCreate() {
        super.onCreate()
        notifications = RedemptionNotifications(this)
        banner = ResultBanner(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(RedemptionNotifications.ID, notifications.redeeming(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        keepNotification = false
        val code = handoff.getAndSet(null)
        if (code == null) {
            stopIfIdle()
            return START_NOT_STICKY
        }
        running++
        // Undispatched, so the finally block runs even if the scope is cancelled before the redemption starts.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                var result: RedemptionResult? = null
                redemption.redeem(code) { status ->
                    show(status)
                    if (status is RedemptionStatus.Finished) result = status.result
                }
                // Shown here rather than from the status sink: the service must outlast the banner. The 25 s limit
                // covers the redemption; the banner extends the service by its display time only.
                result?.let { banner.show(it) }
            } finally {
                running--
                stopIfIdle()
            }
        }
        return START_NOT_STICKY
    }

    private fun show(status: RedemptionStatus) {
        // Remembered for the next open (spec, "Status beim nächsten Öffnen"); the type only, never the code.
        if (status is RedemptionStatus.Finished) lastResult.record(status.result)
        when (status) {
            RedemptionStatus.Redeeming -> Unit // Already on screen via startForeground.
            // Removed as soon as the result is in, not only when the service stops after the banner. Kept while an
            // overlapping redemption still runs or shows an error.
            is RedemptionStatus.Finished if status.result.unlocked ->
                if (running == 1 && !keepNotification) stopForeground(STOP_FOREGROUND_REMOVE)
            is RedemptionStatus.Finished -> {
                notifications.showError(status.result)
                keepNotification = true
            }
        }
    }

    private fun stopIfIdle() {
        if (running > 0) return
        stopForeground(if (keepNotification) STOP_FOREGROUND_DETACH else STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** A short service must stop within seconds of its time running out; the 25 s limit makes this a safety net. */
    override fun onTimeout(startId: Int) {
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /**
         * One per process, so the lock state read when the entry screen opens reaches the service; like the code, it
         * lives in memory only (spec, "Einlöse-Orchestrator (`Redemption`)"). The gateway in it keeps the host that
         * answered.
         */
        private val redemption = Redemption(HttpFritzGateway(), TimeSource.Monotonic)

        /** Outlives the activity, which closes while the reading may still run. */
        private val processScope = MainScope()

        /** Starts reading the lock state. Call when the entry screen opens. Returns the state once read, for the timers. */
        fun prepare(): Deferred<LockState?> = redemption.prepare(processScope)

        /** Carries the code from the activity to the service in memory only, never in the (persistable) intent. */
        private val handoff = AtomicReference<TicketCode?>(null)

        /**
         * Starts a redemption of [code]. Call while the activity is visible: only then may a foreground service
         * start. If the start fails, the code is discarded and the exception propagates.
         */
        fun start(context: Context, code: TicketCode) {
            handoff.getAndSet(code)?.discard()
            try {
                context.startForegroundService(Intent(context, RedemptionService::class.java))
            } catch (e: RuntimeException) {
                handoff.getAndSet(null)?.discard()
                throw e
            }
        }
    }
}
