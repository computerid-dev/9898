package com.echochat.cid.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "groups",
    indices = [Index(value = ["groupId"], unique = true)]
)
data class GroupChat(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: String,
    val name: String,
    val avatarBase64: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
