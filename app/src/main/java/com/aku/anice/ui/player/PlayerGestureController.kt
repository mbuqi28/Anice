package com.aku.anice.ui.player

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class PlayerGestureController(
    private val context: Context,
    private val listener: GestureListener
) : GestureDetector.SimpleOnGestureListener(), View.OnTouchListener {

    private val gestureDetector = GestureDetector(context, this)
    private var screenWidth = context.resources.displayMetrics.widthPixels
    private var isScrolling = false
    private var scrollType = ScrollType.NONE

    enum class ScrollType { NONE, BRIGHTNESS, VOLUME, SEEK }

    interface GestureListener {
        fun onBrightnessChange(delta: Float)
        fun onVolumeChange(delta: Float)
        fun onSeekChange(delta: Float)
        fun onSeekEnd()
        fun onDoubleTapLeft()
        fun onDoubleTapRight()
        fun onSingleTap()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP && isScrolling) {
            if (scrollType == ScrollType.SEEK) listener.onSeekEnd()
            isScrolling = false
            scrollType = ScrollType.NONE
        }
        return gestureDetector.onTouchEvent(event)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        listener.onSingleTap()
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (e.x < screenWidth / 2) {
            listener.onDoubleTapLeft()
        } else {
            listener.onDoubleTapRight()
        }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (e1 == null) return false
        
        if (!isScrolling) {
            isScrolling = true
            scrollType = if (abs(distanceX) > abs(distanceY)) {
                ScrollType.SEEK
            } else if (e1.x < screenWidth / 2) {
                ScrollType.BRIGHTNESS
            } else {
                ScrollType.VOLUME
            }
        }

        when (scrollType) {
            ScrollType.BRIGHTNESS -> listener.onBrightnessChange(-distanceY / context.resources.displayMetrics.heightPixels)
            ScrollType.VOLUME -> listener.onVolumeChange(-distanceY / context.resources.displayMetrics.heightPixels)
            ScrollType.SEEK -> listener.onSeekChange(-distanceX / screenWidth)
            else -> {}
        }
        
        return true
    }
}
