package vn.aurora.launcher.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.aurora.launcher.R
import vn.aurora.launcher.data.AppInfo

/** What can be done with an app; [Rect] is the icon's bounds in the window, for launch animations. */
@Immutable
class AppActions(
    val launch: (AppInfo, Rect) -> Unit,
    val showInfo: (AppInfo, Rect) -> Unit,
    val uninstall: (AppInfo) -> Unit,
)

private val IconSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * App icon with a springy press, a tilt parallax ([depth] = how far it floats above the
 * background) and a long-press menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppInfo,
    actions: AppActions,
    tilt: () -> Offset,
    modifier: Modifier = Modifier,
    depth: Float = 1f,
    iconSize: Dp = 58.dp,
    showLabel: Boolean = true,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.84f else 1f, IconSpring, label = "iconScale")
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var menuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                // combinedClickable already buzzes on long press.
                onLongClick = { menuOpen = true },
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    actions.launch(app, bounds)
                },
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(iconSize)
                    .onGloballyPositioned { bounds = it.boundsInWindow() }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        val t = tilt()
                        translationX = t.x * depth * 6.dp.toPx()
                        translationY = t.y * depth * 6.dp.toPx()
                    },
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.app_info)) },
                    onClick = {
                        menuOpen = false
                        actions.showInfo(app, bounds)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.uninstall)) },
                    onClick = {
                        menuOpen = false
                        actions.uninstall(app)
                    },
                )
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    shadow = TextGlowShadow,
                ),
            )
        }
    }
}
