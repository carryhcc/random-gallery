package com.example.randomgallery.android.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import java.io.File

/**
 * 随机画廊动态视频壁纸服务
 *
 * 核心特性：
 * 1. 采用轻量化 MediaPlayer 在 Surface 上无缝静音循环播放。
 * 2. 精准生命周期节能控制：离开桌面/锁屏或息屏时立即 pause()，完全不消耗后台电量与 CPU。
 * 3. 画面采用 VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING，完美裁切填满全面屏。
 * 4. 监听壁纸更新广播，当用户在应用内设定新动态壁纸时，实时热更新重载当前播放的视频。
 */
class VideoLiveWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "VideoLiveWallpaper"
    }

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var isVisibleState = false
        private var isSurfaceCreated = false

        private val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WallpaperHelper.ACTION_LIVE_WALLPAPER_UPDATED) {
                    Log.d(TAG, "收到动态壁纸更新通知，正在重载视频...")
                    reloadVideo()
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            val filter = IntentFilter(WallpaperHelper.ACTION_LIVE_WALLPAPER_UPDATED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(updateReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(updateReceiver, filter)
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            isSurfaceCreated = true
            initAndPlay(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            isSurfaceCreated = false
            releasePlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleState = visible
            mediaPlayer?.let { player ->
                if (visible) {
                    try {
                        if (!player.isPlaying) player.start()
                    } catch (e: Exception) {
                        Log.e(TAG, "恢复播放异常", e)
                        reloadVideo()
                    }
                } else {
                    try {
                        if (player.isPlaying) player.pause()
                    } catch (e: Exception) {
                        Log.e(TAG, "暂停播放异常", e)
                    }
                }
            } ?: run {
                if (visible && isSurfaceCreated) {
                    initAndPlay(surfaceHolder)
                }
            }
        }

        private fun initAndPlay(holder: SurfaceHolder) {
            val videoFile = WallpaperHelper.getLiveWallpaperFile(this@VideoLiveWallpaperService)
            if (!videoFile.exists() || videoFile.length() <= 0) {
                Log.w(TAG, "未找到有效的动态壁纸视频文件: ${videoFile.absolutePath}")
                return
            }

            releasePlayer()

            try {
                mediaPlayer = MediaPlayer().apply {
                    setSurface(holder.surface)
                    setDataSource(videoFile.absolutePath)
                    isLooping = true
                    setVolume(0f, 0f) // 默认静音
                    setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    setOnPreparedListener { mp ->
                        if (isVisibleState) {
                            mp.start()
                        }
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer 错误: what=$what, extra=$extra")
                        false
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "初始化 MediaPlayer 失败", e)
            }
        }

        fun reloadVideo() {
            if (isSurfaceCreated) {
                initAndPlay(surfaceHolder)
            }
        }

        private fun releasePlayer() {
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) it.stop()
                    it.reset()
                    it.release()
                } catch (e: Exception) {
                    Log.e(TAG, "释放 MediaPlayer 异常", e)
                }
            }
            mediaPlayer = null
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                unregisterReceiver(updateReceiver)
            } catch (_: Exception) {}
            releasePlayer()
        }
    }
}
