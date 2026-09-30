package app.unlock45.tv.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.lerp

/**
 * The result banner (spec, "Ergebnis-Banner"): one line of [text] in a glass pill whose outline and leading dot carry
 * the [accent] colour. It sits centred at the top edge inside the TV safe margins, with room below for its shadow, and
 * fades and grows in while [visible], and back out when it turns false.
 */
@Composable
fun GlassBanner(text: String, accent: Color, visible: Boolean, modifier: Modifier = Modifier) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        shown.animateTo(if (visible) 1f else 0f, tween(MotionTokens.BannerFadeMillis, easing = MotionTokens.Standard))
    }
    val shadowPaint = remember { Paint() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = SpacingTokens.TvSafeMargin,
                top = SpacingTokens.TvSafeMargin,
                end = SpacingTokens.TvSafeMargin,
                bottom = SpacingTokens.Large,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    alpha = shown.value
                    // Per-draw alpha instead of an offscreen layer, which would clip the shadow.
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                    val scale = lerp(ComponentTokens.BannerHiddenScale, 1f, shown.value)
                    scaleX = scale
                    scaleY = scale
                }
                .height(ComponentTokens.BannerHeight)
                .drawBehind {
                    drawSoftShadow(shadowPaint, ElevationTokens.Banner, ShapeTokens.BannerRadius)
                    drawGlass(MaterialTokens.Banner, accent, ComponentTokens.BannerOutlineWidth, ShapeTokens.BannerRadius)
                }
                .padding(horizontal = ComponentTokens.BannerPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(ComponentTokens.BannerDotGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(ComponentTokens.BannerDotSize).background(accent, CircleShape))
            BasicText(text, style = TypographyTokens.Banner, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
