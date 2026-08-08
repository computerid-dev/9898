package com.echochat.cid.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "friends",
    indices = [Index(value = ["friendUid"], unique = true)]
)
data class Friend(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val friendUid: String,
    val nickname: String,
    val isBlocked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
