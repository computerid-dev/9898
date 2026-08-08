package com.echochat.cid.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.echochat.cid.databinding.ItemGroupMemberBinding

data class GroupMemberUiModel(
    val uid: String,
    val displayName: String,
    val isAdmin: Boolean,
    val isOwner: Boolean,
    val isMe: Boolean
)

class GroupMemberAdapter(
    private val canManage: Boolean,
    private val onManageClicked: (GroupMemberUiModel, android.view.View) -> Unit
) : RecyclerView.Adapter<GroupMemberAdapter.MemberViewHolder>() {

    private val members = mutableListOf<GroupMemberUiModel>()

    fun submitList(newMembers: List<GroupMemberUiModel>) {
        members.clear()
        members.addAll(newMembers)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemGroupMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount() = members.size

    inner class MemberViewHolder(
        private val binding: ItemGroupMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: GroupMemberUiModel) {
            binding.textMemberName.text = if (member.isMe) {
                "${member.displayName} (kamu)"
            } else {
                member.displayName
            }
            binding.textMemberRole.text = when {
                member.isOwner -> "Owner"
                member.isAdmin -> "Admin"
                else -> "Anggota"
            }

            val showActions = canManage && !member.isMe && !member.isOwner
            binding.buttonMemberActions.visibility = if (showActions) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
            binding.buttonMemberActions.setOnClickListener {
                onManageClicked(member, it)
            }
        }
    }
}
