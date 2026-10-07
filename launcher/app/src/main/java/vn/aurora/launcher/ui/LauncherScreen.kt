package vn.aurora.launcher.ui

import android.app.ActivityOptions
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.aurora.launcher.LauncherViewModel
import vn.aurora.launcher.R
import vn.aurora.launcher.effects.AuroraBackground
import vn.aurora.launcher.effects.AuroraRenderer
import vn.aurora.launcher.effects.Fireflies
import vn.aurora.launcher.effects.GlassPanel
import vn.aurora.launcher.effects.cubeTransition
import vn.aurora.launcher.effects.rememberTilt
import kotlin.math.roundToInt

private val DrawerSpring = spring<Float>(dampingRatio = 0.82f, stiffness = 300f)

/** Release speed (px/s) above which a flick opens/closes the drawer regardless of position. */
private const val FLING_VELOCITY = 1200f

@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val dock by viewModel.dock.collectAsStateWithLifecycle()

    val tiltState = rememberTilt()
    val tilt = remember(tiltState) { { tiltState.value } }
    val renderer = remember(tilt) { AuroraRenderer(tilt) }

    val scope = rememberCoroutineScope()
    val drawer = remember { Animatable(0f) }
    val drawerVisible by remember { derivedStateOf { drawer.value > 0.001f } }

    val homeApps = remember(apps, dock) {
        val dockKeys = dock.mapTo(HashSet()) { it.key }
        apps.filterNot { it.key in dockKeys }
    }
    val pages = remember(homeApps) { paginate(homeApps) }
    val pagerState = rememberPagerState { pages.size }

    val context = LocalContext.current
    val view = LocalView.current
    val launchFailed = stringResource(R.string.launch_failed)
    val actions = remember(viewModel, view, launchFailed) {
        AppActions(
            launch = { app, bounds ->
                val ok = viewModel.launch(app, bounds.toAndroidRect(), scaleUpOptions(view, bounds))
                if (!ok) Toast.makeText(context, launchFailed, Toast.LENGTH_SHORT).show()
                // Reset the drawer behind the opening app so Home lands on the home screen.
                scope.launch {
                    delay(450)
                    drawer.snapTo(0f)
                }
            },
            showInfo = { app, bounds -> viewModel.openAppInfo(app, bounds.toAndroidRect()) },
            uninstall = { app -> viewModel.uninstall(app) },
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.homePresses.collect {
            if (drawer.value > 0f) drawer.animateTo(0f, DrawerSpring) else pagerState.animateScrollToPage(0)
        }
    }
    BackHandler(enabled = drawerVisible) {
        scope.launch { drawer.animateTo(0f, DrawerSpring) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val onDrag: (Float) -> Unit = { delta ->
            scope.launch { drawer.snapTo((drawer.value - delta / heightPx).coerceIn(0f, 1f)) }
        }
        val onSettle: suspend (Float) -> Unit = { velocity ->
            val target = when {
                velocity < -FLING_VELOCITY -> 1f
                velocity > FLING_VELOCITY -> 0f
                drawer.value > 0.35f -> 1f
                else -> 0f
            }
            drawer.animateTo(target, DrawerSpring, initialVelocity = -velocity / heightPx)
        }

        AuroraBackground(renderer)
        Fireflies(time = { renderer.time.floatValue }, tilt = tilt)

        // Home layer: shrinks, fades and blurs away as the drawer comes up.
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = drawer.value
                    val scale = 1f - 0.08f * p
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - p
                    val blurPx = 24.dp.toPx() * p
                    renderEffect = if (blurPx > 0.5f) BlurEffect(blurPx, blurPx, TileMode.Decal) else null
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState(onDrag),
                    onDragStopped = { velocity -> onSettle(velocity) },
                )
                .systemBarsPadding(),
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f),
            ) { page ->
                HomePage(
                    apps = pages.getOrElse(page) { emptyList() },
                    showClock = page == 0,
                    actions = actions,
                    tilt = tilt,
                    modifier = Modifier
                        .fillMaxSize()
                        .cubeTransition(pagerState, page),
                )
            }
            PageIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 10.dp),
            )
            GlassPanel(
                renderer = renderer,
                shape = RoundedCornerShape(34.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    dock.forEach { app ->
                        AppIcon(app, actions, tilt, depth = 1.4f, showLabel = false)
                    }
                }
            }
        }

        if (drawerVisible) {
            AppDrawer(
                apps = apps,
                renderer = renderer,
                progress = { drawer.value },
                onDrag = onDrag,
                onSettle = onSettle,
                actions = actions,
            )
        }
    }
}

private fun Rect.toAndroidRect() = android.graphics.Rect(
    left.roundToInt(),
    top.roundToInt(),
    right.roundToInt(),
    bottom.roundToInt(),
)

/** The opening app grows out of the tapped icon. */
private fun scaleUpOptions(view: View, bounds: Rect) =
    if (bounds.isEmpty) {
        null
    } else {
        ActivityOptions.makeScaleUpAnimation(
            view,
            bounds.left.roundToInt(),
            bounds.top.roundToInt(),
            bounds.width.roundToInt(),
            bounds.height.roundToInt(),
        ).toBundle()
    }
