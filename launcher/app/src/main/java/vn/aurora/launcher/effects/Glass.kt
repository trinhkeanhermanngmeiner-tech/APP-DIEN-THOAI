package vn.aurora.launcher.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GlassEdge = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.06f)),
)

/**
 * Frosted glass: redraws the slice of the aurora that lies behind this panel, blurs it,
 * then adds a tint, a light rim and a highlight that slides as the phone is tilted.
 */
@Composable
fun GlassPanel(
    renderer: AuroraRenderer,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(32.dp),
    blurRadius: Dp = 30.dp,
    tint: Color = Color.White.copy(alpha = 0.10f),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(shape),
        contentAlignment = contentAlignment,
    ) {
        Spacer(
            Modifier
                .matchParentSize()
                .blur(blurRadius)
                .drawBehind { renderer.draw(this, origin) },
        )
        Spacer(
            Modifier
                .matchParentSize()
                .background(tint)
                .drawBehind {
                    val tilt = renderer.tilt()
                    val center = Offset(
                        size.width * (0.5f + tilt.x * 0.5f),
                        size.height * tilt.y * 0.5f,
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                            center = center,
                            radius = size.maxDimension * 0.6f,
                        ),
                    )
                }
                .border(1.dp, GlassEdge, shape),
        )
        content()
    }
}
