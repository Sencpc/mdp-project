package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mad.project.mdp_project.databinding.FragmentChatbotBinding
import mad.project.mdp_project.model.ChatViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        binding.rvChat.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSettings.setOnClickListener {
            val options = arrayOf("Clear Chat History", "Reset AI Memory", "Clear Both")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chat Options")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> viewModel.clearHistory()
                        1 -> viewModel.resetMemory()
                        2 -> {
                            viewModel.clearHistory()
                            viewModel.resetMemory()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnSend.setOnClickListener {
            val message = binding.etMessage.text.toString()
            if (message.isNotBlank()) {
                viewModel.sendMessage(message)
                binding.etMessage.text.clear()
            }
        }

        binding.chipSleep.setOnClickListener {
            viewModel.sendQuickAction("How can I improve my sleep quality?")
        }

        binding.chipSleepTime.setOnClickListener {
            viewModel.sendQuickAction("Check my sleep time!")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe messages
                launch {
                    viewModel.messages.collectLatest { messages ->
                        adapter.submitList(messages) {
                            if (messages.isNotEmpty()) {
                                binding.rvChat.smoothScrollToPosition(messages.size - 1)
                            }
                        }
                    }
                }

                // Observe loading state to disable/enable send button and show typing indicator
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.btnSend.isEnabled = !isLoading
                        binding.btnSend.alpha = if (isLoading) 0.5f else 1.0f
                        
                        binding.layoutTyping.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
                        
                        // Scroll to bottom when typing indicator appears
                        if (isLoading && adapter.itemCount > 0) {
                            binding.rvChat.smoothScrollToPosition(adapter.itemCount - 1)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
