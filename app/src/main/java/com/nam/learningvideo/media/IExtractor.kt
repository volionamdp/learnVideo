package com.nam.learningvideo.media

import android.media.MediaFormat
import java.nio.ByteBuffer


interface IExtractor {

    fun getFormat(): MediaFormat?

    /**
     * Đọc dữ liệu âm thanh và video
     */
    fun readBuffer(byteBuffer: ByteBuffer): Int

    /**
     * Nhận khung thời gian hiện tại
     */
    fun getCurrentTimestamp(): Long

    fun getSampleFlag(): Int

    /**
     * Tìm kiếm vị trí được chỉ định và trả về dấu thời gian của khung hình thực tế
     */
    fun seek(pos: Long): Long

    fun setStartPos(pos: Long)

    /**
     * Ngừng đọc dữ liệu
     */
    fun stop()
}