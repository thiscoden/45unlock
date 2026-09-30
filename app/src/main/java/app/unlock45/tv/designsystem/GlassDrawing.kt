package app.unlock45.tv.designsystem

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

/** A glass surface of this draw scope's size: gradient [fill] plus an [outline] drawn just inside the edge. */
internal fun DrawScope.drawGlass(fill: GlassFill, outline: Color, outlineWidth: Dp, cornerRadius: Dp) {
    val radius = cornerRadius.toPx()
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(fill.top, fill.bottom)),
        cornerRadius = CornerRadius(radius),
    )
    val stroke = outlineWidth.toPx()
    drawRoundRect(
        color = outline,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius - stroke / 2),
        style = Stroke(stroke),
    )
}

/**
 * Draws [shadow] for a rounded rectangle of this draw scope's size: a soft shadow or, in Electric Blue, the focus
 * glow. Only the part outside the shape is drawn, so the translucent glass on top stays clear. [paint] is reused
 * between frames.
 */
internal fun DrawScope.drawSoftShadow(paint: Paint, shadow: SoftShadow, cornerRadius: Dp) {
    if (shadow.color.alpha == 0f) return
    val radius = CornerRadius(cornerRadius.toPx())
    val outline = Path().apply { addRoundRect(RoundRect(size.toRect(), radius)) }
    clipPath(outline, ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            val framework = paint.asFrameworkPaint().apply {
                isAntiAlias = true
                color = shadow.color.toArgb()
                maskFilter = BlurMaskFilter(shadow.blur.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            val dy = shadow.offsetY.toPx()
            canvas.nativeCanvas.drawRoundRect(0f, dy, size.width, size.height + dy, radius.x, radius.y, framework)
        }
    }
}
