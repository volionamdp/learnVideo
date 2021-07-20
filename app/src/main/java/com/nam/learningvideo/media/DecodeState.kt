package com.nam.learningvideo.media


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
