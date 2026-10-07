package vn.aurora.launcher.effects

import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.sin

/**
 * Animated aurora borealis: three waving light curtains, a violet horizon glow and
 * twinkling stars. Coordinates are in pixels of the full screen, so any panel can
 * redraw the exact same image behind itself (see [GlassPanel]).
 */
private const val AURORA_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iTilt;

float hash(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

float curtain(float2 p, float center, float freq, float speed, float t) {
    float wave = sin(p.x * freq + t * speed + sin(p.x * freq * 0.5 - t * 0.7) * 1.5);
    float y = center + wave * 0.07;
    float band = exp(-abs(p.y - y) * 18.0);
    float tail = exp(-max(y - p.y, 0.0) * 4.0) * step(p.y, y) * 0.35;
    return band + tail;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;
    float2 p = float2(uv.x * aspect, uv.y) + iTilt * 0.035;
    float t = iTime * 0.35;

    float3 col = mix(float3(0.015, 0.02, 0.06), float3(0.06, 0.02, 0.10), uv.y);
    float3 green = float3(0.10, 0.95, 0.65);
    float3 violet = float3(0.55, 0.30, 1.00);
    float3 pink = float3(1.00, 0.35, 0.65);

    float shimmer = 0.75 + 0.25 * sin(p.x * 22.0 + t * 3.0);
    col += green * curtain(p, 0.28, 3.0, 1.0, t) * 0.55 * shimmer;
    col += violet * curtain(p + float2(0.7, 0.0), 0.40, 4.3, -0.8, t) * 0.45;
    col += pink * curtain(p + float2(1.9, 0.0), 0.18, 2.2, 0.6, t) * 0.25;

    float glow = 1.0 - smoothstep(0.0, 0.9, length(float2(p.x - 0.5 * aspect, uv.y - 1.1)));
    col += violet * glow * 0.25;

    float star = step(0.9975, hash(floor(fragCoord / 2.0)));
    float twinkle = 0.5 + 0.5 * sin(iTime * 2.0 + hash(floor(fragCoord / 2.0) + 7.0) * 6.28);
    col += star * twinkle * (1.0 - uv.y);

    // Dither to hide gradient banding.
    col += (hash(fragCoord + fract(iTime)) - 0.5) * 0.012;
    return half4(half3(clamp(col, 0.0, 1.0)), 1.0);
}
"""

@Stable
class AuroraRenderer(val tilt: () -> Offset) {
    /** Seconds of animation; only advances while the launcher is resumed. */
    val time = mutableFloatStateOf(0f)

    /** Full-screen size; panels use it so their slice lines up with the background. */
    var rootSize by mutableStateOf(Size.Zero)

    private val shader: Shader? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) createAuroraShader() else null
    private val shaderBrush: ShaderBrush? = shader?.let { ShaderBrush(it) }

    /** Draws the aurora as seen through a window whose top-left is at [origin] (root coords). */
    fun draw(scope: DrawScope, origin: Offset) {
        val resolution = if (rootSize.isEmpty()) scope.size else rootSize
        val seconds = time.floatValue
        val currentTilt = tilt()
        scope.translate(-origin.x, -origin.y) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null && shaderBrush != null) {
                updateUniforms(shader, resolution, seconds, currentTilt)
                drawRect(shaderBrush, size = resolution)
            } else {
                drawFallback(resolution, seconds)
            }
        }
    }

    /** Android 12: no AGSL, so an animated gradient stands in for the shader. */
    private fun DrawScope.drawFallback(resolution: Size, seconds: Float) {
        val mixAmount = (sin(seconds * 0.3f) + 1f) / 2f
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFF04050F),
                0.35f to lerp(Color(0xFF0E6B55), Color(0xFF3B2378), mixAmount),
                0.7f to Color(0xFF0B0620),
                1f to Color(0xFF1C0F3A),
                endY = resolution.height,
            ),
            size = resolution,
        )
    }
}

/** Null if the shader fails to compile on this GPU; the gradient fallback is used instead. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createAuroraShader(): Shader? = runCatching { RuntimeShader(AURORA_AGSL) }.getOrNull()

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun updateUniforms(shader: Shader, resolution: Size, seconds: Float, tilt: Offset) {
    val runtimeShader = shader as RuntimeShader
    runtimeShader.setFloatUniform("iResolution", resolution.width, resolution.height)
    runtimeShader.setFloatUniform("iTime", seconds)
    runtimeShader.setFloatUniform("iTilt", tilt.x, tilt.y)
}

/** Full-screen aurora; also drives [AuroraRenderer.time] while the launcher is visible. */
@Composable
fun AuroraBackground(renderer: AuroraRenderer, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(renderer, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val startTime = renderer.time.floatValue
            val startNanos = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    renderer.time.floatValue = startTime + (now - startNanos) / 1_000_000_000f
                }
            }
        }
    }
    Spacer(
        modifier
            .fillMaxSize()
            .onSizeChanged { renderer.rootSize = it.toSize() }
            .drawBehind { renderer.draw(this, Offset.Zero) },
    )
}
