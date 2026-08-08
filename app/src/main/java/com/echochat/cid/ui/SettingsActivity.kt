package com.echochat.cid.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.echochat.cid.databinding.ActivitySettingsBinding
import com.echochat.cid.service.NotificationListenerService
import com.echochat.cid.util.SessionManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.switchDarkMode.isChecked = session.isDarkMode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            session.isDarkMode = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.switchHideUid.isChecked = session.isUidHidden
        binding.switchHideUid.setOnCheckedChangeListener { _, isChecked ->
            session.isUidHidden = isChecked
        }

        binding.switchBackgroundService.isChecked = session.isBackgroundServiceEnabled
        binding.switchBackgroundService.setOnCheckedChangeListener { _, isChecked ->
            session.isBackgroundServiceEnabled = isChecked
            if (isChecked) {
                NotificationListenerService.start(this)
            } else {
                NotificationListenerService.stop(this)
            }
        }

        binding.buttonWallpaperSettings.setOnClickListener {
            startActivity(Intent(this, WallpaperSettingsActivity::class.java))
        }

        binding.buttonBatteryOptimization.setOnClickListener { requestIgnoreBatteryOptimization() }
    }

    private fun requestIgnoreBatteryOptimization() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !powerManager.isIgnoringBatteryOptimizations(packageName)
        ) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
}
