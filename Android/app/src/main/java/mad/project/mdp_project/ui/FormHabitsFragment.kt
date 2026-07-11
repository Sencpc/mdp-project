package mad.project.mdp_project.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
            viewModel.habitName.value = binding.etHabitName.text.toString()
            viewModel.habitSubtitle.value = binding.etSubtitle.text.toString()
            
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
        viewModel.isEditMode.observe(viewLifecycleOwner) { isEdit ->
            binding.tvToolbarTitle.text = if (isEdit) "Edit Habit" else "New Habit"
            binding.btnSaveHabit.text = if (isEdit) "Update Habit" else "Save Habit"
        }

        viewModel.habitName.observe(viewLifecycleOwner) { name ->
            if (binding.etHabitName.text.toString() != name) {
                binding.etHabitName.setText(name)
            }
        }

        viewModel.habitSubtitle.observe(viewLifecycleOwner) { subtitle ->
            if (binding.etSubtitle.text.toString() != subtitle) {
                binding.etSubtitle.setText(subtitle)
            }
        }

        viewModel.habitCategory.observe(viewLifecycleOwner) { category ->
            updateCategoryUI(category)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.btnSaveHabit.isEnabled = !loading
            binding.btnSaveHabit.alpha = if (loading) 0.5f else 1.0f
        }
        
        viewModel.reminders.observe(viewLifecycleOwner) { times ->
            binding.llRemindersContainer.removeAllViews()
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            
            times.forEachIndexed { index, time ->
                val view = LayoutInflater.from(requireContext()).inflate(R.layout.item_reminder_form, binding.llRemindersContainer, false)
                val tvTime = view.findViewById<TextView>(R.id.tvReminderTime)
                val ivClear = view.findViewById<View>(R.id.ivClearReminder)
                
                val calendar = Calendar.getInstance().apply { timeInMillis = time }
                tvTime.text = timeFormat.format(calendar.time)
                
                ivClear.setOnClickListener {
                    val currentList = viewModel.reminders.value?.toMutableList() ?: mutableListOf()
                    if (index < currentList.size) {
                        currentList.removeAt(index)
                        viewModel.reminders.value = currentList
                    }
                }
                
                binding.llRemindersContainer.addView(view)
            }
            
            if (times.size >= 24) {
                binding.btnAddReminder.visibility = View.GONE
            } else {
                binding.btnAddReminder.visibility = View.VISIBLE
            }
        }
    }

    private fun setupReminder() {
        binding.btnAddReminder.setOnClickListener {
            val currentList = viewModel.reminders.value?.toMutableList() ?: mutableListOf()
            if (currentList.size >= 24) {
                Toast.makeText(requireContext(), "Maximum 24 reminders allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val calendar = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, m ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                }
                currentList.add(selected.timeInMillis)
                // sort reminders
                currentList.sort()
                viewModel.reminders.value = currentList
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
