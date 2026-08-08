package com.echochat.cid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.echochat.cid.data.GroupMessage
import com.echochat.cid.databinding.ItemGroupMessageBinding
import java.text.SimpleDateFormat
import java.util.Locale

class GroupMessageAdapter : ListAdapter<GroupMessage, GroupMessageAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGroupMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemGroupMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: GroupMessage) {
            val time = timeFormat.format(message.timestamp)
            if (message.isMine) {
                binding.bubbleMine.visibility = View.VISIBLE
                binding.bubbleFriend.visibility = View.GONE
                binding.textMineContent.text = message.content
                binding.textMineTime.text = time
            } else {
                binding.bubbleMine.visibility = View.GONE
                binding.bubbleFriend.visibility = View.VISIBLE
                binding.textSenderName.text = message.senderNickname
                binding.textFriendContent.text = message.content
                binding.textFriendTime.text = time
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<GroupMessage>() {
            override fun areItemsTheSame(oldItem: GroupMessage, newItem: GroupMessage) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: GroupMessage, newItem: GroupMessage) = oldItem == newItem
        }
    }
}
