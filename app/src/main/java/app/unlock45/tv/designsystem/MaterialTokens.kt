package app.unlock45.tv.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** The background and the milky glass on it. Each glass surface is a vertical gradient, lighter at the top edge. */
object MaterialTokens {
    /** Full-screen background: a soft diagonal gradient that stays calm and never glares. */
    val Background = Brush.linearGradient(
        listOf(ColorTokens.BackgroundIceBlue, ColorTokens.BackgroundOffWhite, ColorTokens.BackgroundLavender),
    )

    val Field = GlassFill(
        top = ColorTokens.GlassWhite.copy(alpha = 0.72f),
        bottom = ColorTokens.GlassWhite.copy(alpha = 0.52f),
    )

    /** The focused field's glass is brightened. */
    val FieldFocused = GlassFill(
        top = ColorTokens.GlassWhite.copy(alpha = 0.92f),
        bottom = ColorTokens.GlassWhite.copy(alpha = 0.80f),
    )

    /** The result banner lies over any app, often dark video, so its glass is denser to keep the text readable. */
    val Banner = GlassFill(
        top = ColorTokens.GlassWhite.copy(alpha = 0.94f),
        bottom = ColorTokens.GlassWhite.copy(alpha = 0.86f),
    )
}

/** A glass surface as the two stops of its vertical gradient. */
data class GlassFill(val top: Color, val bottom: Color) {
    /** The surface [fraction] of the way from this one to [other]. */
    fun lerpTo(other: GlassFill, fraction: Float) =
        GlassFill(lerp(top, other.top, fraction), lerp(bottom, other.bottom, fraction))
}
