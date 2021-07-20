package com.nam.learningvideo.media

interface IDecoderProgress {
    /**
     * Gọi lại chiều rộng và chiều cao video
     */
    fun videoSizeChange(width: Int, height: Int, rotationAngle: Int)

    /**
     * Gọi lại tiến trình phát lại video
     */
    fun videoProgressChange(pos: Long)
}