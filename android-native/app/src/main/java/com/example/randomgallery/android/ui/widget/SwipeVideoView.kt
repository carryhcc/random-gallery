package com.example.randomgallery.android.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

class SwipeVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    override fun performClick(): Boolean = super.performClick()
}
