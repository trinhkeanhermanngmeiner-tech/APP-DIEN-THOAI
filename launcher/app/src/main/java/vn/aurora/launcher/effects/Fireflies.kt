package vn.aurora.launcher.effects

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Firefly(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val riseSpeed: Float,
    val phase: Float,
    /** Closer flies move more with tilt, giving a sense of depth. */
    val depth: Float,
)

private val GlowColor = Color(0xFFFFF3B0)

/** Glowing particles drifting upward. Positions are a pure function of time: no per-frame allocation. */
@Composable
fun Fireflies(
    time: () -> Float,
    tilt: () -> Offset,
    modifier: Modifier = Modifier,
    count: Int = 36,
) {
    val flies = remember(count) {
        val random = Random(7)
        List(count) {
            Firefly(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radiusDp = 1.5f + random.nextFloat() * 2.5f,
                riseSpeed = 0.01f + random.nextFloat() * 0.025f,
                phase = random.nextFloat() * 2f * PI.toFloat(),
                depth = 0.3f + random.nextFloat(),
            )
        }
    }
    Spacer(
        modifier
            .fillMaxSize()
            .drawBehind {
                val t = time()
                val currentTilt = tilt()
                val parallaxPx = 28.dp.toPx()
                for (fly in flies) {
                    val y = ((fly.y - t * fly.riseSpeed) % 1f + 1f) % 1f
                    val x = fly.x + sin(t * 0.6f + fly.phase) * 0.03f
                    val center = Offset(
                        x * size.width + currentTilt.x * parallaxPx * fly.depth,
                        y * size.height + currentTilt.y * parallaxPx * fly.depth,
                    )
                    val twinkle = 0.5f + 0.5f * sin(t * 2f + fly.phase * 3f)
                    val radius = fly.radiusDp.dp.toPx()
                    drawCircle(
                        Brush.radialGradient(
                            listOf(GlowColor.copy(alpha = 0.5f * twinkle), Color.Transparent),
                            center = center,
                            radius = radius * 5f,
                        ),
                        radius = radius * 5f,
                        center = center,
                    )
                    drawCircle(Color.White.copy(alpha = 0.8f * twinkle), radius = radius * 0.6f, center = center)
                }
            },
    )
}
