package app.unlock45.tv.notification

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.unlock45.tv.designsystem.ColorTokens
import app.unlock45.tv.designsystem.GlassBanner
import app.unlock45.tv.designsystem.MotionTokens
import app.unlock45.tv.domain.RedemptionResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Shows the result of a redemption over whatever app is running, since the notification is rarely visible on
 * Android TV (spec, "Ergebnis-Banner"). The banner is an overlay window that never takes focus or keys and is not
 * modal. Without the overlay permission, a plain text toast stands in.
 */
class ResultBanner(private val context: Context) {

    /**
     * Completed when a newer banner replaces the one on screen. A newer result cuts the older banner short rather
     * than queueing behind it, so each banner extends the service by at most its own display time.
     */
    private var replaced: CompletableDeferred<Unit>? = null

    /**
     * Shows [result] and returns once the banner has faded out or a newer one has replaced it; a toast fallback
     * returns at once. Call on the main thread.
     */
    suspend fun show(result: RedemptionResult) {
        val message = BannerMessage.of(result)
        if (!Settings.canDrawOverlays(context)) {
            toast(message)
            return
        }
        showOverlay(message)
    }

    private suspend fun showOverlay(message: BannerMessage) {
        // A window context, as the service's own context is not meant for adding windows.
        val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        val windowContext = context.createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        val windowManager = windowContext.getSystemService(WindowManager::class.java)

        var visible by mutableStateOf(false)
        val owner = OverlayOwner()
        val view = ComposeView(windowContext).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { GlassBanner(message.text, accentOf(message.tone), visible) }
        }
        owner.start()
        try {
            windowManager.addView(view, layoutParams())
        } catch (e: RuntimeException) {
            // The permission can be revoked between the check and here.
            owner.destroy()
            toast(message)
            return
        }
        replaced?.complete(Unit)
        val mine = CompletableDeferred<Unit>()
        replaced = mine
        try {
            visible = true
            val onScreen = (MotionTokens.BannerFadeMillis + MotionTokens.BannerHoldMillis).toLong()
            if (withTimeoutOrNull(onScreen) { mine.await() } != null) return
            visible = false
            delay(MotionTokens.BannerFadeMillis.toLong())
        } finally {
            windowManager.removeViewImmediate(view)
            owner.destroy()
            if (replaced === mine) replaced = null
        }
    }

    private fun toast(message: BannerMessage) {
        Toast.makeText(context, message.text, Toast.LENGTH_SHORT).show()
    }

    private fun accentOf(tone: BannerTone) = when (tone) {
        BannerTone.Success -> ColorTokens.Success
        BannerTone.Error -> ColorTokens.Error
        BannerTone.Hint -> ColorTokens.Hint
    }

    /** Full width, so the banner centres itself; the window is only as high as the banner and its shadow. */
    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // Not focusable also makes it not touch-modal: keys and focus stay with the running app.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP
        title = "45unlock result"
    }
}

/** The lifecycle and saved state a [ComposeView] needs outside an activity; resumed for the banner's lifetime. */
private class OverlayOwner : SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry get() = savedState.savedStateRegistry

    fun start() {
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
