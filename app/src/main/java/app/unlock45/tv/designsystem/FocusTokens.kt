package app.unlock45.tv.designsystem

import androidx.compose.ui.unit.dp

/** The focused field: Electric Blue border, outer glow, brightened glass, slightly larger, calm pulse. */
object FocusTokens {
    val Tint = ColorTokens.ElectricBlue
    val BorderWidth = 2.dp
    const val Scale = 1.02f

    /** The focused field grows to this scale at the peak of its hint breath (see [MotionTokens.FocusNudge]). */
    const val NudgeScale = 1.06f

    /** Outer glow; drawn outside the field only, so the glass stays clear. */
    val GlowBlur = 16.dp

    /** The pulse only moves border intensity and glow between these bounds; it never fades out, so it never blinks. */
    const val BorderAlphaMin = 0.72f
    const val BorderAlphaMax = 1f
    const val GlowAlphaMin = 0.30f
    const val GlowAlphaMax = 0.55f
}
