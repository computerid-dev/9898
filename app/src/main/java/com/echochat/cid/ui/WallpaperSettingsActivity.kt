package com.echochat.cid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.databinding.ActivityWallpaperSettingsBinding
import com.echochat.cid.util.SessionManager
import com.echochat.cid.util.WallpaperMode
import kotlinx.coroutines.launch

class WallpaperSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWallpaperSettingsBinding
    private lateinit var session: SessionManager

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) applyImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWallpaperSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        session.wallpaperUri?.let { binding.imagePreview.setImageURI(Uri.parse(it)) }

        when (session.wallpaperMode) {
            WallpaperMode.ALL -> binding.radioAll.isChecked = true
            WallpaperMode.CONTACTS_ONLY -> binding.radioContactsOnly.isChecked = true
            WallpaperMode.SPECIFIC -> binding.radioSpecific.isChecked = true
            WallpaperMode.NONE -> binding.radioNone.isChecked = true
        }
        updateContactsButtonVisibility()

        binding.buttonPickImage.setOnClickListener { pickImage.launch("image/*") }
        binding.buttonResetWallpaper.setOnClickListener {
            session.wallpaperUri = null
            binding.imagePreview.setImageDrawable(null)
        }

        binding.radioGroupMode.setOnCheckedChangeListener { _, _ ->
            session.wallpaperMode = when (binding.radioGroupMode.checkedRadioButtonId) {
                binding.radioAll.id -> WallpaperMode.ALL
                binding.radioContactsOnly.id -> WallpaperMode.CONTACTS_ONLY
                binding.radioSpecific.id -> WallpaperMode.SPECIFIC
                else -> WallpaperMode.NONE
            }
            updateContactsButtonVisibility()
        }

        binding.buttonChooseContacts.setOnClickListener { showContactPicker() }
    }

    private fun applyImage(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (error: SecurityException) {
            // Sebagian sumber gambar tidak mendukung izin permanen; tetap lanjut memakainya.
        }
        session.wallpaperUri = uri.toString()
        binding.imagePreview.setImageURI(uri)
    }

    private fun updateContactsButtonVisibility() {
        binding.buttonChooseContacts.visibility =
            if (session.wallpaperMode == WallpaperMode.SPECIFIC) View.VISIBLE else View.GONE
    }

    private fun showContactPicker() {
        lifecycleScope.launch {
            val friends = AppDatabase.getInstance(this@WallpaperSettingsActivity).friendDao().snapshotAll()
            if (friends.isEmpty()) return@launch

            val names = friends.map { it.nickname }.toTypedArray()
            val selected = friends.map { it.friendUid in session.wallpaperSpecificUids }.toBooleanArray()

            AlertDialog.Builder(this@WallpaperSettingsActivity)
                .setTitle(com.echochat.cid.R.string.wallpaper_choose_contacts)
                .setMultiChoiceItems(names, selected) { _, which, isChecked -> selected[which] = isChecked }
                .setPositiveButton(com.echochat.cid.R.string.action_save) { _, _ ->
                    val chosenUids = friends.filterIndexed { index, _ -> selected[index] }
                        .map { it.friendUid }.toSet()
                    session.wallpaperSpecificUids = chosenUids
                }
                .setNegativeButton(com.echochat.cid.R.string.action_cancel, null)
                .show()
        }
    }
}
