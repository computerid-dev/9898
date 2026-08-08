package com.echochat.cid.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Insert
    suspend fun insert(message: Message): Long

    @Query("SELECT * FROM messages WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): Message?

    @Query("SELECT * FROM messages WHERE chatWithUid = :chatWithUid ORDER BY timestamp ASC")
    fun observeChat(chatWithUid: String): Flow<List<Message>>

    @Query("UPDATE messages SET isRead = 1 WHERE chatWithUid = :chatWithUid AND isRead = 0")
    suspend fun markChatAsRead(chatWithUid: String)

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllOnce(): List<Message>

    @Query("SELECT * FROM messages WHERE chatWithUid = :chatWithUid ORDER BY timestamp ASC")
    suspend fun getAllForChat(chatWithUid: String): List<Message>

    @Query("DELETE FROM messages WHERE chatWithUid = :chatWithUid")
    suspend fun deleteAllForChat(chatWithUid: String)
}
