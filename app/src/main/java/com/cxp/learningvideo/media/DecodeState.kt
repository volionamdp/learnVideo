package com.cxp.learningvideo.media


/**
 * 解码状态
 *
 * @author Chen Xiaoping (562818444@qq.com)
 * @since LearningVideo
 * @version LearningVideo
 * @Datetime 2019-09-02 10:00
 *
 */
enum class DecodeState {
    /** Trạng thái bắt đầu */
    START,
    /** Giải mã */
    DECODING,
    /** Tạm dừng giải mã */
    PAUSE,
    /** Chuyển tiếp nhanh */
    SEEKING,
    /** Giải mã hoàn tất */
    FINISH,
    /** Bản phát hành bộ giải mã */
    STOP
}
