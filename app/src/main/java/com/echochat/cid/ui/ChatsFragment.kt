package com.echochat.cid.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.echochat.cid.R
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.databinding.FragmentChatsBinding
import com.echochat.cid.util.SessionManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {

    private var _binding: FragmentChatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private lateinit var adapter: ChatEntryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        session = SessionManager(requireContext())

        refreshUidVisibility()
        binding.buttonCopyUid.setOnClickListener { copyUidToClipboard() }

        adapter = ChatEntryAdapter(
            onDirectClicked = { entry ->
                val friend = entry.item.friend
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra(ChatActivity.EXTRA_FRIEND_UID, friend.friendUid)
                intent.putExtra(ChatActivity.EXTRA_FRIEND_NICKNAME, friend.nickname)
                startActivity(intent)
            },
            onGroupClicked = { entry ->
                val intent = Intent(requireContext(), GroupChatActivity::class.java)
                intent.putExtra(GroupChatActivity.EXTRA_GROUP_ID, entry.item.group.groupId)
                intent.putExtra(GroupChatActivity.EXTRA_GROUP_NAME, entry.item.group.name)
                startActivity(intent)
            }
        )

        binding.recyclerFriends.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerFriends.adapter = adapter

        binding.fabAddFriend.setOnClickListener { showAddMenu() }

        observeChats()
    }

    private fun showAddMenu() {
        val popup = PopupMenu(requireContext(), binding.fabAddFriend)
        popup.menu.add(0, 1, 0, getString(R.string.action_add_friend_menu))
        popup.menu.add(0, 2, 1, getString(R.string.action_create_group_menu))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> startActivity(Intent(requireContext(), AddFriendActivity::class.java))
                2 -> startActivity(Intent(requireContext(), CreateGroupActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun observeChats() {
        val database = AppDatabase.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                database.friendDao().observeActiveChats(),
                database.groupDao().observeAllWithPreview()
            ) { directs, groups ->
                val entries = mutableListOf<ChatEntry>()
                directs.forEach { entries.add(ChatEntry.Direct(it)) }
                groups.forEach { entries.add(ChatEntry.Group(it)) }
                entries.sortByDescending { it.sortKey }
                entries
            }.collect { entries ->
                adapter.submitList(entries)
                val isEmpty = entries.isEmpty()
                binding.textEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.recyclerFriends.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        }
    }

    private fun refreshUidVisibility() {
        if (session.isUidHidden) {
            binding.rowMyUid.visibility = View.GONE
        } else {
            binding.rowMyUid.visibility = View.VISIBLE
            binding.textMyUid.text = session.myUid
        }
    }

    private fun copyUidToClipboard() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("EchoChat UID", session.myUid))
        Toast.makeText(requireContext(), R.string.id_copied, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        refreshUidVisibility()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
