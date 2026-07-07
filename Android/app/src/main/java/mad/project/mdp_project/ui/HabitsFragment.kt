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
import mad.project.mdp_project.R
import mad.project.mdp_project.data.Habit
import mad.project.mdp_project.databinding.FragmentHabitsBinding
import mad.project.mdp_project.model.HabitViewModel

class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HabitViewModel by viewModels()
    private lateinit var habitAdapter: HabitAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        habitAdapter = HabitAdapter(
            onHabitClick = { habit ->
                // Navigasi ke FormHabits dengan NavArgs untuk edit mode
                val action = HabitsFragmentDirections.actionNavHabitsToNavFormHabits(
                    habitId = habit.id,
                    habitName = habit.name,
                    habitCategory = habit.category,
                    habitSubtitle = habit.subtitle,
                    reminderTime = habit.reminderTime ?: -1L
                )
                findNavController().navigate(action)
            },
            onAddClick = {
                findNavController().navigate(R.id.action_nav_habits_to_nav_form_habits)
            },
            onCompleteClick = { habit, isCompleted ->
                viewModel.toggleHabitCompletion(habit, isCompleted)
            }
        )
        
        binding.rvHabits.apply {
            adapter = habitAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.habits.collectLatest { habits ->
                    habitAdapter.submitList(habits)
                    updateProgress(habits)
                }
            }
        }
    }

    private fun updateProgress(habits: List<Habit>) {
        val total = habits.size
        val completed = habits.count { it.isCompleted }
        
        binding.tvProgressPercent.text = "$completed/$total"
        binding.circularProgressBar.progress = if (total > 0) (completed * 100 / total) else 0
        
        val remaining = total - completed
        binding.tvProgressStatus.text = if (remaining > 0) {
            "Keep it up! $remaining more to go."
        } else {
            "All habits completed! Well done!"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
