package com.echochat.cid.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.echochat.cid.R
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.data.GroupMessage
import com.echochat.cid.databinding.ActivityGroupChatBinding
import com.echochat.cid.util.SessionManager
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class GroupChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var adapter: GroupMessageAdapter
    private lateinit var session: SessionManager
    private lateinit var groupId: String
    private val firestoreRepository = FirestoreRepository()
    private var messagesListener: ListenerRegistration? = null
    private val senderNameCache = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        groupId = intent.getStringExtra(EXTRA_GROUP_ID).orEmpty()
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = groupName
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = GroupMessageAdapter()
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this)
        binding.recyclerMessages.adapter = adapter

        binding.buttonSend.setOnClickListener { sendMessage() }

        observeMessages()

        lifecycleScope.launch {
            AppDatabase.getInstance(this@GroupChatActivity).groupMessageDao().markGroupAsRead(groupId)
        }
    }

    private fun observeMessages() {
        val groupMessageDao = AppDatabase.getInstance(this).groupMessageDao()
        lifecycleScope.launch {
            groupMessageDao.observeGroupChat(groupId).collect { messages ->
                adapter.submitList(messages) {
                    if (messages.isNotEmpty()) {
                        binding.recyclerMessages.scrollToPosition(messages.size - 1)
                    }
                }
                binding.textEmptyChat.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        messagesListener = firestoreRepository.listenGroupMessages(groupId) { remoteMessage ->
            lifecycleScope.launch {
                val existing = groupMessageDao.findByRemoteId(remoteMessage.remoteId)
                if (existing == null) {
                    val nickname = resolveSenderName(remoteMessage.senderUid)
                    groupMessageDao.insert(
                        GroupMessage(
                            groupId = groupId,
                            senderUid = remoteMessage.senderUid,
                            senderNickname = nickname,
                            content = remoteMessage.content,
                            isMine = remoteMessage.senderUid == session.myUid,
                            remoteId = remoteMessage.remoteId,
                            timestamp = remoteMessage.timestampMillis
                        )
                    )
                }
            }
        }
    }

    private suspend fun resolveSenderName(uid: String): String {
        if (uid == session.myUid) return session.displayName
        senderNameCache[uid]?.let { return it }
        val friendNickname = AppDatabase.getInstance(this).friendDao().findByUid(uid)?.nickname
        val name = friendNickname ?: firestoreRepository.fetchUser(uid)?.displayName ?: uid
        senderNameCache[uid] = name
        return name
    }

    private fun sendMessage() {
        val content = binding.inputMessage.text.toString().trim()
        if (content.isEmpty()) return
        firestoreRepository.sendGroupMessage(groupId, session.myUid, content)
        binding.inputMessage.text?.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_group_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuGroupInfo) {
            val intent = Intent(this, GroupDetailActivity::class.java)
            intent.putExtra(GroupDetailActivity.EXTRA_GROUP_ID, groupId)
            intent.putExtra(GroupDetailActivity.EXTRA_GROUP_NAME, supportActionBar?.title.toString())
            startActivity(intent)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
    }
}
