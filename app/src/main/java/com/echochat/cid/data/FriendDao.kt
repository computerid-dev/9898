package com.echochat.cid.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Insert
    suspend fun insert(friend: Friend): Long

    @Delete
    suspend fun delete(friend: Friend)

    @Query("UPDATE friends SET isBlocked = :isBlocked WHERE friendUid = :friendUid")
    suspend fun setBlocked(friendUid: String, isBlocked: Boolean)

    @Query("SELECT * FROM friends ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<Friend>>

    @Query("SELECT * FROM friends ORDER BY addedAt DESC")
    suspend fun snapshotAll(): List<Friend>

    @Query(
        """
        SELECT f.*,
            (SELECT content FROM messages m WHERE m.chatWithUid = f.friendUid ORDER BY m.timestamp DESC LIMIT 1) AS lastMessage,
            (SELECT COUNT(*) FROM messages m2 WHERE m2.chatWithUid = f.friendUid AND m2.isMine = 0 AND m2.isRead = 0) AS unreadCount
        FROM friends f
        WHERE f.isBlocked = 0
        ORDER BY f.addedAt DESC
        """
    )
    fun observeActiveChats(): Flow<List<ChatListItem>>

    @Query("SELECT * FROM friends ORDER BY nickname ASC")
    fun observeContactsSorted(): Flow<List<Friend>>

    @Query("SELECT * FROM friends WHERE friendUid = :uid LIMIT 1")
    suspend fun findByUid(uid: String): Friend?

    @Query("SELECT * FROM friends WHERE friendUid = :uid LIMIT 1")
    fun observeByUid(uid: String): Flow<Friend?>
}
