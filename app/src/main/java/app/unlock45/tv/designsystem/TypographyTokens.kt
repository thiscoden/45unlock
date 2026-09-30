package app.unlock45.tv.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.unlock45.tv.R

/** Inter (SIL Open Font License, see `licenses/Inter-OFL.txt`), bundled so the look is the same on every box. */
object TypographyTokens {
    private val InterDisplay = FontFamily(Font(R.font.inter_display_semibold, FontWeight.SemiBold))

    /** Large, tabular digits readable from TV distance; the visual centre of the screen. */
    val Digit = TextStyle(
        color = ColorTokens.TextPrimary,
        fontFamily = InterDisplay,
        fontSize = 64.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    )

    /** The status line on opening: secondary, calm, still readable from TV distance. */
    val Status = TextStyle(
        color = ColorTokens.TextSecondary,
        fontFamily = InterDisplay,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
    )

    /** The minutes inside a ticket timer: small, dark grey, tabular. */
    val Timer = TextStyle(
        color = ColorTokens.TimerRemaining,
        fontFamily = InterDisplay,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    )

    /** The one line of the result banner, readable from TV distance over any app. */
    val Banner = TextStyle(
        color = ColorTokens.TextPrimary,
        fontFamily = InterDisplay,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
