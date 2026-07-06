package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import mad.project.mdp_project.R
import mad.project.mdp_project.databinding.FragmentFormHabitsBinding
import mad.project.mdp_project.model.HabitViewModel

class FormHabitsFragment : Fragment() {

    private var _binding: FragmentFormHabitsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HabitViewModel by viewModels()
    private var selectedCategory: String = "Mental"

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
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnSaveHabit.setOnClickListener {
            val name = binding.etHabitName.text.toString()
            val description = binding.etDescription.text.toString()
            
            if (name.isNotEmpty()) {
                viewModel.addHabit(name, description, selectedCategory)
                Toast.makeText(requireContext(), "Habit added successfully!", Toast.LENGTH_SHORT).show()
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
