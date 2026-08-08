package com.echochat.cid.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: GroupChat): Long

    @Delete
    suspend fun delete(group: GroupChat)

    @Query("SELECT * FROM groups WHERE groupId = :groupId LIMIT 1")
    suspend fun findByGroupId(groupId: String): GroupChat?

    @Query(
        """
        SELECT g.*,
            (SELECT content FROM group_messages gm WHERE gm.groupId = g.groupId ORDER BY gm.timestamp DESC LIMIT 1) AS lastMessage,
            (SELECT COUNT(*) FROM group_messages gm2 WHERE gm2.groupId = g.groupId AND gm2.isMine = 0 AND gm2.isRead = 0) AS unreadCount
        FROM groups g
        ORDER BY g.addedAt DESC
        """
    )
    fun observeAllWithPreview(): Flow<List<GroupListItem>>
}
