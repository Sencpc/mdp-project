package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
import mad.project.mdp_project.databinding.FragmentHabitDetailBinding
import mad.project.mdp_project.model.HabitViewModel

class HabitDetailFragment : Fragment() {

    private var _binding: FragmentHabitDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HabitViewModel by viewModels()
    private val args: HabitDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        
        lifecycleScope.launch {
            // Kita butuh fungsi di ViewModel atau Repository untuk ambil Habit by ID
            // Namun karena kita punya flow 'habits' di ViewModel, kita bisa filter dari sana
            // atau tambahkan fungsi baru. Mari kita asumsikan ada di Repository.
            // Untuk tantangan ini, kita gunakan viewModel.habits
            viewModel.habits.collect { habits ->
                val habit = habits.find { it.id == args.habitId }
                habit?.let { h ->
                    binding.tvHabitName.text = h.name
                    binding.tvCategory.text = h.category
                    binding.tvDescription.text = h.subtitle
                    binding.tvStreak.text = "Current Streak: ${h.streak} days"
                    
                    binding.ivEdit.setOnClickListener {
                        val action = HabitDetailFragmentDirections.actionNavHabitDetailToNavFormHabits(h.id)
                        findNavController().navigate(action)
                    }

                    binding.btnRemove.setOnClickListener {
                        viewModel.deleteHabit(h)
                        Toast.makeText(requireContext(), "Habit removed", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
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
