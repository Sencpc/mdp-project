package mad.project.mdp_project.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import mad.project.mdp_project.databinding.FragmentScreenTimeBinding
import mad.project.mdp_project.model.ScreenTimeViewModel

class ScreenTimeFragment : Fragment() {
    private var _binding: FragmentScreenTimeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScreenTimeViewModel by viewModels()

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
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data setiap kali fragment tampil
        viewModel.loadTodayUsage()
        viewModel.loadWeeklyAverage()
    }

    private fun setupUI() {
        binding.btnNotification.setOnClickListener {
            // Buka pengaturan Usage Access jika belum diberi permission
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun observeViewModel() {
        viewModel.totalScreenTime.observe(viewLifecycleOwner) { time ->
            // Update the time display in layoutTime
            // The layout has hardcoded TextViews inside layoutTime, 
            // we update them via the parent LinearLayout
            updateTimeDisplay(binding.layoutTime, time)
        }

        viewModel.dailyAverage.observe(viewLifecycleOwner) { avg ->
            // Could update a subtitle text if available
            binding.tvSubtitle.text = "Daily average: $avg"
        }
    }

    private fun updateTimeDisplay(layout: ViewGroup, timeStr: String) {
        // Parse "Xh Ym" format
        val regex = Regex("(\\d+)h\\s*(\\d+)m")
        val match = regex.find(timeStr)
        if (match != null && layout.childCount >= 4) {
            val hours = match.groupValues[1]
            val minutes = match.groupValues[2]
            // layoutTime has 4 children: hours, "h", minutes, "m"
            (layout.getChildAt(0) as? TextView)?.text = hours
            (layout.getChildAt(2) as? TextView)?.text = minutes
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
