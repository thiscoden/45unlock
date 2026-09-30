package app.unlock45.tv.domain

/** State of the six-digit ticket code entry (spec, "Eingabe-State-Machine (`CodeEntry`)"). */
class CodeEntry private constructor(
    val digits: List<Int?>,
    val focus: Int,
    val isSubmitting: Boolean,
) {
    /** Remote-control input, already translated from Android key events by the UI layer. */
    sealed interface Input {
        data class Digit(val value: Int) : Input {
            init {
                require(value in 0..9) { "Not a digit" }
            }

            override fun toString() = "Digit($REDACTED)"
        }

        data object Left : Input
        data object Right : Input
        data object Delete : Input
        data object Confirm : Input
    }

    /** One-off signal for the UI layer. */
    sealed interface Event {
        /** Delete on empty field 1: close the app, like Back anywhere else. */
        data object Dismiss : Event

        /** Confirm with empty fields left: hint at the first empty field, which now has focus. Not an error. */
        data object Incomplete : Event

        /** Confirm with all six digits: hand the code over for redemption. Raised at most once per entry. */
        class Submit(val code: String) : Event {
            override fun toString() = "Submit($REDACTED)"
        }
    }

    /** The entry after an input, plus the event it raised, if any. */
    class Transition(val entry: CodeEntry, val event: Event? = null)

    fun on(input: Input): Transition = if (isSubmitting) Transition(this) else when (input) {
        is Input.Digit -> Transition(withDigit(focus, input.value).withFocus(focus + 1))
        Input.Left -> Transition(withFocus(focus - 1))
        Input.Right -> Transition(withFocus(focus + 1))
        Input.Delete -> when {
            digits[focus] != null -> Transition(withDigit(focus, null))
            focus == 0 -> Transition(this, Event.Dismiss)
            else -> Transition(withFocus(focus - 1).withDigit(focus - 1, null))
        }
        Input.Confirm -> {
            val firstEmpty = digits.indexOfFirst { it == null }
            if (firstEmpty >= 0) {
                Transition(withFocus(firstEmpty), Event.Incomplete)
            } else {
                Transition(CodeEntry(digits, focus, isSubmitting = true), Event.Submit(digits.joinToString("")))
            }
        }
    }

    private fun withFocus(index: Int) = CodeEntry(digits, index.coerceIn(0, LENGTH - 1), isSubmitting)

    private fun withDigit(index: Int, value: Int?) =
        CodeEntry(digits.toMutableList().also { it[index] = value }, focus, isSubmitting)

    /** Ticket codes must never reach a log (spec, "Sicherheit und Netzwerk"). */
    override fun toString() = "CodeEntry($REDACTED)"

    companion object {
        const val LENGTH = 6
        private const val REDACTED = "<redacted>"

        fun empty(): CodeEntry = CodeEntry(digits = List(LENGTH) { null }, focus = 0, isSubmitting = false)
    }
}
