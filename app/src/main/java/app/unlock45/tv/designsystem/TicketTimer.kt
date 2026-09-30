package app.unlock45.tv.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * One small ticket timer (spec, "Design-System", Ticket-Timer): [minutes] in dark grey in the middle of a donut whose
 * dark grey arc is the part of the ticket left ([fill], 0 to 1, from 12 o'clock) and whose light grey rest is the part
 * used, so the ring stays whole. Unobtrusive, takes no focus and no keys.
 */
@Composable
fun TicketTimer(minutes: Int, fill: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ComponentTokens.TimerSize)
            .drawBehind {
                val stroke = ComponentTokens.TimerStroke.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = ColorTokens.TimerUsed,
                    startAngle = ComponentTokens.TimerStartAngle,
                    sweepAngle = ComponentTokens.TimerFullSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke),
                )
                drawArc(
                    color = ColorTokens.TimerRemaining,
                    startAngle = ComponentTokens.TimerStartAngle,
                    sweepAngle = ComponentTokens.TimerFullSweep * fill,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(stroke),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(minutes.toString(), style = TypographyTokens.Timer)
    }
}
