package com.nam.learningvideo.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

abstract class BaseDecoder(private val mFilePath: String) : IDecoder {

    private val TAG = "BaseDecoder"

    // ------------- Chủ đề liên quan ------------------------
    /**
     * Bộ giải mã có đang chạy không
     */
    private var mIsRunning = true

    /**
     * Chủ đề chờ khóa
     */
    private val mLock = Object()

    /**
     * Có thể nhập giải mã không
     */
    private var mReadyForDecode = false

    // --------------- Trạng thái liên quan -----------------------
    /**
     * Bộ giải mã âm thanh và video
     */
    private var mCodec: MediaCodec? = null

    /**
     * Đầu đọc dữ liệu âm thanh và video
     */
    private var mExtractor: IExtractor? = null
    /**
     * Giải mã bộ đệm đầu vào
     */
    private var mInputBuffers: Array<ByteBuffer>? = null

    /**
     * Giải mã bộ đệm đầu ra
     */
    private var mOutputBuffers: Array<ByteBuffer>? = null

    /**
     * Giải mã thông tin dữ liệu
     */
    private var mBufferInfo = MediaCodec.BufferInfo()

    private var mState = DecodeState.STOP

    protected var mStateListener: IDecoderStateListener? = null

    /**
     * Dữ liệu luồng có kết thúc hay không
     */
    private var mIsEOS = false

    protected var mVideoWidth = 0

    protected var mVideoHeight = 0

    private var mDuration: Long = 0

    private var mStartPos: Long = 0

    private var mEndPos: Long = 0

    /**
     * Thời gian bắt đầu giải mã, được sử dụng để đồng bộ hóa âm thanh và video
     */
    private var mStartTimeForSync = -1L

