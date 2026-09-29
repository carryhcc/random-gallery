package com.example.randomgallery.android.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.randomgallery.android.util.ImageUrlResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class WallpaperTarget {
    HOME,
    LOCK,
    BOTH
}

object WallpaperHelper {

    private const val WALLPAPER_DIR = "wallpapers"
    private const val LIVE_WALLPAPER_FILE = "current_live_wallpaper.mp4"
    const val ACTION_LIVE_WALLPAPER_UPDATED = "com.example.randomgallery.android.LIVE_WALLPAPER_UPDATED"

    /**
     * 壁纸视频下载专用客户端：懒加载复用，避免每次下载都新建客户端（各自的连接池与线程池）。
     *
     * 不复用全局客户端——其 ApiNodeFailoverInterceptor 会把任意失败的 GET 改写到 API 节点，
     * 而这里下载的是 xhscdn 视频直链，改写目标错误；同时显式放宽超时以适配大文件。
     */
    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /** 获取本地存储的动态壁纸文件 */
    fun getLiveWallpaperFile(context: Context): File {
        val dir = File(context.filesDir, WALLPAPER_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, LIVE_WALLPAPER_FILE)
    }

    /**
     * 使用 Coil 异步拉取原始高清晰度 Bitmap
     */
    suspend fun loadSourceBitmap(context: Context, url: String): Bitmap? = withContext(Dispatchers.IO) {
        val resolvedUrl = ImageUrlResolver.rawUrl(url)
        // 复用应用级单例 ImageLoader（含 HEIC 解码器、磁盘缓存与代理配置）
        val imageLoader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(resolvedUrl)
            .allowHardware(false) // 需要在 CPU 内存中裁切与处理
            .build()
        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            result.drawable.toBitmap()
        } else {
            null
        }
    }

    /**
     * 将用户在屏幕视口中的交互变换（缩放、平移）映射到高分辨率位图，
     * 输出像素级精准裁切的屏幕分辨率位图，可叠加虚化滤镜。
     *
     * @param source 原图 Bitmap
     * @param viewportWidth 屏幕/视口宽度 (px)
     * @param viewportHeight 屏幕/视口高度 (px)
     * @param scale 用户双指缩放倍数 (>= 1.0)
     * @param offsetX 用户水平拖拽偏移量 (px)
     * @param offsetY 用户垂直拖拽偏移量 (px)
     * @param blurRadius 虚化半径 (0 ~ 25)
     */
    fun cropAndTransformBitmap(
        source: Bitmap,
        viewportWidth: Int,
        viewportHeight: Int,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        blurRadius: Float = 0f
    ): Bitmap {
        val targetWidth = if (viewportWidth > 0) viewportWidth else source.width
        val targetHeight = if (viewportHeight > 0) viewportHeight else source.height

        val outBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        // 1. 计算原图铺满屏幕的 baseScale (CenterCrop 规则)
        val scaleX = targetWidth.toFloat() / source.width
        val scaleY = targetHeight.toFloat() / source.height
        val baseScale = max(scaleX, scaleY)

        val totalScale = baseScale * max(scale, 1.0f)
        val scaledW = source.width * totalScale
        val scaledH = source.height * totalScale

        // 居中基础偏移 + 用户拖拽位移
        val baseTranslateX = (targetWidth - scaledW) / 2f
        val baseTranslateY = (targetHeight - scaledH) / 2f
        val finalTranslateX = baseTranslateX + offsetX
        val finalTranslateY = baseTranslateY + offsetY

        val matrix = Matrix().apply {
            postScale(totalScale, totalScale)
            postTranslate(finalTranslateX, finalTranslateY)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        // 2. 高斯模糊微调（如果用户开启了虚化）
        return if (blurRadius > 0.5f) {
            fastBlur(outBitmap, blurRadius.roundToInt().coerceIn(1, 25))
        } else {
            outBitmap
        }
    }

    /**
     * 应用静态壁纸到 Android 系统
     */
    suspend fun applyStaticWallpaper(
        context: Context,
        bitmap: Bitmap,
        target: WallpaperTarget
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val flag = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            wallpaperManager.setBitmap(bitmap, null, true, flag)
            Unit
        }
    }

    /**
     * 准备动态壁纸文件：将远程或本地视频缓存至应用专用壁纸目录
     */
    suspend fun prepareLiveWallpaper(
        context: Context,
        videoUrl: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = getLiveWallpaperFile(context)
            val resolvedUrl = ImageUrlResolver.rawUrl(videoUrl)

            if (resolvedUrl.startsWith("file://") || resolvedUrl.startsWith("/")) {
                val sourceFile = File(resolvedUrl.removePrefix("file://"))
                if (sourceFile.exists()) {
                    sourceFile.copyTo(targetFile, overwrite = true)
                    notifyWallpaperUpdated(context)
                    return@runCatching targetFile
                }
            }

            // HTTP / HTTPS 下载并落盘
            val requestBuilder = Request.Builder().url(resolvedUrl)
            if (resolvedUrl.contains("xhscdn.com") || resolvedUrl.contains("xiaohongshu.com")) {
                requestBuilder.header("Referer", "https://www.xiaohongshu.com/")
                requestBuilder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }

            downloadClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("下载视频失败: HTTP ${response.code}")
                val body = response.body ?: throw IllegalStateException("空视频数据")
                FileOutputStream(targetFile).use { fos ->
                    body.byteStream().copyTo(fos)
                }
            }

            notifyWallpaperUpdated(context)
            targetFile
        }
    }

    /**
     * 唤起系统动态壁纸选择与预览界面
     */
    fun launchLiveWallpaperChooser(context: Context): Result<Unit> = runCatching {
        val componentName = ComponentName(context, VideoLiveWallpaperService::class.java)
        
        // 首选方案：直达本应用动态壁纸预览
        val directIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, componentName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(directIntent)
        } catch (e: Exception) {
            // 次选方案：打开系统动态壁纸选择器列表
            val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(chooserIntent)
            } catch (_: Exception) {
                // 兜底方案：打开系统壁纸选择器
                val fallbackIntent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    /**
     * 通知当前运行中的 WallpaperService 重载视频
     */
    fun notifyWallpaperUpdated(context: Context) {
        val intent = Intent(ACTION_LIVE_WALLPAPER_UPDATED).apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    /**
     * 高性能轻量级 StackBlur 虚化算法（纯 Kotlin 实现，10-20ms 级耗时）
     */
    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return sentBitmap
        val w = sentBitmap.width
        val h = sentBitmap.height
        val pix = IntArray(w * h)
        sentBitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yi = 0
        yw = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + min(wm, max(i, 0))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = (p and 0x0000ff)

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - kotlin.math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                // 保留原 Alpha 通道 0xFF000000
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = min(y + r1, hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
                y++
            }
            x++
        }

        val blurred = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        blurred.setPixels(pix, 0, w, 0, 0, w, h)
        return blurred
    }
}
