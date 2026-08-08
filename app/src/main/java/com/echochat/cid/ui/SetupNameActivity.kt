package com.echochat.cid.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.echochat.cid.databinding.ActivitySetupNameBinding
import com.echochat.cid.util.SessionManager

class SetupNameActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupNameBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupNameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)

        binding.textImportData.setOnClickListener {
            startActivity(Intent(this, ImportDataActivity::class.java))
        }

        binding.buttonContinue.setOnClickListener {
            val name = binding.inputDisplayName.text.toString().trim()
            if (name.isEmpty()) {
                binding.inputDisplayName.error = getString(com.echochat.cid.R.string.error_name_empty)
                return@setOnClickListener
            }
            session.displayName = name
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