    final override fun run() {
        if (mState == DecodeState.STOP) {
            mState = DecodeState.START
        }
        mStateListener?.decoderPrepare(this)

        // [Các bước giải mã: 1. Khởi tạo và khởi động bộ giải mã]
        if (!init()) return

        Log.i(TAG, "Bắt đầu giải mã")

        while (mIsRunning) {
            if (mState != DecodeState.START &&
                mState != DecodeState.DECODING &&
                mState != DecodeState.SEEKING
            ) {
                Log.i(TAG, "Vào chờ：$mState")

                waitDecode()
                // --------- [Chỉnh sửa thời gian đồng bộ hóa] -------------
                // Thời gian bắt đầu tiếp tục đồng bộ hóa, tức là loại bỏ thời gian chờ bị mất
                mStartTimeForSync = System.currentTimeMillis() - getCurTimeStamp()
            }

            if (!mIsRunning ||
                mState == DecodeState.STOP
            ) {
                mIsRunning = false
                break
            }

            if (mStartTimeForSync == -1L) {
                mStartTimeForSync = System.currentTimeMillis()
            }

            // Nếu dữ liệu chưa được giải mã, đẩy dữ liệu vào bộ giải mã để giải mã
            if (!mIsEOS) {
                // [Các bước giải mã: 2. Xem dữ liệu được nhấn vào bộ đệm đầu vào của bộ giải mã]
                mIsEOS = pushBufferToDecoder()
            }

            // [Bước giải mã: 3. Kéo dữ liệu đã giải mã ra khỏi bộ đệm]
            val index = pullBufferFromDecoder()
            if (index >= 0) {
                // --------- 【đồng bộ âm thanh và video】 -------------
                if (mState == DecodeState.DECODING) {
                    sleepRender()
                }
                // [Bước giải mã: 4. Kết xuất]
                render(mOutputBuffers!![index], mBufferInfo)
                // [Bước giải mã: 5. Giải phóng bộ đệm đầu ra]
                mCodec!!.releaseOutputBuffer(index, true)
                if (mState == DecodeState.START) {
                    mState = DecodeState.PAUSE
                }
            }
            // 【Bước giải mã: 6. Xác định xem quá trình giải mã đã hoàn tất chưa】
            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                Log.i(TAG, "Kết thúc giải mã")
                mState = DecodeState.FINISH
                mStateListener?.decoderFinish(this)
            }
        }
        doneDecode()
        release()
    }

    private fun init(): Boolean {
        if (mFilePath.isEmpty() || !File(mFilePath).exists()) {
            Log.w(TAG, "Đường dẫn tệp trống")
            mStateListener?.decoderError(this, "Đường dẫn tệp trống")
            return false
        }

        if (!check()) return false

        // Khởi tạo trình trích xuất dữ liệu
        mExtractor = initExtractor(mFilePath)
        if (mExtractor == null ||
            mExtractor!!.getFormat() == null
        ) {
            Log.w(TAG, "Không thể phân tích cú pháp tệp")
            return false
        }

        // Tham số khởi tạo
        if (!initParams()) return false

        // Khởi tạo trình kết xuất
        if (!initRender()) return false

        // Khởi tạo bộ giải mã
        if (!initCodec()) return false
        return true
    }

    private fun initParams(): Boolean {
        try {
            val format = mExtractor!!.getFormat()!!
            mDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000
            if (mEndPos == 0L) mEndPos = mDuration

            initSpecParams(mExtractor!!.getFormat()!!)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun initCodec(): Boolean {
        try {
            val type = mExtractor!!.getFormat()!!.getString(MediaFormat.KEY_MIME)
            mCodec = MediaCodec.createDecoderByType(type)
            if (!configCodec(mCodec!!, mExtractor!!.getFormat()!!)) {
                waitDecode()
            }
            mCodec!!.start()

            mInputBuffers = mCodec?.inputBuffers
            mOutputBuffers = mCodec?.outputBuffers
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun pushBufferToDecoder(): Boolean {
        var inputBufferIndex = mCodec!!.dequeueInputBuffer(2000)
        var isEndOfStream = false

        if (inputBufferIndex >= 0) {
            val inputBuffer = mInputBuffers!![inputBufferIndex]
            val sampleSize = mExtractor!!.readBuffer(inputBuffer)

            if (sampleSize < 0) {
                // Nếu dữ liệu đã được tìm nạp, hãy nhấn vào cờ kết thúc dữ liệu: MediaCodec.BUFFER_FLAG_END_OF_STREAM
                mCodec!!.queueInputBuffer(
                    inputBufferIndex, 0, 0,
                    0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                isEndOfStream = true
            } else {
                mCodec!!.queueInputBuffer(
                    inputBufferIndex, 0,
                    sampleSize, mExtractor!!.getCurrentTimestamp(), 0
                )
            }
        }
        return isEndOfStream
    }

    private fun pullBufferFromDecoder(): Int {
        // Truy vấn xem có dữ liệu được giải mã hay không, khi chỉ mục> = 0, điều đó có nghĩa là dữ liệu hợp lệ và chỉ mục là chỉ mục bộ đệm
        var index = mCodec!!.dequeueOutputBuffer(mBufferInfo, 1000)
        when (index) {
            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
            }
            MediaCodec.INFO_TRY_AGAIN_LATER -> {
            }
            MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                mOutputBuffers = mCodec!!.outputBuffers
            }
            else -> {
                return index
            }
        }
        return -1
    }

    private fun sleepRender() {
        val passTime = System.currentTimeMillis() - mStartTimeForSync
        val curTime = getCurTimeStamp()
        if (curTime > passTime) {
            Thread.sleep(curTime - passTime)
        }
    }

    private fun release() {
        try {
            mState = DecodeState.STOP
            mIsEOS = false
            mExtractor?.stop()
            mCodec?.stop()
            mCodec?.release()
            mStateListener?.decoderDestroy(this)
        } catch (e: Exception) {
        }
    }

    /**
     * Chuỗi giải mã đang chờ
     **/
    private fun waitDecode() {
        try {
            if (mState == DecodeState.PAUSE) {
                mStateListener?.decoderPause(this)
            }
            synchronized(mLock) {
                mLock.wait()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     *Thông báo cho chuỗi giải mã tiếp tục chạy
     */
    protected fun notifyDecode() {
        synchronized(mLock) {
            mLock.notifyAll()
        }
        if (mState == DecodeState.DECODING) {
            mStateListener?.decoderRunning(this)
        }
    }

    override fun pause() {
        mState = DecodeState.DECODING
    }

    override fun goOn() {
        mState = DecodeState.DECODING
        notifyDecode()
    }

    override fun seekTo(pos: Long): Long {
        return 0
    }

    override fun seekAndPlay(pos: Long): Long {
        return 0
    }

    override fun stop() {

    }

    override fun isDecoding(): Boolean {
        return mState == DecodeState.DECODING
    }

    override fun isSeeking(): Boolean {
        return mState == DecodeState.SEEKING
    }

    override fun isStop(): Boolean {
        return mState == DecodeState.STOP
    }

    override fun setSizeListener(l: IDecoderProgress) {
    }

    override fun setStateListener(l: IDecoderStateListener?) {
        mStateListener = l
    }

    override fun getWidth(): Int {
        return mVideoWidth
    }

    override fun getHeight(): Int {
        return mVideoHeight
    }

    override fun getDuration(): Long {
        return mDuration
    }

    override fun getCurTimeStamp(): Long {
        return mBufferInfo.presentationTimeUs / 1000
    }

    override fun getRotationAngle(): Int {
        return 0
    }

    override fun getMediaFormat(): MediaFormat? {
        return mExtractor?.getFormat()
    }

    override fun getTrack(): Int {
        return 0
    }

    override fun getFilePath(): String {
        return mFilePath
    }

    override fun asCropper(): IDecoder {
        return this
    }

    /**
     * Kiểm tra các thông số của lớp con
     */
    abstract fun check(): Boolean

    /**
     * Khởi tạo trình trích xuất dữ liệu
     */
    abstract fun initExtractor(path: String): IExtractor

    /**
     * Khởi tạo các tham số duy nhất của lớp con
     */
    abstract fun initSpecParams(format: MediaFormat)

    /**
     * Định cấu hình bộ giải mã
     */
    abstract fun configCodec(codec: MediaCodec, format: MediaFormat): Boolean

    /**
     * Khởi tạo trình kết xuất
     */
    abstract fun initRender(): Boolean

    /**
     * Kết xuất
     */
    abstract fun render(
        outputBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    )

    /**
     * Kết thúc giải mã
     */
    abstract fun doneDecode()
}