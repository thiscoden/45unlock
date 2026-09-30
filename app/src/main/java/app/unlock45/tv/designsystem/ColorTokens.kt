package app.unlock45.tv.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Colour palette of the 45unlock Glass Design System (spec, "Design-System"). Surfaces that combine these colours
 * with transparency live in [MaterialTokens].
 */
object ColorTokens {
    /** Electric Blue, reserved for focus and primary interaction. */
    val ElectricBlue = Color(0xFF0A84FF)

    /** Background gradient: ice blue, muted off-white and lavender. Never pure white, so it does not glare at night. */
    val BackgroundIceBlue = Color(0xFFE2EAF3)

    /** Also the window background shown before Compose draws; matches `@color/window_background`. */
    val BackgroundOffWhite = Color(0xFFEEF0F4)
    val BackgroundLavender = Color(0xFFE6E2F0)

    /** Base of the milky glass; only ever used with transparency (see [MaterialTokens]). */
    val GlassWhite = Color(0xFFFFFFFF)

    /** Anthracite for the digits and primary text. */
    val TextPrimary = Color(0xFF1F2328)

    /** Blue-grey for secondary text. */
    val TextSecondary = Color(0xFF66758A)

    /** Ticket timers: dark grey for the minutes and the part of the ticket left, light grey for the part used. */
    val TimerRemaining = Color(0xFF4B525C)
    val TimerUsed = Color(0xFFD5DAE1)

    /** Light blue-grey for the outline of unfocused glass fields. */
    val OutlineSubtle = Color(0xFFB7C0CC)

    /** Restrained green for success. */
    val Success = Color(0xFF2F9E6B)

    /** Warm red for real errors. */
    val Error = Color(0xFFDD5541)

    /** Hints in the result banner carry the blue-grey of secondary text, calm like the status line. */
    val Hint = TextSecondary

    /** Cool blue-grey tint for the soft shadows under the glass, so they never look black. */
    val ShadowTint = Color(0xFF4A5A78)
}
