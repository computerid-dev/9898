package com.echochat.cid.data

import android.content.Context
import android.net.Uri
import com.echochat.cid.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backup/restore seluruh data lokal jadi satu file .zip:
 * - profile.json      : identitas sendiri (uid, nama, avatar base64)
 * - contacts.json      : daftar teman
 * - <nama>-<uid>-avatar.json    : avatar tiap teman (base64, diambil langsung dari Firestore)
 * - <nama>-<uid>-fullchat.json  : seluruh riwayat chat teks dengan teman itu
 * Wallpaper SENGAJA tidak ikut dibackup (bersifat lokal per perangkat).
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val session: SessionManager,
    private val firestoreRepository: FirestoreRepository
) {

    suspend fun exportToZip(destination: Uri) = withContext(Dispatchers.IO) {
        val friends = database.friendDao().snapshotAll()

        context.contentResolver.openOutputStream(destination)?.use { output ->
            ZipOutputStream(output).use { zip ->

                zip.putNextEntry(ZipEntry("profile.json"))
                zip.write(buildProfileJson().toString(2).toByteArray())
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("contacts.json"))
                zip.write(buildContactsJson(friends).toString(2).toByteArray())
                zip.closeEntry()

                for (friend in friends) {
                    val slug = slugFor(friend)

                    val avatarJson = JSONObject()
                    val remoteUser = runCatching { firestoreRepository.fetchUser(friend.friendUid) }.getOrNull()
                    avatarJson.put("friendUid", friend.friendUid)
                    avatarJson.put("nickname", friend.nickname)
                    avatarJson.put("avatarBase64", remoteUser?.avatarBase64 ?: JSONObject.NULL)
                    zip.putNextEntry(ZipEntry("$slug-avatar.json"))
                    zip.write(avatarJson.toString(2).toByteArray())
                    zip.closeEntry()

                    val messages = database.messageDao().getAllForChat(friend.friendUid)
                    val chatArray = JSONArray()
                    for (message in messages) {
                        val obj = JSONObject()
                        obj.put("content", message.content)
                        obj.put("isMine", message.isMine)
                        obj.put("remoteId", message.remoteId)
                        obj.put("timestamp", message.timestamp)
                        chatArray.put(obj)
                    }
                    val chatJson = JSONObject()
                    chatJson.put("friendUid", friend.friendUid)
                    chatJson.put("nickname", friend.nickname)
                    chatJson.put("messages", chatArray)
                    zip.putNextEntry(ZipEntry("$slug-fullchat.json"))
                    zip.write(chatJson.toString(2).toByteArray())
                    zip.closeEntry()
                }
            }
        }
    }

    suspend fun importFromZip(source: Uri) = withContext(Dispatchers.IO) {
        val friendDao = database.friendDao()
        val messageDao = database.messageDao()

        context.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                val pendingChats = mutableListOf<JSONObject>()

                while (entry != null) {
                    val name = entry.name
                    val text = zip.bufferedReader().readText()

                    when {
                        name == "profile.json" -> applyProfile(JSONObject(text))
                        name == "contacts.json" -> applyContacts(JSONObject(text), friendDao)
                        name.endsWith("-fullchat.json") -> pendingChats.add(JSONObject(text))
                        // file -avatar.json sengaja dilewati saat impor: avatar teman akan
                        // otomatis ter-refresh dari Firestore begitu chat dibuka lagi.
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }

                for (chatJson in pendingChats) {
                    applyChat(chatJson, messageDao)
                }
            }
        }
    }

    private fun buildProfileJson(): JSONObject {
        val profile = JSONObject()
        profile.put("uid", session.myUid)
        profile.put("displayName", session.displayName)
        profile.put("avatarBase64", session.avatarBase64 ?: JSONObject.NULL)
        return profile
    }

    private fun buildContactsJson(friends: List<Friend>): JSONObject {
        val array = JSONArray()
        for (friend in friends) {
            val obj = JSONObject()
            obj.put("friendUid", friend.friendUid)
            obj.put("nickname", friend.nickname)
            obj.put("isBlocked", friend.isBlocked)
            obj.put("addedAt", friend.addedAt)
            array.put(obj)
        }
        val root = JSONObject()
        root.put("friends", array)
        return root
    }

    private fun slugFor(friend: Friend): String {
        val safeName = friend.nickname.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "teman" }
        return "$safeName-${friend.friendUid}"
    }

    private fun applyProfile(profile: JSONObject) {
        val importedUid = profile.optString("uid").takeIf { it.isNotBlank() }
        val importedName = profile.optString("displayName").takeIf { it.isNotBlank() }
        val importedAvatar = if (profile.isNull("avatarBase64")) null
            else profile.optString("avatarBase64").takeIf { it.isNotBlank() }

        if (importedUid != null) session.overwriteUid(importedUid)
        if (importedName != null) session.displayName = importedName
        if (importedAvatar != null) session.avatarBase64 = importedAvatar
    }

    private suspend fun applyContacts(contacts: JSONObject, friendDao: FriendDao) {
        val array = contacts.optJSONArray("friends") ?: return
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val friendUid = obj.getString("friendUid")
            val existing = friendDao.findByUid(friendUid)
            if (existing == null) {
                friendDao.insert(
                    Friend(
                        friendUid = friendUid,
                        nickname = obj.optString("nickname", friendUid),
                        isBlocked = obj.optBoolean("isBlocked", false),
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private suspend fun applyChat(chatJson: JSONObject, messageDao: MessageDao) {
        val friendUid = chatJson.optString("friendUid").takeIf { it.isNotBlank() } ?: return
        val messages = chatJson.optJSONArray("messages") ?: return
        for (i in 0 until messages.length()) {
            val obj = messages.getJSONObject(i)
            val remoteId = obj.optString("remoteId")
            if (remoteId.isBlank()) continue
            val existing = messageDao.findByRemoteId(remoteId)
            if (existing == null) {
                messageDao.insert(
                    Message(
                        chatWithUid = friendUid,
                        content = obj.getString("content"),
                        isMine = obj.getBoolean("isMine"),
                        remoteId = remoteId,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }
    }
}
