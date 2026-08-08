package com.echochat.cid.ui

import com.echochat.cid.data.ChatListItem
import com.echochat.cid.data.GroupListItem

sealed class ChatEntry {
    abstract val sortKey: Long
    abstract val unreadCount: Int

    data class Direct(val item: ChatListItem) : ChatEntry() {
        override val sortKey get() = item.friend.addedAt
        override val unreadCount get() = item.unreadCount
    }

    data class Group(val item: GroupListItem) : ChatEntry() {
        override val sortKey get() = item.group.addedAt
        override val unreadCount get() = item.unreadCount
    }
}
