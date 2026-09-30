package app.unlock45.tv.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import app.unlock45.tv.designsystem.ComponentTokens
import app.unlock45.tv.designsystem.GlassDigitField
import app.unlock45.tv.designsystem.MaterialTokens
import app.unlock45.tv.designsystem.MotionTokens
import app.unlock45.tv.designsystem.SpacingTokens
import app.unlock45.tv.designsystem.StatusLine
import app.unlock45.tv.designsystem.TicketTimer
import app.unlock45.tv.domain.CodeEntry
import app.unlock45.tv.domain.ResultKind
import app.unlock45.tv.domain.TICKET_MINUTES
import app.unlock45.tv.domain.ticketTimers
import app.unlock45.tv.notification.StatusTexts
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * Six glass digit fields standing free on the background, two layers and no card (spec, "Design-System"), driven
 * entirely by [CodeEntry]. Keys are handled by the activity, so nothing here is focusable and no text field, cursor or
 * on-screen keyboard exists.
 *
 * [nudgeSignal] changes each time an incomplete code is confirmed: focus is already on the first empty field, which
 * breathes once. No red and no shake, since nothing went wrong yet. [rejectSignal] changes when the system refused
 * the code and the entry starts over: the row shakes, like a wrong passcode.
 *
 * [ticketEndsAt] is when the time on the box runs out, on the [SystemClock.elapsedRealtime] clock, or null without a
 * running ticket. Small timers top right count it down, one per ticket (spec, "Design-System", Ticket-Timer).
 *
 * [status] is the last redemption's result, shown above the row for a few seconds each time [statusSignal] changes
 * (spec, "Status beim nächsten Öffnen"). Its space stays reserved, so the row never moves, and it takes no keys: entry
 * works at once.
 *
 * Once [entry] is submitting, the confirm sequence runs (spec, "Abfolge nach Enter"): the fields light up, the digits
 * group, then [onHandover] hands the code to the service. If that fails, the activity resets [entry] and the screen
 * returns to rest. Otherwise the row shrinks and fades out, then [onClosed] finishes the activity.
 */
@Composable
internal fun CodeEntryScreen(
    entry: CodeEntry,
    ticketEndsAt: Long?,
    status: ResultKind?,
    statusSignal: Int,
    nudgeSignal: Int,
    rejectSignal: Int,
    onHandover: () -> Boolean,
    onClosed: () -> Unit,
) {
    val shake = remember { Animatable(0.dp, Dp.VectorConverter) }
    LaunchedEffect(rejectSignal) {
        if (rejectSignal == 0) return@LaunchedEffect
        shake.animateTo(0.dp, MotionTokens.rejectShake(ComponentTokens.RejectShakeOffset))
    }
    val nudge = remember { Animatable(0f) }
    LaunchedEffect(nudgeSignal) {
        if (nudgeSignal == 0) return@LaunchedEffect
        nudge.animateTo(0f, MotionTokens.FocusNudge)
    }

    var statusVisible by remember { mutableStateOf(false) }
    LaunchedEffect(statusSignal) {
        if (statusSignal == 0) return@LaunchedEffect
        statusVisible = true
        delay((MotionTokens.StatusFadeMillis + MotionTokens.StatusHoldMillis).toLong())
        statusVisible = false
    }

    val lit = remember { Animatable(0f) }
    val gap = remember { Animatable(ComponentTokens.DigitGap, Dp.VectorConverter) }
    val close = remember { Animatable(0f) }
    val handover by rememberUpdatedState(onHandover)
    val closed by rememberUpdatedState(onClosed)
    LaunchedEffect(entry.isSubmitting) {
        if (!entry.isSubmitting) {
            lit.snapTo(0f)
            gap.snapTo(ComponentTokens.DigitGap)
            close.snapTo(0f)
            return@LaunchedEffect
        }
        lit.animateTo(1f, tween(MotionTokens.ConfirmGlowMillis, easing = MotionTokens.Standard))
        gap.animateTo(ComponentTokens.DigitGapGrouped, tween(MotionTokens.ConfirmGroupMillis, easing = MotionTokens.Standard))
        if (!handover()) return@LaunchedEffect
        close.animateTo(1f, tween(MotionTokens.CloseMillis, easing = MotionTokens.Standard))
        closed()
    }

    // Minutes per ticket timer, counted down locally from the reading; empty without a running ticket.
    val timers by produceState(emptyList<Int>(), ticketEndsAt) {
        val end = ticketEndsAt
        do {
            val secondsLeft = end?.let { (it - SystemClock.elapsedRealtime()).milliseconds.inWholeSeconds.toInt() } ?: 0
            value = ticketTimers(secondsLeft)
            delay(MotionTokens.TimerTickMillis)
        } while (value.isNotEmpty())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTokens.Background)
            .padding(SpacingTokens.TvSafeMargin),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(ComponentTokens.DigitRowWidth)
                .graphicsLayer {
                    val scale = lerp(1f, ComponentTokens.RowClosedScale, close.value)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - close.value
                    // Per-draw alpha instead of an offscreen layer, which would clip the fields' shadows and glow.
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusLine(text = status?.let { StatusTexts.of(it) }.orEmpty(), visible = statusVisible)
            Spacer(Modifier.height(ComponentTokens.StatusGap))
            Row(
                modifier = Modifier.offset { IntOffset(shake.value.roundToPx(), 0) },
                horizontalArrangement = Arrangement.spacedBy(gap.value),
            ) {
                entry.digits.forEachIndexed { index, digit ->
                    GlassDigitField(
                        digit = digit,
                        focused = index == entry.focus,
                        lit = { lit.value },
                        nudge = { if (index == entry.focus) nudge.value else 0f },
                    )
                }
            }
        }
        // The last timers stay drawn while they fade out once the time is up.
        var shownTimers by remember { mutableStateOf(timers) }
        if (timers.isNotEmpty()) shownTimers = timers
        AnimatedVisibility(
            visible = timers.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .graphicsLayer { alpha = 1f - close.value },
            enter = fadeIn(tween(MotionTokens.TimerAppearMillis)) +
                scaleIn(tween(MotionTokens.TimerAppearMillis), initialScale = ComponentTokens.TimerHiddenScale),
            exit = fadeOut(tween(MotionTokens.TimerAppearMillis)),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(ComponentTokens.TimerGap)) {
                shownTimers.forEach { minutes -> TicketTimer(minutes, fill = minutes.toFloat() / TICKET_MINUTES) }
            }
        }
    }
}
