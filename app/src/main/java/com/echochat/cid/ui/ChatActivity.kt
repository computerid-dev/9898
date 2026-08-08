package com.echochat.cid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.echochat.cid.R
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.data.Message
import com.echochat.cid.databinding.ActivityChatBinding
import com.echochat.cid.util.SessionManager
import com.echochat.cid.util.WallpaperMode
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: MessageAdapter
    private lateinit var session: SessionManager
    private lateinit var friendUid: String
    private val firestoreRepository = FirestoreRepository()
    private var messagesListener: ListenerRegistration? = null

    private val pickWallpaper = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) applyWallpaper(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        friendUid = intent.getStringExtra(EXTRA_FRIEND_UID).orEmpty()
        val nickname = intent.getStringExtra(EXTRA_FRIEND_NICKNAME).orEmpty()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = nickname
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = MessageAdapter()
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerMessages.adapter = adapter

        binding.buttonSend.setOnClickListener { sendMessage() }

        loadWallpaperIfSet()
        observeMessages()
        observeFriendBlockedState()
        markAsRead()
    }

    override fun onResume() {
        super.onResume()
        currentOpenChatUid = friendUid
        markAsRead()
    }

    override fun onPause() {
        super.onPause()
        if (currentOpenChatUid == friendUid) currentOpenChatUid = null
    }

    private fun markAsRead() {
        lifecycleScope.launch {
            AppDatabase.getInstance(this@ChatActivity).messageDao().markChatAsRead(friendUid)
        }
    }

    private fun observeMessages() {
        val messageDao = AppDatabase.getInstance(this).messageDao()
        lifecycleScope.launch {
            messageDao.observeChat(friendUid).collect { messages ->
                adapter.submitList(messages) {
                    if (messages.isNotEmpty()) {
                        binding.recyclerMessages.scrollToPosition(messages.size - 1)
                    }
                }
                binding.textEmptyChat.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        val chatId = firestoreRepository.chatIdFor(session.myUid, friendUid)
        messagesListener = firestoreRepository.listenMessages(chatId) { remoteMessage ->
            lifecycleScope.launch {
                val existing = messageDao.findByRemoteId(remoteMessage.remoteId)
                if (existing == null) {
                    try {
                        messageDao.insert(
                            Message(
                                chatWithUid = friendUid,
                                content = remoteMessage.content,
                                isMine = remoteMessage.senderUid == session.myUid,
                                remoteId = remoteMessage.remoteId,
                                timestamp = remoteMessage.timestampMillis
                            )
                        )
                    } catch (error: android.database.sqlite.SQLiteConstraintException) {
                        // Sudah keburu dimasukkan oleh service notifikasi latar belakang - aman diabaikan.
                    }
                }
            }
        }
    }

    private fun observeFriendBlockedState() {
        val friendDao = AppDatabase.getInstance(this).friendDao()
        lifecycleScope.launch {
            friendDao.observeByUid(friendUid).collect { friend ->
                val isBlocked = friend?.isBlocked == true
                binding.layoutInputRow.visibility = if (isBlocked) View.GONE else View.VISIBLE
                binding.textBlockedNotice.visibility = if (isBlocked) View.VISIBLE else View.GONE
            }
        }
    }

    private fun sendMessage() {
        val content = binding.inputMessage.text.toString().trim()
        if (content.isEmpty()) return

        val chatId = firestoreRepository.chatIdFor(session.myUid, friendUid)
        firestoreRepository.sendMessage(chatId, session.myUid, friendUid, content)
        binding.inputMessage.text?.clear()
    }

    private fun applyWallpaper(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (error: SecurityException) {
            // Sebagian sumber gambar tidak mendukung izin permanen; tetap lanjut memakainya.
        }
        session.wallpaperUri = uri.toString()
        loadWallpaperIfSet()
    }

    /** Wallpaper tampil sesuai mode yang diatur di Pengaturan > Wallpaper chat. */
    private fun loadWallpaperIfSet() {
        val wallpaper = session.wallpaperUri
        val shouldShow = wallpaper != null && when (session.wallpaperMode) {
            WallpaperMode.NONE -> false
            WallpaperMode.SPECIFIC -> friendUid in session.wallpaperSpecificUids
            WallpaperMode.ALL, WallpaperMode.CONTACTS_ONLY -> true
        }
        if (shouldShow && wallpaper != null) {
            binding.imageWallpaper.setImageURI(Uri.parse(wallpaper))
            binding.imageWallpaper.visibility = View.VISIBLE
        } else {
            binding.imageWallpaper.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuFriendDetail -> {
                val intent = Intent(this, FriendDetailActivity::class.java)
                intent.putExtra(FriendDetailActivity.EXTRA_FRIEND_UID, friendUid)
                startActivity(intent)
                true
            }
            R.id.menuWallpaper -> {
                pickWallpaper.launch("image/*")
                true
            }
            R.id.menuResetWallpaper -> {
                session.wallpaperUri = null
                loadWallpaperIfSet()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }

    companion object {
        const val EXTRA_FRIEND_UID = "extra_friend_uid"
        const val EXTRA_FRIEND_NICKNAME = "extra_friend_nickname"

        /** UID chat yang sedang aktif dibuka, dipakai service notifikasi biar tidak dobel notif. */
        var currentOpenChatUid: String? = null
    }
}
