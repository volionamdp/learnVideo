package com.nam.learningvideo.media.decoder

import android.media.*
import com.nam.learningvideo.media.BaseDecoder
import com.nam.learningvideo.media.extractor.AudioExtractor
import com.nam.learningvideo.media.IExtractor
import java.nio.ByteBuffer


class AudioDecoder(path: String): BaseDecoder(path) {
    /** Tốc độ lấy mẫu */
    private var mSampleRate = -1

    /** Số kênh âm thanh */
    private var mChannels = 1

    /** Các bit lấy mẫu PCM */
    private var mPCMEncodeBit = AudioFormat.ENCODING_PCM_16BIT

    /** Máy nghe nhạc */
    private var mAudioTrack: AudioTrack? = null

    /** Bộ nhớ đệm dữ liệu âm thanh */
    private var mAudioOutTempBuf: ShortArray? = null
    
    override fun check(): Boolean {
        return true
    }

    override fun initExtractor(path: String): IExtractor {
        return AudioExtractor(path)
    }

    override fun initSpecParams(format: MediaFormat) {
        try {
            mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

            mPCMEncodeBit = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                format.getInteger(MediaFormat.KEY_PCM_ENCODING)
            } else {
                // Nếu không có tham số này, mặc định là lấy mẫu 16 bit
                AudioFormat.ENCODING_PCM_16BIT
            }
        } catch (e: Exception) {
        }
    }

    override fun configCodec(codec: MediaCodec, format: MediaFormat): Boolean {
        codec.configure(format, null , null, 0)
        return true
    }

    override fun initRender(): Boolean {
        val channel = if (mChannels == 1) {
            //Bệnh tăng bạch cầu đơn nhân
            AudioFormat.CHANNEL_OUT_MONO
        } else {
            // Kênh kép
            AudioFormat.CHANNEL_OUT_STEREO
        }

        // Lấy bộ đệm nhỏ nhất
        val minBufferSize = AudioTrack.getMinBufferSize(mSampleRate, channel, mPCMEncodeBit)

        mAudioOutTempBuf = ShortArray(minBufferSize/2)

        mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,// Loại phát: âm nhạc
            mSampleRate, // Tỷ lệ lấy mẫu
            channel, // lối đi
            mPCMEncodeBit, // Số lượng bit lấy mẫu
            minBufferSize, // Kích thước bộ đệm
            AudioTrack.MODE_STREAM) // Chế độ phát: luồng dữ liệu được ghi động, luồng còn lại là ghi một lần

        mAudioTrack!!.play()
        return true
    }

    override fun render(outputBuffer: ByteBuffer,
                        bufferInfo: MediaCodec.BufferInfo) {
        if (mAudioOutTempBuf!!.size < bufferInfo.size / 2) {
            mAudioOutTempBuf = ShortArray(bufferInfo.size / 2)
        }
        outputBuffer.position(0)
        outputBuffer.asShortBuffer().get(mAudioOutTempBuf, 0, bufferInfo.size/2)
        mAudioTrack!!.write(mAudioOutTempBuf!!, 0, bufferInfo.size / 2)
    }

    override fun doneDecode() {
        mAudioTrack?.stop()
        mAudioTrack?.release()
    }
}