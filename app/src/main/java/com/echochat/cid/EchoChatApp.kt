package com.echochat.cid

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.echochat.cid.util.SessionManager

class EchoChatApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val session = SessionManager(this)
        AppCompatDelegate.setDefaultNightMode(
            if (session.isDarkMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
    }
}
