package app.unlock45.tv.notification

import android.content.Context
import androidx.core.content.edit
import app.unlock45.tv.domain.LastResult
import app.unlock45.tv.domain.ResultStorage

/**
 * The last result in the app's private preferences: one small value with the result type and the time, never the
 * code. Written synchronously, so it survives the process ending right after the service stops.
 */
private class PreferencesResultStorage(context: Context) : ResultStorage {
    private val preferences = context.getSharedPreferences("last_result", Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString(KEY, null)

    override fun write(value: String) {
        preferences.edit(commit = true) { putString(KEY, value) }
    }

    override fun clear() {
        preferences.edit(commit = true) { remove(KEY) }
    }

    private companion object {
        const val KEY = "result"
    }
}

/** The last result, remembered by the service and shown once on the next open. */
fun openLastResult(context: Context) = LastResult(PreferencesResultStorage(context), System::currentTimeMillis)
