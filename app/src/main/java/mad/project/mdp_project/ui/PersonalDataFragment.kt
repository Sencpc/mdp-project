package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mad.project.mdp_project.databinding.FragmentPersonalDataBinding

/**
 * Fragment to display and manage personal health data.
 */
class PersonalDataFragment : Fragment() {

    private var _binding: FragmentPersonalDataBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnNotification.setOnClickListener {
            Toast.makeText(context, "Notifications", Toast.LENGTH_SHORT).show()
        }

        binding.btnEditHeight.setOnClickListener {
            showEditDialog("Height", binding.tvHeightValue.text.toString(), binding.tvHeightUnit.text.toString()) { newValue, newUnit ->
                binding.tvHeightValue.text = newValue
                binding.tvHeightUnit.text = newUnit
            }
        }

        binding.btnEditWeight.setOnClickListener {
            showEditDialog("Weight", binding.tvWeightValue.text.toString(), binding.tvWeightUnit.text.toString()) { newValue, newUnit ->
                binding.tvWeightValue.text = newValue
                binding.tvWeightUnit.text = newUnit
            }
        }

        binding.btnEditBloodType.setOnClickListener {
            showEditDialog("Blood Type", binding.tvBloodValue.text.toString(), "") { newValue, _ ->
                binding.tvBloodValue.text = newValue
            }
        }

        binding.btnEditContact.setOnClickListener {
            showEditContactDialog()
        }

        binding.btnAddCondition.setOnClickListener {
            Toast.makeText(context, "Add Condition feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveChanges.setOnClickListener {
            Toast.makeText(context, "Changes Saved Successfully", Toast.LENGTH_SHORT).show()
        }

        // Chip close listeners
        binding.chipAsthma.setOnCloseIconClickListener {
            binding.chipGroupConditions.removeView(it)
        }
        binding.chipPollenAllergy.setOnCloseIconClickListener {
            binding.chipGroupConditions.removeView(it)
        }
    }

    private fun showEditDialog(
        title: String,
        currentValue: String,
        currentUnit: String,
        onSave: (String, String) -> Unit
    ) {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
        }

        val inputVal = EditText(context).apply {
            hint = "Value"
            setText(currentValue)
        }
        layout.addView(inputVal)

        val inputUnit = EditText(context).apply {
            hint = "Measurement (e.g., cm, kg)"
            setText(currentUnit)
            if (currentUnit.isEmpty()) visibility = View.GONE
        }
        layout.addView(inputUnit)

        MaterialAlertDialogBuilder(context)
            .setTitle("Edit $title")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newValue = inputVal.text.toString()
                val newUnit = inputUnit.text.toString()
                onSave(newValue, newUnit)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditContactDialog() {
        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
        }

        val inputName = EditText(context).apply {
            hint = "Name"
            setText(binding.tvContactName.text)
        }
        layout.addView(inputName)

        val inputPhone = EditText(context).apply {
            hint = "Phone Number"
            setText(binding.tvContactPhone.text)
        }
        layout.addView(inputPhone)

        MaterialAlertDialogBuilder(context)
            .setTitle("Edit Emergency Contact")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                binding.tvContactName.text = inputName.text.toString()
                binding.tvContactPhone.text = inputPhone.text.toString()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
