package com.echochat.cid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.echochat.cid.databinding.FragmentDeveloperBinding

class DeveloperFragment : Fragment() {

    private var _binding: FragmentDeveloperBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeveloperBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textDeveloperName.text = DEVELOPER_NAME

        binding.buttonContactWhatsapp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WHATSAPP_CS_URL))
            startActivity(intent)
        }

        binding.buttonContactEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL_CS"))
            startActivity(intent)
        }

        binding.buttonOpenSettings.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val DEVELOPER_NAME = "Nugroho Y.R."
        private const val WHATSAPP_CS_URL = "https://wa.me/6281522851050/"
        private const val EMAIL_CS = "nugrohokelyn@gmail.com"
    }
}
