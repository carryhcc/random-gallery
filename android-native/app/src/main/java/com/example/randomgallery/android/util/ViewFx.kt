package com.example.randomgallery.android.util

import android.view.View
import android.view.animation.AnimationUtils
import com.example.randomgallery.android.R

fun View.playEnterAnimation() {
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.enter_fade_up))
}

fun View.startSkeletonPulse() {
    visibility = View.VISIBLE
    startAnimation(AnimationUtils.loadAnimation(context, R.anim.skeleton_pulse))
}

fun View.stopSkeletonPulse() {
    clearAnimation()
    visibility = View.GONE
}
