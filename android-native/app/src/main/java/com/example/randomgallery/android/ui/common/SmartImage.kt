package com.example.randomgallery.android.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size

private sealed interface SmartImageDisplay {
    data object None : SmartImageDisplay
    data class Thumb(val bitmap: ImageBitmap) : SmartImageDisplay
    data class Full(val bitmap: ImageBitmap) : SmartImageDisplay
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

    LaunchedEffect(url, thumbSize, fullSize) {
        display = SmartImageDisplay.None

        // 阶段 1：缩略图
        var thumbBitmap: ImageBitmap? = null
        runCatching {
            imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(url)
                    .size(thumbSize, thumbSize)
                    .build()
            ).drawable
        }.onSuccess { drawable ->
            if (drawable != null) {
                val bitmap = drawable.toBitmap().asImageBitmap()
                thumbBitmap = bitmap
                display = SmartImageDisplay.Thumb(bitmap)
                val size = bitmap.width.toFloat() / bitmap.height.toFloat()
                if (size > 0 && !size.isNaN()) {
                    onRatioKnown(size)
                }
            }
        }.onFailure {
            // 缩略图失败不中断，继续尝试清晰图
        }

        // 阶段 2：有界清晰图
        val fullResult = runCatching {
            imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(url)
                    .apply { if (fullSize > 0) size(fullSize, fullSize) else size(Size.ORIGINAL) }
                    .build()
            ).drawable
        }.getOrNull()

        if (fullResult != null) {
            display = SmartImageDisplay.Full(fullResult.toBitmap().asImageBitmap())
            onFullLoaded()
        } else if (thumbBitmap == null) {
            onError()
        }
    }

    Crossfade(
        targetState = display,
        animationSpec = tween(200),
        label = "smart_image"
    ) { state ->
        when (state) {
            SmartImageDisplay.None -> Box(modifier)
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
