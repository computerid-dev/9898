package com.echochat.cid.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.echochat.cid.R
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.databinding.ActivityGroupDetailBinding
import com.echochat.cid.util.SessionManager
import kotlinx.coroutines.launch

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupDetailBinding
    private lateinit var session: SessionManager
    private lateinit var groupId: String
    private val firestoreRepository = FirestoreRepository()
    private lateinit var adapter: GroupMemberAdapter
    private var iAmAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        groupId = intent.getStringExtra(EXTRA_GROUP_ID).orEmpty()
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME).orEmpty()

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.textGroupName.text = groupName

        binding.buttonAddMember.setOnClickListener { showAddMemberDialog() }

        refreshMembers()
    }

    private fun refreshMembers() {
        lifecycleScope.launch {
            val group = firestoreRepository.fetchGroup(groupId) ?: run {
                finish()
                return@launch
            }
            iAmAdmin = session.myUid in group.admins
            binding.buttonAddMember.visibility = if (iAmAdmin) View.VISIBLE else View.GONE

            val friendDao = AppDatabase.getInstance(this@GroupDetailActivity).friendDao()
            val members = group.members.map { uid ->
                val nickname = if (uid == session.myUid) {
                    session.displayName
                } else {
                    friendDao.findByUid(uid)?.nickname ?: uid
                }
                GroupMemberUiModel(
                    uid = uid,
                    displayName = nickname,
                    isAdmin = uid in group.admins,
                    isOwner = uid == group.ownerUid,
                    isMe = uid == session.myUid
                )
            }

            adapter = GroupMemberAdapter(canManage = iAmAdmin) { member, anchor ->
                showMemberActionsMenu(member, anchor)
            }
            binding.recyclerMembers.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@GroupDetailActivity)
            binding.recyclerMembers.adapter = adapter
            adapter.submitList(members)
        }
    }

    private fun showMemberActionsMenu(member: GroupMemberUiModel, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, getString(if (member.isAdmin) R.string.action_remove_admin else R.string.action_make_admin))
        popup.menu.add(0, 2, 1, getString(R.string.action_kick_member))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    if (member.isAdmin) {
                        firestoreRepository.demoteFromAdmin(groupId, member.uid)
                    } else {
                        firestoreRepository.promoteToAdmin(groupId, member.uid)
                    }
                    refreshMembers()
                }
                2 -> {
                    firestoreRepository.removeMember(groupId, member.uid)
                    refreshMembers()
                }
            }
            true
        }
        popup.show()
    }

    private fun showAddMemberDialog() {
        val input = EditText(this)
        input.hint = getString(R.string.hint_member_uid)

        AlertDialog.Builder(this)
            .setTitle(R.string.action_add_member)
            .setView(input)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val uid = input.text.toString().trim().uppercase()
                if (uid.isNotEmpty()) addMember(uid)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun addMember(uid: String) {
        lifecycleScope.launch {
            val exists = firestoreRepository.uidExists(uid)
            if (!exists) {
                Toast.makeText(this@GroupDetailActivity, R.string.error_friend_id_not_found, Toast.LENGTH_SHORT).show()
                return@launch
            }
            firestoreRepository.addMember(groupId, uid)
            refreshMembers()
        }
    }

    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
    }
}
