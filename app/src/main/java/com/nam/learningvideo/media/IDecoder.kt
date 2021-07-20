package com.nam.learningvideo.media

import android.media.MediaFormat


interface IDecoder: Runnable {

    /**
     * Tạm dừng giải mã
     */
    fun pause()

    /**
     * Tiếp tục giải mã
     */
    fun goOn()

    /**
     * Chuyển đến vị trí được chỉ định
     * Và trả lại thời gian khung hình thực tế
     *
     * @param pos: mili giây
     * @return dấu thời gian thực tế, đơn vị: mili giây
     */
    fun seekTo(pos: Long): Long

    /**
     * Chuyển đến vị trí được chỉ định và chơi
     * Và trả lại thời gian khung hình thực tế
     *
     * @param pos: mili giây
     * @return dấu thời gian thực tế, đơn vị: mili giây
     */
    fun seekAndPlay(pos: Long): Long

    /**
     * Dừng giải mã
     */
    fun stop()

    /**
     * Nó đang được giải mã
     */
    fun isDecoding(): Boolean

    /**
     * Tua đi nhanh
     */
    fun isSeeking(): Boolean

    /**
     * Có dừng giải mã không
     */
    fun isStop(): Boolean
    /**
     * Đặt trình nghe kích thước
     */
    fun setSizeListener(l: IDecoderProgress)

    /**
     * Thiết lập trình nghe trạng thái
     */
    fun setStateListener(l: IDecoderStateListener?)

    /**
     * Nhận chiều rộng video
     */
    fun getWidth(): Int

    /**
     * Nhận chiều cao video
     */
    fun getHeight(): Int

    /**
     * Nhận độ dài video
     */
    fun getDuration(): Long

    /**
     * Thời gian khung hiện tại, đơn vị: ms
     */
    fun getCurTimeStamp(): Long

    /**
     * Nhận góc quay video
     */
    fun getRotationAngle(): Int

    /**
     * Nhận các thông số định dạng tương ứng với âm thanh và video
     */
    fun getMediaFormat(): MediaFormat?

    /**
     * Nhận bản nhạc phương tiện tương ứng với âm thanh và video
     */
    fun getTrack(): Int

    /**
     * Lấy đường dẫn tệp được giải mã
     */
    fun getFilePath(): String

    /**
     * Giải mã dưới dạng bộ tổng hợp
     */
    fun asCropper(): IDecoder
}