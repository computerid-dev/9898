package com.echochat.cid.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.echochat.cid.databinding.ItemChatEntryBinding

class ChatEntryAdapter(
    private val onDirectClicked: (ChatEntry.Direct) -> Unit,
    private val onGroupClicked: (ChatEntry.Group) -> Unit
) : ListAdapter<ChatEntry, ChatEntryAdapter.ChatEntryViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatEntryViewHolder {
        val binding = ItemChatEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatEntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatEntryViewHolder(
        private val binding: ItemChatEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: ChatEntry) {
            when (entry) {
                is ChatEntry.Direct -> {
                    val friend = entry.item.friend
                    binding.textNickname.text = friend.nickname
                    binding.textAvatarInitial.text = friend.nickname.trim().firstOrNull()
                        ?.uppercaseChar()?.toString() ?: "?"
                    binding.textLastMessage.text = entry.item.lastMessage ?: friend.friendUid
                    binding.root.setOnClickListener { onDirectClicked(entry) }
                }
                is ChatEntry.Group -> {
                    val group = entry.item.group
                    binding.textNickname.text = group.name
                    binding.textAvatarInitial.text = group.name.trim().firstOrNull()
                        ?.uppercaseChar()?.toString() ?: "#"
                    binding.textLastMessage.text = entry.item.lastMessage ?: "Grup baru dibuat"
                    binding.root.setOnClickListener { onGroupClicked(entry) }
                }
            }

            val unread = entry.unreadCount
            if (unread > 0) {
                binding.textUnreadBadge.visibility = android.view.View.VISIBLE
                binding.textUnreadBadge.text = if (unread > 99) "99+" else unread.toString()
            } else {
                binding.textUnreadBadge.visibility = android.view.View.GONE
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChatEntry>() {
            override fun areItemsTheSame(oldItem: ChatEntry, newItem: ChatEntry): Boolean {
                return when {
                    oldItem is ChatEntry.Direct && newItem is ChatEntry.Direct ->
                        oldItem.item.friend.id == newItem.item.friend.id
                    oldItem is ChatEntry.Group && newItem is ChatEntry.Group ->
                        oldItem.item.group.id == newItem.item.group.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: ChatEntry, newItem: ChatEntry) =
                oldItem == newItem
        }
    }
}
