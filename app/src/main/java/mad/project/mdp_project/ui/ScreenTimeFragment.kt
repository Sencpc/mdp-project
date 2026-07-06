package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import mad.project.mdp_project.databinding.FragmentScreenTimeBinding

class ScreenTimeFragment : Fragment() {
    private var _binding: FragmentScreenTimeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScreenTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Here you can initialize values or set click listeners
        binding.btnNotification.setOnClickListener {
            // Handle notification click
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
