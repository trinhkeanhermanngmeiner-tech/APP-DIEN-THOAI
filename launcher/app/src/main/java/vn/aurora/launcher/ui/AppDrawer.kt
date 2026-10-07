package vn.aurora.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import vn.aurora.launcher.R
import vn.aurora.launcher.data.AppInfo
import vn.aurora.launcher.data.normalizeForSearch
import vn.aurora.launcher.effects.AuroraRenderer

/**
 * Full-screen app list over a heavily blurred aurora. [progress] is 0 (closed) .. 1 (open);
 * [onDrag] takes a vertical pixel delta (positive = finger moving down = closing) and
 * [onSettle] a release velocity in px/s.
 */
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    renderer: AuroraRenderer,
    progress: () -> Float,
    onDrag: (Float) -> Unit,
    onSettle: suspend (Float) -> Unit,
    actions: AppActions,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val needle = query.normalizeForSearch().trim()
        if (needle.isEmpty()) apps else apps.filter { needle in it.searchKey }
    }
    val keyboard = LocalSoftwareKeyboardController.current
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }

    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnSettle by rememberUpdatedState(onSettle)
    val gridState = rememberLazyGridState()
    val pullToClose = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Half-closed drawer + finger moving up: reopen it before scrolling the list.
                if (source == NestedScrollSource.UserInput && available.y < 0f && progress() < 1f) {
                    currentOnDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // List is already at the top and the finger keeps pulling down: close the drawer.
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    currentOnDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (progress() < 1f) {
                    currentOnSettle(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
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
        // Backdrop stays put while the content slides, like a sheet over frosted glass.
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress() }
                .blur(48.dp)
                .drawBehind {
                    renderer.draw(this, Offset.Zero)
                    drawRect(Color.Black.copy(alpha = 0.35f))
                },
        )
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress()
                    alpha = p
                    translationY = (1f - p) * size.height * 0.15f
                }
                .systemBarsPadding()
                .imePadding(),
        ) {
            // Grab handle: dragging it closes the drawer even when the list is scrolled.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> currentOnDrag(delta) },
                        onDragStopped = { velocity -> currentOnSettle(velocity) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 5.dp)
                        .background(Color.White.copy(alpha = 0.5f), CircleShape),
                )
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = { filtered.firstOrNull()?.let { actions.launch(it, Rect.Zero) } },
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.14f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.10f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_COLUMNS),
                state = gridState,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(pullToClose),
            ) {
                itemsIndexed(filtered, key = { _, app -> app.key }) { index, app ->
                    AppIcon(
                        app = app,
                        actions = actions,
                        tilt = { Offset.Zero },
                        modifier = Modifier
                            .animateItem()
                            .graphicsLayer {
                                // Rows cascade in one after another as the drawer opens.
                                val delay = (0.2f + (index / GRID_COLUMNS) * 0.05f).coerceAtMost(0.55f)
                                val local = ((progress() - delay) / (1f - delay)).coerceIn(0f, 1f)
                                alpha = local
                                translationY = (1f - local) * 40.dp.toPx()
                                val scale = 0.9f + 0.1f * local
                                scaleX = scale
                                scaleY = scale
                            },
                    )
                }
            }
        }
    }
}
