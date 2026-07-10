package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mad.project.mdp_project.R
import mad.project.mdp_project.data.NutritionLog
import mad.project.mdp_project.databinding.FragmentNutritionLogBinding
import mad.project.mdp_project.model.NutritionLogViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NutritionLogFragment : Fragment() {

    private var _binding: FragmentNutritionLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NutritionLogViewModel by viewModels()

    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

    private val mealTypeLabels = mapOf(
        "breakfast" to "Breakfast",
        "lunch" to "Lunch",
        "dinner" to "Dinner",
        "additional" to "Additional"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNutritionLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnPrevDay.setOnClickListener {
            viewModel.navigateDate(-1)
        }
        binding.btnNextDay.setOnClickListener {
            viewModel.navigateDate(1)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe selected date
                launch {
                    viewModel.selectedDate.collectLatest { dateMs ->
                        binding.tvSelectedDate.text = dateFormat.format(Date(dateMs))
                    }
                }

                // Observe logs for the selected date
                launch {
                    viewModel.logsForDate.collectLatest { logs ->
                        renderMealSections(logs)
                    }
                }

                // Observe errors
                launch {
                    viewModel.updateError.collectLatest { error ->
                        if (error != null) {
                            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun renderMealSections(logs: List<NutritionLog>) {
        val grouped = logs.groupBy { it.mealType }

        val recommended = viewModel.recommendedCalories.value
        val totalCalories = logs.sumOf { it.calories }
        binding.tvTotalCalories.text = "Total: ${String.format(Locale.getDefault(), "%,d", totalCalories)} / ${String.format(Locale.getDefault(), "%,d", recommended)} kcal"

        renderSection(
            container = binding.llBreakfastItems,
            emptyView = binding.tvBreakfastEmpty,
            caloriesView = binding.tvBreakfastCalories,
            items = grouped["breakfast"] ?: emptyList()
        )

        renderSection(
            container = binding.llLunchItems,
            emptyView = binding.tvLunchEmpty,
            caloriesView = binding.tvLunchCalories,
            items = grouped["lunch"] ?: emptyList()
        )

        renderSection(
            container = binding.llDinnerItems,
            emptyView = binding.tvDinnerEmpty,
            caloriesView = binding.tvDinnerCalories,
            items = grouped["dinner"] ?: emptyList()
        )

        renderSection(
            container = binding.llAdditionalItems,
            emptyView = binding.tvAdditionalEmpty,
            caloriesView = binding.tvAdditionalCalories,
            items = grouped["additional"] ?: emptyList()
        )
    }

    private fun renderSection(
        container: LinearLayout,
        emptyView: TextView,
        caloriesView: TextView,
        items: List<NutritionLog>
    ) {
        container.removeAllViews()

        if (items.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            caloriesView.text = "0 kcal"
            return
        }

        emptyView.visibility = View.GONE
        val total = items.sumOf { it.calories }
        caloriesView.text = "$total kcal"

        items.forEach { log ->
            val itemView = layoutInflater.inflate(R.layout.item_nutrition_entry, container, false)

            val tvFoodName = itemView.findViewById<TextView>(R.id.tv_entry_food_name)
            val tvCalories = itemView.findViewById<TextView>(R.id.tv_entry_calories)
            val tvMealType = itemView.findViewById<TextView>(R.id.tv_entry_meal_type)

            tvFoodName.text = log.foodName
            tvCalories.text = "${log.calories} kcal"
            tvMealType.text = "${mealTypeLabels[log.mealType] ?: "Additional"} ▾"

            // Tap meal type badge to show reclassification popup
            tvMealType.setOnClickListener { anchor ->
                showMealTypePopup(anchor, log)
            }

            container.addView(itemView)
        }
    }

    private fun showMealTypePopup(anchor: View, log: NutritionLog) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 0, 0, "Breakfast")
        popup.menu.add(0, 1, 1, "Lunch")
        popup.menu.add(0, 2, 2, "Dinner")
        popup.menu.add(0, 3, 3, "Additional")

        popup.setOnMenuItemClickListener { menuItem ->
            val newType = when (menuItem.itemId) {
                0 -> "breakfast"
                1 -> "lunch"
                2 -> "dinner"
                3 -> "additional"
                else -> return@setOnMenuItemClickListener false
            }

            if (newType != log.mealType) {
                viewModel.updateMealType(log.id, newType)
            }
            true
        }

        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
