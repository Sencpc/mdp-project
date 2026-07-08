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
import mad.project.mdp_project.model.HabitViewModel

class FormHabitsFragment : Fragment() {

    private var _binding: FragmentFormHabitsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HabitViewModel by viewModels()
    private val args: FormHabitsFragmentArgs by navArgs()
    private var selectedCategory: String = "Mental"
    private var selectedReminderTime: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormHabitsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupCategorySelection()
        setupReminder()
        loadArgsData()
    }

    /**
     * Load data dari NavArgs — jika habitId != -1, ini adalah mode edit.
     */
    private fun loadArgsData() {
        val habitId = args.habitId
        val habitName = args.habitName
        val habitCategory = args.habitCategory
        val habitSubtitle = args.habitSubtitle

        if (habitId != -1) {
            // Edit mode
            binding.etHabitName.setText(habitName)
            binding.etDescription.setText(habitSubtitle)
            selectedCategory = habitCategory
            binding.tvSaveHabit.setText("Update Habit")
            
            // Show remove button in edit mode
            binding.btnRemoveHabit.visibility = View.VISIBLE
            binding.btnRemoveHabit.setOnClickListener {
                viewModel.deleteHabitById(habitId)
                Toast.makeText(requireContext(), "Habit removed!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }

            val reminderTime = args.reminderTime
            if (reminderTime != -1L) {
                selectedReminderTime = reminderTime
                val calendar = Calendar.getInstance().apply { timeInMillis = reminderTime }
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                binding.tvReminderTime.text = timeFormat.format(calendar.time)
                binding.ivClearReminder.visibility = View.VISIBLE
            } else {
                binding.tvReminderTime.text = "Not set"
                binding.ivClearReminder.visibility = View.GONE
            }

            // Highlight the correct category chip
            val categories = mapOf(
                binding.tvCatNutrition to "Nutrition",
                binding.tvCatMental to "Mental",
                binding.tvCatFitness to "Fitness",
                binding.tvCatFocus to "Focus",
                binding.tvCatSleep to "Sleep"
            )
            categories.entries.find { it.value == habitCategory }?.let { entry ->
                updateCategoryUI(categories.keys, entry.key)
            }
        }
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnSaveHabit.setOnClickListener {
            val name = binding.etHabitName.text.toString()
            val description = binding.etDescription.text.toString()
            
            if (name.isNotEmpty()) {
                val finalReminderTime = selectedReminderTime
                if (args.habitId != -1) {
                    viewModel.updateHabit(args.habitId, name, description, selectedCategory, finalReminderTime)
                    Toast.makeText(requireContext(), "Habit updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addHabit(name, description, selectedCategory, finalReminderTime)
                    Toast.makeText(requireContext(), "Habit added successfully!", Toast.LENGTH_SHORT).show()
                }
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "Please enter a habit name", Toast.LENGTH_SHORT).show()
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
                selectedCategory = category
                updateCategoryUI(categories.keys, view)
            }
        }
    }

    private fun setupReminder() {
        binding.ivClearReminder.setOnClickListener {
            selectedReminderTime = null
            binding.tvReminderTime.text = "Not set"
            binding.ivClearReminder.visibility = View.GONE
        }

        binding.btnSelectReminderTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            selectedReminderTime?.let { time ->
                calendar.timeInMillis = time
            }

            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    val selectedCalendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                    }
                    selectedReminderTime = selectedCalendar.timeInMillis
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    binding.tvReminderTime.text = timeFormat.format(selectedCalendar.time)
                    binding.ivClearReminder.visibility = View.VISIBLE
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // 12 hour format
            ).show()
        }
    }

    private fun updateCategoryUI(allViews: Set<TextView>, selectedView: TextView) {
        allViews.forEach { view ->
            if (view == selectedView) {
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
