package com.echochat.cid.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.databinding.FragmentContactsBinding
import kotlinx.coroutines.launch

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: FriendListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = FriendListAdapter { friend ->
            val intent = Intent(requireContext(), FriendDetailActivity::class.java)
            intent.putExtra(FriendDetailActivity.EXTRA_FRIEND_UID, friend.friendUid)
            startActivity(intent)
        }

        binding.recyclerContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerContacts.adapter = adapter

        val friendDao = AppDatabase.getInstance(requireContext()).friendDao()
        viewLifecycleOwner.lifecycleScope.launch {
            friendDao.observeContactsSorted().collect { friends ->
                adapter.submitList(friends)
                val isEmpty = friends.isEmpty()
                binding.textEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.recyclerContacts.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
