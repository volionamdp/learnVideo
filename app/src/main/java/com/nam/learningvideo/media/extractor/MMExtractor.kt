package com.nam.learningvideo.media.extractor

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer



class MMExtractor(path: String?) {

    /** Bộ tách âm thanh và video */
    private var mExtractor: MediaExtractor? = null

    /** Chỉ mục kênh âm thanh */
    private var mAudioTrack = -1

    /** Chỉ mục kênh video */
    private var mVideoTrack = -1

    /** Dấu thời gian của khung hiện tại */
    private var mCurSampleTime: Long = 0

    /** Cờ khung hiện tại */
    private var mCurSampleFlag: Int = 0

    /** Thời điểm bắt đầu giải mã */
    private var mStartPos: Long = 0

    init {
        mExtractor = MediaExtractor()
        mExtractor?.setDataSource(path)
    }

    /**
     * Nhận thông số định dạng video
     */
    fun getVideoFormat(): MediaFormat? {
        for (i in 0 until mExtractor!!.trackCount) {
            val mediaFormat = mExtractor!!.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("video/")) {
                mVideoTrack = i
                break
            }
        }
        return if (mVideoTrack >= 0)
            mExtractor!!.getTrackFormat(mVideoTrack)
        else null
    }

    /**
     * Nhận thông số định dạng âm thanh
     */
    fun getAudioFormat(): MediaFormat? {
        for (i in 0 until mExtractor!!.trackCount) {
            val mediaFormat = mExtractor!!.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (mime.startsWith("audio/")) {
                mAudioTrack = i
                break
            }
        }
        return if (mAudioTrack >= 0) {
            mExtractor!!.getTrackFormat(mAudioTrack)
        } else null
    }

    /**
     * Đọc dữ liệu video
     */
    fun readBuffer(byteBuffer: ByteBuffer): Int {
        byteBuffer.clear()
        selectSourceTrack()
        var readSampleCount = mExtractor!!.readSampleData(byteBuffer, 0)
        if (readSampleCount < 0) {
            return -1
        }
        // Ghi lại dấu thời gian của khung hiện tại
        mCurSampleTime = mExtractor!!.sampleTime
        mCurSampleFlag = mExtractor!!.sampleFlags
        // Chuyển đến khung tiếp theo
        mExtractor!!.advance()
        return readSampleCount
    }

    /**
     * Chọn kênh
     */
    private fun selectSourceTrack() {
        if (mVideoTrack >= 0) {
            mExtractor!!.selectTrack(mVideoTrack)
        } else if (mAudioTrack >= 0) {
            mExtractor!!.selectTrack(mAudioTrack)
        }
    }

    /**
     * Tìm kiếm vị trí được chỉ định và trả về dấu thời gian của khung hình thực tế
     */
    fun seek(pos: Long): Long {
        mExtractor!!.seekTo(pos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        return mExtractor!!.sampleTime
    }

    /**
     * Ngừng đọc dữ liệu
     */
    fun stop() {
        mExtractor?.release()
        mExtractor = null
    }

    fun getVideoTrack(): Int {
        return mVideoTrack
    }

    fun getAudioTrack(): Int {
        return mAudioTrack
    }

    fun setStartPos(pos: Long) {
        mStartPos = pos
    }

    /**
     * Nhận khung thời gian hiện tại
     */
    fun getCurrentTimestamp(): Long {
        return mCurSampleTime
    }

    fun getSampleFlag(): Int {
        return mCurSampleFlag
    }
}