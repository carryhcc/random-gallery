package com.example.randomgallery.android.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

/**
 * 具有双击放大/复原、双指捏合缩放（Pinch-to-zoom）、边界阻尼平移和弹性回弹的图片手势容器。
 * 当处于放大状态（scale > 1f）时，消费滑动事件以支持自由拖动平移；
 * 当处于 1x 原始大小时，不拦截单指水平滑动，确保父级 HorizontalPager 左右翻页极致顺畅。
 *
 * @param modifier 外部修饰符
 * @param minScale 最小允许缩放比例（回弹基准为 1f）
 * @param maxScale 最大缩放比例
 * @param doubleTapScale 双击放大的目标倍数
 * @param onSingleTap 单击回调（例如关闭预览或显示/隐藏控制栏）
 * @param content 待缩放展示的 Composable 内容（如 AsyncImage）
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    minScale: Float = 1f,
    maxScale: Float = 4f,
    doubleTapScale: Float = 2.5f,
    onSingleTap: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun maxOffsetX(currentScale: Float): Float =
        ((containerSize.width * (currentScale - 1f)) / 2f).coerceAtLeast(0f)

    fun maxOffsetY(currentScale: Float): Float =
        ((containerSize.height * (currentScale - 1f)) / 2f).coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSingleTap?.invoke() },
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            val targetScale = if (scale > 1.05f) 1f else doubleTapScale
                            if (targetScale == 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                val center = Offset(containerSize.width / 2f, containerSize.height / 2f)
                                val diff = center - tapOffset
                                val maxBoundX = maxOffsetX(targetScale)
                                val maxBoundY = maxOffsetY(targetScale)
                                val targetOffsetX = (diff.x * (targetScale - 1f)).coerceIn(-maxBoundX, maxBoundX)
                                val targetOffsetY = (diff.y * (targetScale - 1f)).coerceIn(-maxBoundY, maxBoundY)

                                scale = targetScale
                                offset = Offset(targetOffsetX, targetOffsetY)
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes
                        val pointerCount = pointers.count { it.pressed }

                        if (pointerCount >= 2) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            val newScale = (scale * zoomChange).coerceIn(0.75f, maxScale * 1.5f)
                            scale = newScale

                            val maxBoundX = maxOffsetX(newScale)
                            val maxBoundY = maxOffsetY(newScale)

                            offset = Offset(
                                x = (offset.x + panChange.x).coerceIn(-maxBoundX * 1.2f, maxBoundX * 1.2f),
                                y = (offset.y + panChange.y).coerceIn(-maxBoundY * 1.2f, maxBoundY * 1.2f)
                            )

                            pointers.forEach { it.consume() }
                        } else if (pointerCount == 1 && scale > 1.02f) {
                            val panChange = event.calculatePan()
                            val maxBoundX = maxOffsetX(scale)
                            val maxBoundY = maxOffsetY(scale)

                            if (panChange != Offset.Zero) {
                                offset = Offset(
                                    x = (offset.x + panChange.x).coerceIn(-maxBoundX, maxBoundX),
                                    y = (offset.y + panChange.y).coerceIn(-maxBoundY, maxBoundY)
                                )
                                pointers.forEach {
                                    if (it.positionChanged()) it.consume()
                                }
                            }
                        }
                    } while (pointers.any { it.pressed })

                    // 手指离开屏幕后，若处于过小、过大或越界偏移状态，弹性平滑回弹到安全范围
                    coroutineScope.launch {
                        val finalScale = scale.coerceIn(minScale, maxScale)
                        val maxBoundX = maxOffsetX(finalScale)
                        val maxBoundY = maxOffsetY(finalScale)
                        val finalOffset = if (finalScale <= 1.02f) {
                            Offset.Zero
                        } else {
                            Offset(
                                x = offset.x.coerceIn(-maxBoundX, maxBoundX),
                                y = offset.y.coerceIn(-maxBoundY, maxBoundY)
                            )
                        }

                        if (scale != finalScale || offset != finalOffset) {
                            val scaleAnim = Animatable(scale)
                            val offsetAnim = Animatable(offset, Offset.VectorConverter)
                            launch {
                                scaleAnim.animateTo(finalScale, spring()) {
                                    scale = value
                                }
                            }
                            launch {
                                offsetAnim.animateTo(finalOffset, spring()) {
                                    offset = value
                                }
                            }
                        }
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
    ) {
        content()
    }
}
