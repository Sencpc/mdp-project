package mad.project.mdp_project.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mad.project.mdp_project.R
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

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
        
        val context = requireContext()
        val sessionManager = SessionManager(context)
        val db = AppDatabase.getDatabase(context)
        val userId = sessionManager.getUserId()

        // Observe User Data (Real-time Sync)
        viewLifecycleOwner.lifecycleScope.launch {
            db.userDao().getUserById(userId).collectLatest { user ->
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

        binding.profileCircle.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_nav_profile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
