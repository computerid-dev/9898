package com.echochat.cid.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_messages",
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class GroupMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: String,
    val senderUid: String,
    val senderNickname: String,
    val content: String,
    val isMine: Boolean,
    val remoteId: String,
    val isRead: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
