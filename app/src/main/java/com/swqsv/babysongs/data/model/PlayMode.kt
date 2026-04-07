package com.swqsv.babysongs.data.model

/**
 * 顺序：播完最后一首后停止。
 * 列表循环：最后一首后回到第一首。
 * 单曲循环：自然播放结束后重复当前曲；上一首/下一首仍按列表切换。
 * 随机：基于洗牌队列，避免简单重复 random。
 */
enum class PlayMode {
    ORDER,
    LIST_LOOP,
    SINGLE_LOOP,
    SHUFFLE,
}
