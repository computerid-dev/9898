package com.echochat.cid.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMessageDao {

    @Insert
    suspend fun insert(message: GroupMessage): Long

    @Query("SELECT * FROM group_messages WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: String): GroupMessage?

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun observeGroupChat(groupId: String): Flow<List<GroupMessage>>

    @Query("UPDATE group_messages SET isRead = 1 WHERE groupId = :groupId AND isRead = 0")
    suspend fun markGroupAsRead(groupId: String)

    @Query("DELETE FROM group_messages WHERE groupId = :groupId")
    suspend fun deleteAllForGroup(groupId: String)
}
