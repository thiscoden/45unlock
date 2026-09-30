package app.unlock45.tv.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import app.unlock45.tv.R
import app.unlock45.tv.domain.RedemptionResult

/**
 * The single, unobtrusive notification of a redemption (spec, "Hintergrunddienst und Benachrichtigung"): low
 * importance, so no sound, no vibration and no heads-up. An error replaces the in-progress text; after success the
 * notification is removed.
 */
class RedemptionNotifications(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        val channel = NotificationChannel(CHANNEL_ID, "Ticket einlösen", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun redeeming(): Notification = build(StatusTexts.REDEEMING, Notification.CATEGORY_PROGRESS)

    /** Posts the error in place of the in-progress notification; it stays after the service ends. */
    fun showError(result: RedemptionResult) {
        manager.notify(ID, build(StatusTexts.of(result), Notification.CATEGORY_STATUS))
    }

    private fun build(text: String, category: String): Notification =
        Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(text)
            .setCategory(category)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()

    companion object {
        const val ID = 1
        private const val CHANNEL_ID = "redemption"
    }
}
