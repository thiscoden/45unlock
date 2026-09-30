package app.unlock45.tv.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp

/**
 * One glass digit field. Its states (spec, "Design-System", Feldzustände):
 * - empty or filled, at rest
 * - [focused]: Electric Blue border, outer glow, brightened glass, slightly larger, calm pulse
 * - hint: while [nudge] is above 0 the field grows a little further and glows at full intensity; the caller passes
 *   it for the focused field only, when Enter finds empty fields left. There is no red error state.
 * - processing: [lit] from 0 to 1 gives the field the focus look without the pulse, used after Enter
 *
 * [lit] and [nudge] are read while drawing only, so animating them does not recompose.
 */
@Composable
fun GlassDigitField(
    digit: Int?,
    focused: Boolean,
    modifier: Modifier = Modifier,
    lit: () -> Float = { 0f },
    nudge: () -> Float = { 0f },
) {
    val focus = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(MotionTokens.FocusChangeMillis, easing = MotionTokens.Standard),
        label = "focus",
    )
    // Only the focused field pulses. A pulse starts at full intensity, the value an unfocused field holds.
    val pulse = if (focused) focusPulse() else null
    val shadowPaint = remember { Paint() }

    // The focus look, from focus or from lighting up after Enter. Lighting up holds the pulse at full intensity.
    fun look() = maxOf(focus.value, lit())
    fun intensity() = maxOf(lit() + (1f - lit()) * (pulse?.value ?: 1f), nudge())

    Box(
        modifier = modifier
            .size(ComponentTokens.DigitFieldWidth, ComponentTokens.DigitFieldHeight)
            .graphicsLayer {
                val scale = lerp(lerp(1f, FocusTokens.Scale, look()), FocusTokens.NudgeScale, nudge())
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                val look = look()
                val intensity = intensity()
                drawSoftShadow(shadowPaint, ElevationTokens.Field.fadedTo(1f - look), ShapeTokens.DigitFieldRadius)
                val glowAlpha = lerp(FocusTokens.GlowAlphaMin, FocusTokens.GlowAlphaMax, intensity) * look
                drawSoftShadow(
                    shadowPaint,
                    SoftShadow(FocusTokens.Tint.copy(alpha = glowAlpha), FocusTokens.GlowBlur, 0.dp),
                    ShapeTokens.DigitFieldRadius,
                )
                val borderAlpha = lerp(FocusTokens.BorderAlphaMin, FocusTokens.BorderAlphaMax, intensity)
                val border = lerp(ColorTokens.OutlineSubtle, FocusTokens.Tint.copy(alpha = borderAlpha), look)
                val borderWidth = lerp(ComponentTokens.DigitFieldOutlineWidth, FocusTokens.BorderWidth, look)
                drawGlass(
                    fill = MaterialTokens.Field.lerpTo(MaterialTokens.FieldFocused, look),
                    outline = border,
                    outlineWidth = borderWidth,
                    cornerRadius = ShapeTokens.DigitFieldRadius,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (digit != null) Digit(digit)
    }
}

/** A new digit fades and grows in; replacing a digit plays it again. */
@Composable
private fun Digit(digit: Int) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(digit) {
        appear.snapTo(0f)
        appear.animateTo(1f, tween(MotionTokens.DigitMillis, easing = MotionTokens.Standard))
    }
    BasicText(
        text = digit.toString(),
        style = TypographyTokens.Digit,
        modifier = Modifier.graphicsLayer {
            alpha = appear.value
            val scale = lerp(ComponentTokens.DigitEnterScale, 1f, appear.value)
            scaleX = scale
            scaleY = scale
        },
    )
}

/** Breathes from 1 to 0 and back once per [MotionTokens.PulseMillis]. */
@Composable
private fun focusPulse(): State<Float> = rememberInfiniteTransition(label = "pulse").animateFloat(
    initialValue = 1f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
        animation = tween(MotionTokens.PulseMillis / 2, easing = MotionTokens.Pulse),
        repeatMode = RepeatMode.Reverse,
    ),
    label = "pulse",
)
