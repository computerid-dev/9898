package com.echochat.cid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.echochat.cid.R
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.data.BackupManager
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.databinding.ActivityImportDataBinding
import com.echochat.cid.util.SessionManager
import kotlinx.coroutines.launch

class ImportDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImportDataBinding
    private lateinit var session: SessionManager
    private val firestoreRepository = FirestoreRepository()

    private val pickBackupFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) restoreFrom(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImportDataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonPickBackupFile.setOnClickListener { pickBackupFile.launch("application/zip") }
    }

    private fun restoreFrom(uri: Uri) {
        setLoading(true)
        val backupManager = BackupManager(this, AppDatabase.getInstance(this), session, firestoreRepository)

        lifecycleScope.launch {
            try {
                backupManager.importFromZip(uri)
                firestoreRepository.registerPresence(session.myUid, session.displayName, session.avatarBase64)
                Toast.makeText(this@ImportDataActivity, R.string.import_restore_success, Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@ImportDataActivity, MainActivity::class.java))
                finish()
            } catch (error: Exception) {
                setLoading(false)
                Toast.makeText(this@ImportDataActivity, R.string.backup_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressImport.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonPickBackupFile.isEnabled = !isLoading
    }
}
