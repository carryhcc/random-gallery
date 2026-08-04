package com.example.randomgallery.android.util

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Scale
import coil.size.Size
import coil.size.pxOrElse
import com.radzivon.bartoshyk.avif.coder.HeifCoder
import com.radzivon.bartoshyk.avif.coder.PreferredColorConfig
import com.radzivon.bartoshyk.avif.coder.ScaleMode
import kotlinx.coroutines.runInterruptible
import okio.ByteString.Companion.encodeUtf8
import java.nio.ByteBuffer

/**
 * HEIC/HEIF/AVIF 解码器：系统优先 + libheif 软解兜底。
 *
 * 流程：
 * 1. 优先使用系统 [ImageDecoder]（API 28+ 原生 HEIF，性能高，硬解）；
 * 2. 系统解码抛异常（10bit/序列图/heix 等系统不支持的场景）时，
 *    自动降级为 libheif（avif-coder / HeifCoder）软件解码；
 * 3. 保证同一设备上所有合法 HEIC 都能打开，避免"有的图显示、有的不显示"的割裂问题。
 */
class HeifSystemFirstDecoder(
    private val source: SourceResult,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult? = runInterruptible {
        // 一次性读出数据，系统解码与软解兜底都基于该字节数组，避免源被重复消费
        val bytes = source.source.source().readByteArray()

        // 1) 系统硬解优先
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { decodeWithSystemDecoder(bytes) }
                .getOrNull()
                ?.let { return@runInterruptible it }
        }

        // 2) libheif 软件解码兜底
        decodeWithLibheif(bytes)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithSystemDecoder(bytes: ByteArray): DecodeResult {
        val target = options.size
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
            if (target != Size.ORIGINAL) {
                val srcW = info.size.width
                val srcH = info.size.height
                val targetW = target.width.pxOrElse { srcW }.takeIf { it > 0 } ?: srcW
                val targetH = target.height.pxOrElse { srcH }.takeIf { it > 0 } ?: srcH
                val sample = maxOf(1, maxOf(srcW / targetW, srcH / targetH))
                if (sample > 1) decoder.setTargetSampleSize(sample)
            }
        }
        return DecodeResult(
            BitmapDrawable(options.context.resources, bitmap),
            isSampled = target != Size.ORIGINAL
        )
    }

    private fun decodeWithLibheif(bytes: ByteArray): DecodeResult {
        val target = options.size
        val preferredConfig = when (options.config) {
            Bitmap.Config.RGB_565 -> PreferredColorConfig.RGB_565
            Bitmap.Config.ALPHA_8 -> PreferredColorConfig.RGBA_8888
            Bitmap.Config.ARGB_8888 -> PreferredColorConfig.RGBA_8888
            Bitmap.Config.RGBA_F16 ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) PreferredColorConfig.RGBA_F16 else PreferredColorConfig.DEFAULT
            Bitmap.Config.HARDWARE ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) PreferredColorConfig.HARDWARE else PreferredColorConfig.DEFAULT
            else -> PreferredColorConfig.DEFAULT
        }

        val coder = HeifCoder()
        val bitmap = if (target == Size.ORIGINAL) {
            coder.decode(bytes, preferredColorConfig = preferredConfig)
        } else {
            val dstWidth = target.width.pxOrElse { 0 }
            val dstHeight = target.height.pxOrElse { 0 }
            val scaleMode = if (options.scale == Scale.FILL) ScaleMode.FILL else ScaleMode.FIT
            coder.decodeSampled(
                bytes,
                dstWidth,
                dstHeight,
                preferredColorConfig = preferredConfig,
                scaleMode = scaleMode,
            )
        }
        return DecodeResult(
            BitmapDrawable(options.context.resources, bitmap),
            isSampled = target != Size.ORIGINAL
        )
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            return if (AVAILABLE_BRANDS.any { result.source.source().rangeEquals(4, it) }) {
                HeifSystemFirstDecoder(result, options)
            } else null
        }

        private companion object {
            private fun brand(ftyp: String) = "ftyp$ftyp".encodeUtf8()

            private val AVAILABLE_BRANDS = listOf(
                "heic", "heix", "hevc", "hevx", "mif1", "msf1", "avif", "avis",
            ).map(::brand)
        }
    }
}
