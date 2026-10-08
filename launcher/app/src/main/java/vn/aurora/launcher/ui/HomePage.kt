package vn.aurora.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.aurora.launcher.data.AppInfo
import kotlin.math.abs

const val PAGE_ROWS = 5

/** The first page gives one row to the clock. */
const val FIRST_PAGE_ROWS = 4

/** How icons look; shared by the home pages, the dock and the drawer. */
@Immutable
data class IconStyle(val size: Dp, val showLabel: Boolean)

fun paginate(apps: List<AppInfo>, columns: Int): List<List<AppInfo>> {
    val firstPageSlots = FIRST_PAGE_ROWS * columns
    val first = apps.take(firstPageSlots)
    return listOf(first) + apps.drop(firstPageSlots).chunked(PAGE_ROWS * columns)
}

@Composable
fun HomePage(
    apps: List<AppInfo>,
    showClock: Boolean,
    columns: Int,
    iconStyle: IconStyle,
    actions: AppActions,
    tilt: () -> Offset,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 8.dp)) {
        if (showClock) {
            ClockHeader(tilt, Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 8.dp))
        }
        val rows = if (showClock) FIRST_PAGE_ROWS else PAGE_ROWS
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (column in 0 until columns) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            val app = apps.getOrNull(row * columns + column)
                            if (app != null) {
                                // Lower rows sit "closer" and move more with tilt.
                                AppIcon(
                                    app = app,
                                    actions = actions,
                                    tilt = tilt,
                                    depth = 0.6f + 0.15f * row,
                                    iconSize = iconStyle.size,
                                    showLabel = iconStyle.showLabel,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Dots that stretch into a pill for the current page and morph while swiping. */
@Composable
fun PageIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    if (pagerState.pageCount <= 1) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val position = pagerState.currentPage + pagerState.currentPageOffsetFraction
        repeat(pagerState.pageCount) { page ->
            val selected = (1f - abs(position - page)).coerceIn(0f, 1f)
            Box(
                Modifier
                    .height(6.dp)
                    .width(6.dp + 14.dp * selected)
                    .background(Color.White.copy(alpha = 0.35f + 0.6f * selected), CircleShape),
            )
        }
    }
}
