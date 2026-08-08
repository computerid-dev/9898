package com.echochat.cid.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.echochat.cid.R
import com.echochat.cid.data.AppDatabase
import com.echochat.cid.data.FirestoreRepository
import com.echochat.cid.data.GroupChat
import com.echochat.cid.databinding.ActivityCreateGroupBinding
import com.echochat.cid.util.SessionManager
import kotlinx.coroutines.launch

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateGroupBinding
    private lateinit var session: SessionManager
    private val firestoreRepository = FirestoreRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonCreateGroup.setOnClickListener { createGroup() }
    }

    private fun createGroup() {
        val name = binding.inputGroupName.text.toString().trim()
        if (name.isEmpty()) {
            binding.inputGroupName.error = getString(R.string.error_group_name_empty)
            return
        }

        binding.buttonCreateGroup.isEnabled = false
        val groupId = firestoreRepository.createGroup(name, session.myUid)

        lifecycleScope.launch {
            AppDatabase.getInstance(this@CreateGroupActivity).groupDao().upsert(
                GroupChat(groupId = groupId, name = name)
            )
            val intent = android.content.Intent(this@CreateGroupActivity, GroupDetailActivity::class.java)
            intent.putExtra(GroupDetailActivity.EXTRA_GROUP_ID, groupId)
            intent.putExtra(GroupDetailActivity.EXTRA_GROUP_NAME, name)
            startActivity(intent)
            finish()
        }
    }
}
