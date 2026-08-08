package com.echochat.cid.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.echochat.cid.databinding.ActivitySplashBinding
import com.echochat.cid.util.SessionManager

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = SessionManager(this)

        binding.root.postDelayed({
            val destination = if (session.hasCompletedSetup) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, SetupNameActivity::class.java)
            }
            startActivity(destination)
            finish()
        }, SPLASH_DELAY_MS)
    }

    companion object {
        private const val SPLASH_DELAY_MS = 900L
    }
}
