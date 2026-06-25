package com.example.randomgallery.android.ui.widget

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.widget.VideoView
import kotlin.math.roundToInt

class AspectRatioVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    private var videoWidthPx = 0
    private var videoHeightPx = 0

    fun play(url: String) {
        setOnPreparedListener { player: MediaPlayer ->
            videoWidthPx = player.videoWidth
            videoHeightPx = player.videoHeight
            player.isLooping = true
            player.setVolume(0f, 0f)
            requestLayout()
            start()
        }
        setVideoURI(Uri.parse(url))
    }

    fun clear() {
        setOnPreparedListener(null)
        runCatching { stopPlayback() }
        videoWidthPx = 0
        videoHeightPx = 0
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        if (videoWidthPx > 0 && videoHeightPx > 0 && measuredWidth > 0) {
            val height = (measuredWidth.toFloat() * videoHeightPx / videoWidthPx).roundToInt()
            setMeasuredDimension(measuredWidth, height)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
