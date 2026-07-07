package mad.project.mdp_project.ui

import android.net.Uri
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
import coil.load
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mad.project.mdp_project.R
import mad.project.mdp_project.databinding.FragmentDashboardBinding
import mad.project.mdp_project.model.DashboardViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe User Data
                launch {
                    viewModel.user.collectLatest { user ->
                        user?.let {
                            binding.tvGreeting.text = "Good morning, ${it.fullName.ifEmpty { it.username }}."

                            if (!it.profilePicturePath.isNullOrEmpty()) {
                                binding.profileCircle.load(Uri.parse(it.profilePicturePath)) {
                                    crossfade(true)
                                    placeholder(R.drawable.bg_circle_green)
                                    error(R.drawable.bg_circle_green)
                                }
                                binding.profileCircle.setPadding(0, 0, 0, 0)
                            } else {
                                binding.profileCircle.setImageResource(R.drawable.bg_circle_green)
                                binding.profileCircle.setPadding(0, 0, 0, 0)
                            }
                        }
                    }
                }

                // Observe Habits summary
                launch {
                    viewModel.habits.collectLatest { habits ->
                        val completed = habits.count { it.isCompleted }
                        val total = habits.size
                        // Update UI jika ada view untuk summary habits di dashboard
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.profileCircle.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_nav_profile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
