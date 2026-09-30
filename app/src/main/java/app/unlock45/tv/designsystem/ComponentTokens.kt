package app.unlock45.tv.designsystem

import androidx.compose.ui.unit.dp
import app.unlock45.tv.domain.CodeEntry

object ComponentTokens {
    val DigitFieldWidth = 96.dp
    val DigitFieldHeight = 120.dp
    val DigitFieldOutlineWidth = 1.dp
    val DigitGap = SpacingTokens.Small

    /** Gap after Enter, when the digits group into one code. */
    val DigitGapGrouped = SpacingTokens.XXSmall

    /** Width of the six fields at rest; the row keeps it while the digits group, so nothing else moves. */
    val DigitRowWidth = DigitFieldWidth * CodeEntry.LENGTH + DigitGap * (CodeEntry.LENGTH - 1)

    /** A new digit grows in from this scale. */
    const val DigitEnterScale = 0.9f

    /** The digit row shrinks to this scale while it fades out after Enter. */
    const val RowClosedScale = 0.96f

    /** Horizontal travel of the rejection motion. */
    val RejectShakeOffset = 12.dp

    /** The result banner: a single-line glass pill over other apps. */
    val BannerHeight = 72.dp
    val BannerPaddingHorizontal = SpacingTokens.Large

    /** Its outline and the dot before the text carry the accent colour. */
    val BannerOutlineWidth = FocusTokens.BorderWidth
    val BannerDotSize = SpacingTokens.Small
    val BannerDotGap = SpacingTokens.Small

    /** The banner grows in from and shrinks back to this scale while it fades. */
    const val BannerHiddenScale = 0.96f

    /** The status line on opening; its height stays reserved while it is hidden. */
    val StatusLineHeight = SpacingTokens.Large

    /** Between the status line and the digit row. */
    val StatusGap = SpacingTokens.Large

    /** A ticket timer top right: a small donut, about a quarter of a digit field's area, unobtrusive. */
    val TimerSize = 56.dp
    val TimerStroke = 5.dp
    val TimerGap = SpacingTokens.XSmall

    /** The arc of time left starts at 12 o'clock and runs clockwise; a full ticket is the whole ring. */
    const val TimerStartAngle = -90f
    const val TimerFullSweep = 360f

    /** A timer grows in from this scale when the reading arrives. */
    const val TimerHiddenScale = 0.9f
}
