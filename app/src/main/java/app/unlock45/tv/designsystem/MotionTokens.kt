package app.unlock45.tv.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.ui.unit.Dp

/** Short, calm motion (spec, "Design-System", Motion). */
object MotionTokens {
    const val FocusChangeMillis = 180
    const val DigitMillis = 140
    const val PulseMillis = 1600
    const val RejectShakeMillis = 280
    const val CloseMillis = 220
    const val SuccessMillis = 320

    /** The focused field breathes once when Enter finds empty fields left. */
    const val NudgeMillis = 280

    /** The result banner fades in and out like the success transition, whatever the result. */
    const val BannerFadeMillis = SuccessMillis

    /** The result banner stays this long between fading in and out; about 5.6 s on screen in all (spec, "Ergebnis-Banner"). */
    const val BannerHoldMillis = 5000

    /**
     * The ticket timers pop up and fade gently. They look at the clock once a second, so the minute shown changes
     * right when it is used up.
     */
    const val TimerAppearMillis = SuccessMillis
    const val TimerTickMillis = 1000L

    /** The status line on opening fades in and out like the banner and stays about 4 s in between. */
    const val StatusFadeMillis = BannerFadeMillis
    const val StatusHoldMillis = 4000

    /**
     * Steps after Enter: the fields light up, then the digits group; closing takes [CloseMillis]. The spec gives no
     * values for the first two; they are kept short, since the service only starts after them.
     */
    const val ConfirmGlowMillis = 140
    const val ConfirmGroupMillis = 180

    /** Decelerating ease for everything that moves or changes state. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Sine-like ease for the focus pulse, so it breathes rather than blinks. */
    val Pulse: Easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

    /**
     * Rejection motion, only when the system refused the code and the entry starts over: a damped horizontal shake
     * of [amplitude] that settles back at rest, like a wrong passcode. Never red, never for a merely incomplete code.
     */
    fun rejectShake(amplitude: Dp): KeyframesSpec<Dp> = keyframes {
        durationMillis = RejectShakeMillis
        val step = RejectShakeMillis / 6
        -amplitude at step
        amplitude at step * 2
        -amplitude / 2 at step * 3
        amplitude / 2 at step * 4
    }

    /**
     * Hint at the first empty field, 0 to 1 and back: the focused field breathes once. A calm pointer to what is
     * missing, not an error.
     */
    val FocusNudge: KeyframesSpec<Float> = keyframes {
        durationMillis = NudgeMillis
        1f at NudgeMillis * 2 / 5 using Standard
    }
}
