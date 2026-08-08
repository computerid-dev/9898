package com.echochat.cid.data

import androidx.room.Embedded

data class GroupListItem(
    @Embedded
    val group: GroupChat,
    val lastMessage: String?,
    val unreadCount: Int
)
