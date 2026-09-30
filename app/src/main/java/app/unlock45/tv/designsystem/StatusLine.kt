package app.unlock45.tv.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow

/**
 * One calm line of secondary text on the background (spec, "Status beim nächsten Öffnen"), with no card, pill, dialog
 * or colour: red stays with the result banner, and the entry screen only ever shows the blue focus. It fades in while
 * [visible] and back out when it turns false. Its height is always reserved, so nothing around it moves.
 */
@Composable
fun StatusLine(text: String, visible: Boolean, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        shown.animateTo(if (visible) 1f else 0f, tween(MotionTokens.StatusFadeMillis, easing = MotionTokens.Standard))
    }

    Box(
        modifier = modifier
            .height(ComponentTokens.StatusLineHeight)
            .graphicsLayer { alpha = shown.value },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(text, style = TypographyTokens.Status, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
