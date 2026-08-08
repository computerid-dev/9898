package com.echochat.cid.util

import android.content.Context
import android.content.SharedPreferences

enum class WallpaperMode {
    ALL, CONTACTS_ONLY, SPECIFIC, NONE
}

/**
 * Menyimpan seluruh pengaturan lokal (identitas akun tamu, avatar, wallpaper,
 * tema, dan preferensi tampilan lain) di SharedPreferences. Tidak ada login;
 * identitas dibuat sendiri oleh tiap perangkat.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val myUid: String
        get() {
            var uid = prefs.getString(KEY_UID, null)
            if (uid == null) {
                uid = UidGenerator.generate()
                prefs.edit().putString(KEY_UID, uid).apply()
            }
            return uid
        }

    var displayName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    /** Avatar disimpan sebagai string base64 (gambar sudah dikompres kecil). */
    var avatarBase64: String?
        get() = prefs.getString(KEY_AVATAR, null)
        set(value) = prefs.edit().putString(KEY_AVATAR, value).apply()

    /** Wallpaper default untuk layar chat, disimpan sebagai content Uri string. Tidak ikut dibackup. */
    var wallpaperUri: String?
        get() = prefs.getString(KEY_WALLPAPER, null)
        set(value) = prefs.edit().putString(KEY_WALLPAPER, value).apply()

    var wallpaperMode: WallpaperMode
        get() = WallpaperMode.valueOf(prefs.getString(KEY_WALLPAPER_MODE, WallpaperMode.ALL.name)!!)
        set(value) = prefs.edit().putString(KEY_WALLPAPER_MODE, value.name).apply()

    /** UID teman yang dipilih manual untuk mode wallpaper SPECIFIC. */
    var wallpaperSpecificUids: Set<String>
        get() = prefs.getStringSet(KEY_WALLPAPER_SPECIFIC, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_WALLPAPER_SPECIFIC, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    var isUidHidden: Boolean
        get() = prefs.getBoolean(KEY_HIDE_UID, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_UID, value).apply()

    /** Penanda waktu pesan masuk terakhir yang sudah dinotifikasi, biar tidak dobel. */
    var lastNotifiedTimestamp: Long
        get() = prefs.getLong(KEY_LAST_NOTIFIED, System.currentTimeMillis())
        set(value) = prefs.edit().putLong(KEY_LAST_NOTIFIED, value).apply()

    var isBackgroundServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_BG_SERVICE, false)
        set(value) = prefs.edit().putBoolean(KEY_BG_SERVICE, value).apply()

    val hasCompletedSetup: Boolean
        get() = displayName.isNotBlank()

    /** Dipakai saat impor backup, supaya identitas UID lama bisa dipulihkan di perangkat baru. */
    fun overwriteUid(uid: String) {
        prefs.edit().putString(KEY_UID, uid).apply()
    }

    companion object {
        private const val PREFS_NAME = "echochat_session"
        private const val KEY_UID = "my_uid"
        private const val KEY_NAME = "display_name"
        private const val KEY_AVATAR = "avatar_base64"
        private const val KEY_WALLPAPER = "wallpaper_uri"
        private const val KEY_WALLPAPER_MODE = "wallpaper_mode"
        private const val KEY_WALLPAPER_SPECIFIC = "wallpaper_specific_uids"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_HIDE_UID = "hide_uid"
        private const val KEY_LAST_NOTIFIED = "last_notified_timestamp"
        private const val KEY_BG_SERVICE = "bg_service_enabled"
    }
}
