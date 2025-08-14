package com.efedonmez.nothingmatrixmusicdisc.nowplaying

import android.graphics.Bitmap

data class NowPlayingInfo(
    val title: String?,
    val artist: String?,
    val album: String?,
    val isPlaying: Boolean,
    val positionMs: Long?,
    val durationMs: Long?,
    val art: Bitmap?,
    val sourcePackage: String?
)


