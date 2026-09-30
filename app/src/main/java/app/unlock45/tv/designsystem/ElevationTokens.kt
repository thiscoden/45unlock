package app.unlock45.tv.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Soft, tinted shadows that let the glass float; no hard Material-style drop shadows. */
object ElevationTokens {
    /** The fields stand free on the background with no card behind them, so their shadow carries the depth. */
    val Field = SoftShadow(
        color = ColorTokens.ShadowTint.copy(alpha = 0.16f),
        blur = 20.dp,
        offsetY = 6.dp,
    )

    /** The result banner floats like the fields. */
    val Banner = Field
}

/** A blurred copy of a surface's shape behind it, shifted down by [offsetY]. */
data class SoftShadow(val color: Color, val blur: Dp, val offsetY: Dp) {
    /** The same shadow, its opacity scaled by [fraction]. */
    fun fadedTo(fraction: Float) = copy(color = color.copy(alpha = color.alpha * fraction))
}
