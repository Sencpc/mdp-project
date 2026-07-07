package mad.project.mdp_project.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

                // Observe Reminders
                launch {
                    viewModel.habitsWithReminder.collectLatest { reminders ->
                        updateRemindersUI(reminders)
                    }
                }
            }
        }
    }

    private fun updateRemindersUI(reminders: List<mad.project.mdp_project.data.Habit>) {
        val container = binding.llRemindersContainer
        // Keep the "no reminders" text view, remove others
        for (i in container.childCount - 1 downTo 0) {
            val view = container.getChildAt(i)
            if (view.id != R.id.tv_no_reminders) {
                container.removeViewAt(i)
            }
        }
        
        if (reminders.isEmpty()) {
            binding.tvNoReminders.visibility = View.VISIBLE
            return
        }
        
        binding.tvNoReminders.visibility = View.GONE
        
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        
        // Take at most 3 upcoming reminders
        reminders.take(3).forEach { habit ->
            val reminderView = layoutInflater.inflate(R.layout.item_dashboard_reminder, container, false)
            
            val tvName = reminderView.findViewById<TextView>(R.id.tv_reminder_name)
            val tvTime = reminderView.findViewById<TextView>(R.id.tv_reminder_time)
            val indicator = reminderView.findViewById<View>(R.id.view_indicator)
            
            tvName.text = habit.name
            
            habit.reminderTime?.let { time ->
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = time }
                tvTime.text = timeFormat.format(calendar.time)
            }
            
            // Set indicator color based on category
            val colorRes = when (habit.category.lowercase()) {
                "nutrition" -> R.drawable.bg_circle_green
                "mental" -> R.drawable.bg_circle_blue
                "fitness" -> R.drawable.bg_circle_green // Reuse existing colors
                "sleep" -> R.drawable.bg_circle_green
                else -> R.drawable.bg_circle_blue
            }
            indicator.setBackgroundResource(colorRes)
            
            container.addView(reminderView)
        }
    }

    private fun setupClickListeners() {
        binding.profileCircle.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_nav_profile)
        }
        binding.btnConsult.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_nav_personal_doctor)
        }
        binding.btnChatbot.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_nav_chatbot)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
