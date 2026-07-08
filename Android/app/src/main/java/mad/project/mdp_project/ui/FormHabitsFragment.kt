package mad.project.mdp_project.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import mad.project.mdp_project.R
import mad.project.mdp_project.databinding.FragmentFormHabitsBinding
import mad.project.mdp_project.model.FormHabitViewModel

class FormHabitsFragment : Fragment() {

    private var _binding: FragmentFormHabitsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FormHabitViewModel by viewModels()
    private val args: FormHabitsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormHabitsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadHabit(args.habitId)
        setupUI()
        setupCategorySelection()
        setupReminder()
        observeViewModel()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnSaveHabit.setOnClickListener {
            viewModel.saveHabit {
                Toast.makeText(requireContext(), "Habit saved!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun setupCategorySelection() {
        val categories = mapOf(
            binding.tvCatNutrition to "Nutrition",
            binding.tvCatMental to "Mental",
            binding.tvCatFitness to "Fitness",
            binding.tvCatFocus to "Focus",
            binding.tvCatSleep to "Sleep"
        )

        categories.forEach { (view, category) ->
            view.setOnClickListener {
                viewModel.habitCategory.value = category
            }
        }
    }

    private fun observeViewModel() {
        viewModel.habitCategory.observe(viewLifecycleOwner) { category ->
            updateCategoryUI(category)
        }
        
        viewModel.reminderTime.observe(viewLifecycleOwner) { time ->
            if (time != null) {
                val calendar = Calendar.getInstance().apply { timeInMillis = time }
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                binding.tvReminderTime.text = timeFormat.format(calendar.time)
                binding.ivClearReminder.visibility = View.VISIBLE
            } else {
                binding.tvReminderTime.text = "Not set"
                binding.ivClearReminder.visibility = View.GONE
            }
        }
    }

    private fun setupReminder() {
        binding.ivClearReminder.setOnClickListener {
            viewModel.reminderTime.value = null
        }

        binding.btnSelectReminderTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            viewModel.reminderTime.value?.let { calendar.timeInMillis = it }

            TimePickerDialog(requireContext(), { _, h, m ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                }
                viewModel.reminderTime.value = selected.timeInMillis
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }
    }

    private fun updateCategoryUI(selectedCategory: String) {
        val categoryViews = listOf(
            binding.tvCatNutrition, binding.tvCatMental, 
            binding.tvCatFitness, binding.tvCatFocus, binding.tvCatSleep
        )
        val categoryMap = mapOf(
            "Nutrition" to binding.tvCatNutrition,
            "Mental" to binding.tvCatMental,
            "Fitness" to binding.tvCatFitness,
            "Focus" to binding.tvCatFocus,
            "Sleep" to binding.tvCatSleep
        )

        categoryViews.forEach { view ->
            if (view == categoryMap[selectedCategory]) {
                view.setBackgroundResource(R.drawable.bg_chip_selected)
                view.setTextColor(resources.getColor(R.color.brand_primary, null))
            } else {
                view.setBackgroundResource(R.drawable.bg_badge)
                view.setTextColor(0xFF333333.toInt())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
