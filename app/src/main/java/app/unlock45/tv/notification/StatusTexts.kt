package app.unlock45.tv.notification

import app.unlock45.tv.domain.RedemptionResult
import app.unlock45.tv.domain.ResultKind

/** Status texts (spec, "Ergebnistypen und Meldungstexte", "Status beim nächsten Öffnen"). */
internal object StatusTexts {

    const val REDEEMING = "45 Minuten werden freigeschaltet …"

    /** The text right after a redemption: an added ticket also says how much time there is in all. */
    fun of(result: RedemptionResult): String = when (result) {
        is RedemptionResult.TicketAdded -> "45 Minuten vorgemerkt – insgesamt noch ${result.totalMinutes} Minuten"
        else -> of(ResultKind.of(result))
    }

    /** The text for a result type, also on the next open, when an added ticket's total would be stale. */
    fun of(kind: ResultKind): String = when (kind) {
        ResultKind.Success -> "45 Minuten freigeschaltet"
        ResultKind.TicketAdded -> "45 Minuten vorgemerkt"
        ResultKind.NotRestricted -> "Für dieses Gerät gibt es keine Beschränkung"
        ResultKind.InvalidOrUsedCode, ResultKind.Unrecognized -> "Code ungültig oder bereits verwendet"
        ResultKind.RouterUnreachable -> "FRITZ!Box nicht erreichbar"
        ResultKind.Timeout -> "FRITZ!Box antwortet nicht"
        ResultKind.NoLockPageActive -> "Momentan ist keine FRITZ!Box-Sperrseite aktiv"
        ResultKind.DeviceBlocked -> "Gerätesperre aktiv – Ticket nicht möglich"
    }
}
