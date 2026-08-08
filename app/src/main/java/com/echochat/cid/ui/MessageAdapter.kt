package com.echochat.cid.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.echochat.cid.data.Message
import com.echochat.cid.databinding.ItemMessageBinding
import java.text.SimpleDateFormat
import java.util.Locale

class MessageAdapter : ListAdapter<Message, MessageAdapter.MessageViewHolder>(DIFF_CALLBACK) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(
        private val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            val time = timeFormat.format(message.timestamp)
            if (message.isMine) {
                binding.bubbleMine.visibility = android.view.View.VISIBLE
                binding.bubbleFriend.visibility = android.view.View.GONE
                binding.textMineContent.text = message.content
                binding.textMineTime.text = time
            } else {
                binding.bubbleMine.visibility = android.view.View.GONE
                binding.bubbleFriend.visibility = android.view.View.VISIBLE
                binding.textFriendContent.text = message.content
                binding.textFriendTime.text = time
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Message, newItem: Message) =
                oldItem == newItem
        }
    }
}
