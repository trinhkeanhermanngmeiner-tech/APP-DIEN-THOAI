package vn.aurora.launcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat = DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", Locale.forLanguageTag("vi"))

private val TimeBrush = Brush.linearGradient(
    listOf(Color.White, Color(0xFFB8FFE6), Color(0xFFD7C2FF)),
)

/** Big gradient clock with a glow; floats above the background when the phone tilts. */
@Composable
fun ClockHeader(tilt: () -> Offset, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            // Wake right after the next second boundary.
            delay(1_000L - now.nano / 1_000_000L)
        }
    }
    Column(
        modifier = modifier.graphicsLayer {
            val t = tilt()
            translationX = t.x * 10.dp.toPx()
            translationY = t.y * 10.dp.toPx()
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = now.format(TimeFormat),
            style = TextStyle(
                brush = TimeBrush,
                fontSize = 88.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
                shadow = Shadow(color = Color(0xFF7CF5D0).copy(alpha = 0.55f), blurRadius = 36f),
            ),
        )
        Text(
            text = now.format(DateFormat).replaceFirstChar { it.titlecase(Locale.forLanguageTag("vi")) },
            style = TextStyle(
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 17.sp,
                shadow = TextGlowShadow,
            ),
        )
    }
}
