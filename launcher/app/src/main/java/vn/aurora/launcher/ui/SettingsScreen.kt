package vn.aurora.launcher.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import vn.aurora.launcher.R
import vn.aurora.launcher.data.LauncherSettings
import vn.aurora.launcher.data.MAX_ICON_SIZE_DP
import vn.aurora.launcher.data.MIN_ICON_SIZE_DP
import vn.aurora.launcher.effects.AuroraRenderer
import vn.aurora.launcher.system.SystemActions
import kotlin.math.roundToInt

private val SectionShape = RoundedCornerShape(24.dp)
private val SecondaryText = Color.White.copy(alpha = 0.65f)

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    onUpdate: ((LauncherSettings) -> LauncherSettings) -> Unit,
    renderer: AuroraRenderer,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var accessibilityOn by remember { mutableStateOf(SystemActions.isAccessibilityEnabled(context)) }
    // Re-check when coming back from the Accessibility settings page.
    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) accessibilityOn = SystemActions.isAccessibilityEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier
            .fillMaxSize()
            // Swallow touches so nothing reaches the home screen underneath.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent()
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .blur(48.dp)
                .drawBehind {
                    renderer.draw(this, Offset.Zero)
                    drawRect(Color.Black.copy(alpha = 0.45f))
                },
        )
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.done), color = Color.White, fontSize = 16.sp)
                }
            }

            Section(R.string.section_navigation) {
                SwitchRow(
                    title = R.string.hide_nav_bar,
                    description = stringResource(R.string.hide_nav_bar_desc),
                    checked = settings.hideNavBar,
                    onCheckedChange = { on -> onUpdate { it.copy(hideNavBar = on) } },
                )
                SwitchRow(
                    title = R.string.swipe_down_notifications,
                    description = stringResource(R.string.swipe_down_notifications_desc),
                    checked = settings.swipeDownNotifications,
                    onCheckedChange = { on -> onUpdate { it.copy(swipeDownNotifications = on) } },
                )
                SwitchRow(
                    title = R.string.double_tap_lock,
                    description = stringResource(
                        if (accessibilityOn) R.string.double_tap_lock_ready else R.string.double_tap_lock_desc,
                    ),
                    checked = settings.doubleTapLock,
                    onCheckedChange = { on ->
                        onUpdate { it.copy(doubleTapLock = on) }
                        if (on && !accessibilityOn) SystemActions.openAccessibilitySettings(context)
                    },
                )
                Text(
                    text = stringResource(R.string.system_navigation_tip),
                    color = SecondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }

            Section(R.string.section_effects) {
                SwitchRow(
                    title = R.string.aurora_background,
                    checked = settings.auroraBackground,
                    onCheckedChange = { on -> onUpdate { it.copy(auroraBackground = on) } },
                )
                SwitchRow(
                    title = R.string.fireflies,
                    checked = settings.fireflies,
                    onCheckedChange = { on -> onUpdate { it.copy(fireflies = on) } },
                )
                SwitchRow(
                    title = R.string.tilt_parallax,
                    checked = settings.tiltParallax,
                    onCheckedChange = { on -> onUpdate { it.copy(tiltParallax = on) } },
                )
                SwitchRow(
                    title = R.string.cube_pages,
                    checked = settings.cubePages,
                    onCheckedChange = { on -> onUpdate { it.copy(cubePages = on) } },
                )
                SwitchRow(
                    title = R.string.glass_dock,
                    checked = settings.glassDock,
                    onCheckedChange = { on -> onUpdate { it.copy(glassDock = on) } },
                )
            }

            Section(R.string.section_layout) {
                SwitchRow(
                    title = R.string.show_labels,
                    checked = settings.showLabels,
                    onCheckedChange = { on -> onUpdate { it.copy(showLabels = on) } },
                )
                ColumnsRow(
                    selected = settings.gridColumns,
                    onSelect = { columns -> onUpdate { it.copy(gridColumns = columns) } },
                )
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        text = "${stringResource(R.string.icon_size)}: ${settings.iconSizeDp} dp",
                        color = Color.White,
                        fontSize = 16.sp,
                    )
                    Slider(
                        value = settings.iconSizeDp.toFloat(),
                        onValueChange = { value -> onUpdate { it.copy(iconSizeDp = value.roundToInt()) } },
                        valueRange = MIN_ICON_SIZE_DP.toFloat()..MAX_ICON_SIZE_DP.toFloat(),
                    )
                }
            }

            Section(R.string.section_system) {
                ActionRow(R.string.default_home_settings) { SystemActions.openHomeSettings(context) }
                ActionRow(R.string.accessibility_settings) { SystemActions.openAccessibilitySettings(context) }
                ActionRow(R.string.aurora_app_info) { SystemActions.openOwnAppInfo(context) }
            }

            Text(
                text = stringResource(R.string.settings_hint),
                color = SecondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = stringResource(title),
            color = SecondaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(SectionShape)
                .background(Color.White.copy(alpha = 0.10f)),
            content = content,
        )
    }
}

@Composable
private fun SwitchRow(
    @StringRes title: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), color = Color.White, fontSize = 16.sp)
            if (description != null) {
                Text(description, color = SecondaryText, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColumnsRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.grid_columns),
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        for (columns in 4..5) {
            FilterChip(
                selected = selected == columns,
                onClick = { onSelect(columns) },
                label = { Text(columns.toString()) },
                colors = FilterChipDefaults.filterChipColors(labelColor = Color.White),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ActionRow(@StringRes title: Int, onClick: () -> Unit) {
    Text(
        text = stringResource(title),
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
