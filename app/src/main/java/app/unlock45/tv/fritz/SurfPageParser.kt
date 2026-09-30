package app.unlock45.tv.fritz

import app.unlock45.tv.domain.LockState
import app.unlock45.tv.domain.RedemptionResult
import app.unlock45.tv.domain.TICKET_SECONDS
import app.unlock45.tv.domain.minutesRoundedUp
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Reads the parental-control page `GET /` and the answer to `POST /surf.lua` (spec, "Kindersicherungs-API").
 * The page embeds the whole state as one line `var data = {…};`; the POST answer is that same JSON on its own.
 * Anything missing or of an unexpected type makes the page unreadable rather than falling back to a default. The one
 * leniency: numbers may come as strings, because the box sends some of them that way. A page without the data block
 * is the box's login page when it loads the login script, and unreadable otherwise.
 */
object SurfPageParser {

    fun lockState(page: String): LockState {
        val block = dataBlock.find(page)
            // A device the parental control does not restrict right now gets the login page instead (spec,
            // "Kindersicherungs-API", seen with the time restriction switched off).
            ?: return if (LOGIN_SCRIPT in page) LockState.NotLocked else LockState.Unrecognized
        return parseObject(block.groupValues[1])?.let(::lockState) ?: LockState.Unrecognized
    }

    /**
     * An accepted ticket is [RedemptionResult.TicketAdded] when the box then holds more than one ticket's time: only
     * a ticket added to a running one can (spec, "Ticket vormerken", 41 + 45 = 86 min on the device).
     */
    fun submitResult(answer: String): RedemptionResult {
        val data = parseObject(answer)
        return when (data?.string("ticket_successful")) {
            "accepted" -> {
                // Without a readable remaining time the ticket still went through; it only cannot be told apart.
                val sec = data.int(TICKET_VALID_SEC) ?: 0
                if (sec > TICKET_SECONDS) {
                    RedemptionResult.TicketAdded(totalMinutes = minutesRoundedUp(sec))
                } else {
                    RedemptionResult.Success
                }
            }
            "unaccepted" -> RedemptionResult.InvalidOrUsedCode
            else -> RedemptionResult.Unrecognized
        }
    }

    /** The order of the checks is the one in the spec; the first match wins. */
    private fun lockState(data: JsonObject): LockState? {
        val abuse = data.bool("abuse") ?: return null
        val deviceLocked = data.bool("device_locked") ?: return null
        val deviceUnlimited = data.bool("device_unlimited") ?: return null
        val ticketMode = data.string("ticket_mode") ?: return null
        val ticketActive = data.bool("ticket_active") ?: return null
        val ticketValidSec = data.int(TICKET_VALID_SEC) ?: return null
        val timePeriodAllowed = data.obj("time_period")?.bool("allowed") ?: return null
        val budget = data.obj("time_budget") ?: return null
        val budgetRestricted = budget.bool("restriction") ?: return null
        val budgetUnspentSec = budget.int("unspent") ?: return null
        val blockedSec = data.int("ticketblockedtime") ?: return null
        return when {
            abuse || deviceLocked -> LockState.DeviceBlocked
            deviceUnlimited || ticketMode.startsWith("disallowed") -> LockState.NotRestricted
            ticketActive -> LockState.TicketActive(ticketValidSec, blockedSec)
            timePeriodAllowed && (!budgetRestricted || budgetUnspentSec > 0) -> LockState.NotLocked
            else -> LockState.Locked(blockedSec)
        }
    }

    private fun parseObject(json: String): JsonObject? =
        try {
            Json.parseToJsonElement(json) as? JsonObject
        } catch (_: SerializationException) {
            null
        }

    /** The FRITZ!Box sends some numbers as strings (`"ticketblockedtime":"4"`), others as numbers. */
    private fun JsonObject.int(key: String) = (get(key) as? JsonPrimitive)?.content?.toIntOrNull()

    private fun JsonObject.bool(key: String) =
        (get(key) as? JsonPrimitive)?.takeUnless { it.isString }?.content?.toBooleanStrictOrNull()

    private fun JsonObject.string(key: String) = (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.obj(key: String) = get(key) as? JsonObject

    private const val TICKET_VALID_SEC = "ticket_valid_sec"

    /** Quoted as in the import, so a path merely ending in `login.js`, like `/js/box-login.js`, does not match. */
    private const val LOGIN_SCRIPT = "\"/js/login.js\""

    private val dataBlock = Regex("""var\s+data\s*=\s*(\{.*\})\s*;""")
}
