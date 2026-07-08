package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.habits.collect { habits ->
                    _binding?.let { b ->
                        val habit = habits.find { it.id == args.habitId }
                        habit?.let { h ->
                            b.tvHabitName.text = h.name
                            b.tvSubtitle.text = h.subtitle
                            b.tvCategory.text = h.category

                            b.tvStreak.text = "Current Streak: ${h.streak} days"
                            
                            // Tampilkan info reminder jika ada
                            if (h.reminderTime != null) {
                                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                                b.tvStreak.append("\nReminder: ${sdf.format(Date(h.reminderTime!!))}")
                            }
                            
                            b.ivEdit.setOnClickListener {
                                val action = HabitDetailFragmentDirections.actionNavHabitDetailToNavFormHabits(h.id)
                                findNavController().navigate(action)
                            }

                            b.btnRemove.setOnClickListener {
                                viewModel.deleteHabit(h)
                                Toast.makeText(requireContext(), "Habit removed", Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            }
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
