package com.example.randomgallery.android.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.randomgallery.android.R
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.Disposable
import coil.size.Size

private sealed interface SmartImageDisplay {
    data object None : SmartImageDisplay
    data class Thumb(val bitmap: ImageBitmap) : SmartImageDisplay
    data class Full(val bitmap: ImageBitmap) : SmartImageDisplay
    data object Failed : SmartImageDisplay
}

/**
 * 两阶段图片加载（主流 App 做法，如小红书/微博）：
 * 1) 先请求小尺寸缩略图 → 秒出模糊占位，并通过 onRatioKnown 立即上报真实宽高比，让布局快速稳定；
 * 2) 再请求有界清晰图 → 平滑交叉淡入替换缩略图。
 *
 * 调用方负责设置容器尺寸/宽高比与背景；本组件仅在 modifier 内绘制图片内容。
 * [fullSize] <= 0 时按原图加载（用于全屏查看）。[thumbSize] 为缩略图边长(px)。
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SmartImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    thumbSize: Int = 96,
    fullSize: Int = 0,
    blurThumb: Boolean = true,
    onRatioKnown: (Float) -> Unit = {},
    onFullLoaded: () -> Unit = {},
    onError: () -> Unit = {}
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader

    var display by remember(url) { mutableStateOf<SmartImageDisplay>(SmartImageDisplay.None) }

    DisposableEffect(url, thumbSize, fullSize) {
        display = SmartImageDisplay.None

        // 跟踪所有在途请求：组件离场或 url 变化时统一取消，避免旧图回调覆盖新图（错图/闪烁），
        // 同时释放底层 Bitmap，避免内存堆积。
        val disposables = mutableListOf<Disposable>()
        var disposed = false
        fun safeEnqueue(request: ImageRequest) {
            if (disposed) return
            disposables += imageLoader.enqueue(request)
        }

        fun enqueueFull() {
            safeEnqueue(
                ImageRequest.Builder(context)
                    .data(url)
                    .apply { if (fullSize > 0) size(fullSize, fullSize) else size(Size.ORIGINAL) }
                    .target(
                        onSuccess = { fullDrawable ->
                            if (disposed) return@target
                            display = SmartImageDisplay.Full(fullDrawable.toBitmap().asImageBitmap())
                            onFullLoaded()
                        },
                        onError = {
                            if (disposed) return@target
                            // 两阶段都失败时才置失败态；若缩略图已出图则保留它，避免把可用画面换成错误占位。
                            if (display !is SmartImageDisplay.Thumb) {
                                display = SmartImageDisplay.Failed
                            }
                            onError()
                        }
                    )
                    .build()
            )
        }

        // 阶段 1：缩略图（异步加载，避免在主线程同步解码导致卡顿/ANR）
        safeEnqueue(
            ImageRequest.Builder(context)
                .data(url)
                .size(thumbSize, thumbSize)
                .target(
                    onSuccess = { drawable ->
                        if (disposed) return@target
                        val bitmap = drawable.toBitmap().asImageBitmap()
                        display = SmartImageDisplay.Thumb(bitmap)
                        val size = bitmap.width.toFloat() / bitmap.height.toFloat()
                        if (size > 0 && !size.isNaN()) onRatioKnown(size)
                        enqueueFull()
                    },
                    onError = { enqueueFull() }
                )
                .build()
        )

        onDispose {
            disposed = true
            disposables.forEach { it.dispose() }
        }
    }

    Crossfade(
        targetState = display,
        animationSpec = tween(200),
        label = "smart_image"
    ) { state ->
        when (state) {
            SmartImageDisplay.None -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
            SmartImageDisplay.Failed -> Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                // 失败态必须与「正在加载」的空灰底可区分，否则用户无从判断该等待还是重试。
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = stringResource(R.string.common_load_failed),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp)
                )
            }
            is SmartImageDisplay.Thumb -> Image(
                bitmap = state.bitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier.let { if (blurThumb) it.blur(16.dp) else it }
            )
            is SmartImageDisplay.Full -> Image(
                bitmap = state.bitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    }
}
