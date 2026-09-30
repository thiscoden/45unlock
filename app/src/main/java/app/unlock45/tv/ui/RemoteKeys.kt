package app.unlock45.tv.ui

import android.view.KeyEvent
import app.unlock45.tv.domain.CodeEntry.Input

/** Translates remote-control keys to code entry input (spec, "Eingabe-State-Machine (`CodeEntry`)"). */
internal fun codeInputFor(keyCode: Int): Input? = when (keyCode) {
    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> Input.Digit(keyCode - KeyEvent.KEYCODE_0)
    in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> Input.Digit(keyCode - KeyEvent.KEYCODE_NUMPAD_0)
    KeyEvent.KEYCODE_DPAD_LEFT -> Input.Left
    KeyEvent.KEYCODE_DPAD_RIGHT -> Input.Right
    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DEL -> Input.Delete
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_BUTTON_SELECT -> Input.Confirm
    else -> null
}

/** Held D-pad keys may move focus repeatedly; a held digit, delete or confirm counts once. */
internal fun Input.acceptsKeyRepeat(): Boolean = this == Input.Left || this == Input.Right
