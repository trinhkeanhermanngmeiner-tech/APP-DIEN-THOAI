package vn.aurora.launcher.effects

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

/** 3D cube: pages rotate around their shared edge while the pager slides them. */
fun Modifier.cubeTransition(pagerState: PagerState, page: Int): Modifier = graphicsLayer {
    // Negative = page is left of the screen centre, positive = right.
    val position = (page - pagerState.currentPage - pagerState.currentPageOffsetFraction)
        .coerceIn(-1f, 1f)
    cameraDistance = 14f * density
    transformOrigin = TransformOrigin(if (position < 0f) 1f else 0f, 0.5f)
    rotationY = 90f * position
    alpha = 1f - abs(position) * 0.4f
}
