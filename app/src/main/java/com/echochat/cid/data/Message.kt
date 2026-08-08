package com.echochat.cid.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatWithUid"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatWithUid: String,
    val content: String,
    val isMine: Boolean,
    val remoteId: String,
    val isRead: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
