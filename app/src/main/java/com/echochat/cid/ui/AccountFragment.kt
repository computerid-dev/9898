package com.echochat.cid.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.echochat.cid.R
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.data.BackupManager
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.databinding.FragmentAccountBinding
import com.echochat.cid.util.ImageUtils
import com.echochat.cid.util.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private val firestoreRepository = FirestoreRepository()

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) applyNewAvatar(uri)
    }

    private val createBackupFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) exportBackupTo(uri)
    }

    private val openBackupFile = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) importBackupFrom(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        refreshProfileUi()

        binding.buttonCopyUid.setOnClickListener { copyUidToClipboard() }
        binding.buttonChangeAvatar.setOnClickListener { pickAvatar.launch("image/*") }
        binding.imageAvatar.setOnClickListener { pickAvatar.launch("image/*") }

        binding.buttonSaveName.setOnClickListener { saveName() }

        binding.buttonExportBackup.setOnClickListener {
            val fileName = "echochat-backup-${timestampForFileName()}.zip"
            createBackupFile.launch(fileName)
        }
        binding.buttonImportBackup.setOnClickListener {
            openBackupFile.launch("application/zip")
        }
    }

    private fun refreshProfileUi() {
        binding.textMyUid.text = session.myUid
        binding.inputDisplayName.setText(session.displayName)
        val avatar = session.avatarBase64
        if (avatar != null) {
            val bitmap = ImageUtils.base64ToBitmap(avatar)
            if (bitmap != null) binding.imageAvatar.setImageBitmap(bitmap)
        }
    }

    private fun applyNewAvatar(uri: Uri) {
        val base64 = ImageUtils.uriToCompressedBase64(requireContext(), uri)
        if (base64 == null) {
            Toast.makeText(requireContext(), R.string.backup_failed, Toast.LENGTH_SHORT).show()
            return
        }
        session.avatarBase64 = base64
        binding.imageAvatar.setImageBitmap(ImageUtils.base64ToBitmap(base64))
        firestoreRepository.registerPresence(session.myUid, session.displayName, base64)
    }

    private fun saveName() {
        val name = binding.inputDisplayName.text.toString().trim()
        if (name.isEmpty()) {
            binding.inputDisplayName.error = getString(R.string.error_name_empty)
            return
        }
        session.displayName = name
        firestoreRepository.registerPresence(session.myUid, name, session.avatarBase64)
        Toast.makeText(requireContext(), R.string.name_saved, Toast.LENGTH_SHORT).show()
    }

    private fun copyUidToClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("EchoChat UID", session.myUid))
        Toast.makeText(requireContext(), R.string.id_copied, Toast.LENGTH_SHORT).show()
    }

    private fun exportBackupTo(uri: Uri) {
        val backupManager = BackupManager(
            requireContext(), AppDatabase.getInstance(requireContext()), session, firestoreRepository
        )
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                backupManager.exportToZip(uri)
                Toast.makeText(requireContext(), R.string.backup_export_success, Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                Toast.makeText(requireContext(), R.string.backup_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importBackupFrom(uri: Uri) {
        val backupManager = BackupManager(
            requireContext(), AppDatabase.getInstance(requireContext()), session, firestoreRepository
        )
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                backupManager.importFromZip(uri)
                refreshProfileUi()
                firestoreRepository.registerPresence(session.myUid, session.displayName, session.avatarBase64)
                Toast.makeText(requireContext(), R.string.backup_import_success, Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                Toast.makeText(requireContext(), R.string.backup_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun timestampForFileName(): String {
        return SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(java.util.Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
