package app.unlock45.tv.notification

import app.unlock45.tv.domain.RedemptionResult

/**
 * The accent colour of the result banner: restrained green for success, warm red for real errors, calm blue-grey for
 * a hint that there is nothing to unlock (spec, "Ergebnis-Banner").
 */
enum class BannerTone { Success, Error, Hint }

/** What the result banner shows for a finished redemption (spec, "Ergebnis-Banner"). */
data class BannerMessage(val text: String, val tone: BannerTone) {
    companion object {
        fun of(result: RedemptionResult) = BannerMessage(
            text = StatusTexts.of(result),
            tone = when {
                result.unlocked -> BannerTone.Success
                result in hints -> BannerTone.Hint
                else -> BannerTone.Error
            },
        )

        /** Nothing was sent, because there is nothing to unlock. */
        private val hints = setOf(RedemptionResult.NoLockPageActive, RedemptionResult.NotRestricted)
    }
}
