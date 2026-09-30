package app.unlock45.tv.domain

/** The type of a [RedemptionResult] without its data, the only part remembered for the next open. */
enum class ResultKind {
    Success,
    TicketAdded,
    InvalidOrUsedCode,
    NoLockPageActive,
    NotRestricted,
    DeviceBlocked,
    RouterUnreachable,
    Timeout,
    Unrecognized,
    ;

    companion object {
        fun of(result: RedemptionResult): ResultKind = when (result) {
            RedemptionResult.Success -> Success
            is RedemptionResult.TicketAdded -> TicketAdded
            RedemptionResult.InvalidOrUsedCode -> InvalidOrUsedCode
            RedemptionResult.NoLockPageActive -> NoLockPageActive
            RedemptionResult.NotRestricted -> NotRestricted
            RedemptionResult.DeviceBlocked -> DeviceBlocked
            RedemptionResult.RouterUnreachable -> RouterUnreachable
            RedemptionResult.Timeout -> Timeout
            RedemptionResult.Unrecognized -> Unrecognized
        }
    }
}

/** Where [LastResult] keeps its one small value; it survives the process ending. */
interface ResultStorage {
    fun read(): String?
    fun write(value: String)
    fun clear()
}

/**
 * The last redemption's result, shown once on the next open (spec, "Status beim nächsten Öffnen"). Only its
 * [ResultKind] and the time are kept, never the code. [now] is the wall clock in milliseconds.
 */
class LastResult(private val storage: ResultStorage, private val now: () -> Long) {

    fun record(result: RedemptionResult) {
        storage.write("${ResultKind.of(result).name}$SEPARATOR${now()}")
    }

    /**
     * The remembered result, if any; it is deleted as it is taken, so it shows once. A result older than one ticket
     * ([MAX_AGE_MILLIS]) is deleted unseen: by then it no longer says anything about now. So is one that cannot be
     * read, or one from the future after the clock was set back.
     */
    fun take(): ResultKind? {
        val stored = storage.read() ?: return null
        storage.clear()
        val kind = ResultKind.entries.firstOrNull { it.name == stored.substringBefore(SEPARATOR) } ?: return null
        val at = stored.substringAfter(SEPARATOR, missingDelimiterValue = "").toLongOrNull() ?: return null
        return kind.takeIf { now() - at in 0..MAX_AGE_MILLIS }
    }

    companion object {
        /** The length of one ticket. */
        const val MAX_AGE_MILLIS = TICKET_SECONDS * 1000L

        private const val SEPARATOR = '@'
    }
}
