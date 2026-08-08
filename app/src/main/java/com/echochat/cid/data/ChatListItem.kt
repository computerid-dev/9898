package com.echochat.cid.data

import androidx.room.Embedded

data class ChatListItem(
    @Embedded
    val friend: Friend,
    val lastMessage: String?,
    val unreadCount: Int
)
