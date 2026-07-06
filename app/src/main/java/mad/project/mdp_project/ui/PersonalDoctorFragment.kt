package mad.project.mdp_project.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import mad.project.mdp_project.R
import mad.project.mdp_project.databinding.FragmentPersonalDoctorBinding
import mad.project.mdp_project.model.Doctor

class PersonalDoctorFragment : Fragment() {
    private var _binding: FragmentPersonalDoctorBinding? = null
    private val binding get() = _binding!!
    private val doctorAdapter = DoctorAdapter()

    private val allDoctors = listOf(
        Doctor(
            1, "Dr. Sarah Jenkins", "General Practice", 4.9f, 128,
            "Experienced general practitioner focusing on holistic health and preventative care for adults...",
            "Today, 2:00 PM", true
        ),
        Doctor(
            2, "Dr. Marcus Chen", "Therapy", 4.8f, 95,
            "Specializing in cognitive behavioral therapy and stress management techniques...",
            "Tomorrow, 10:00 AM"
        ),
        Doctor(
            3, "Dr. Elena Rodriguez", "Nutrition", 4.7f, 150,
            "Expert in clinical nutrition and metabolic health, helping patients achieve sustainable weight goals...",
            "Monday, 3:30 PM"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalDoctorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
    }

    private fun setupRecyclerView() {
        binding.rvDoctors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDoctors.adapter = doctorAdapter
        doctorAdapter.submitList(allDoctors)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDoctors()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val filterButtons = listOf(
            binding.btnFilterAll,
            binding.btnFilterGeneral,
            binding.btnFilterTherapy,
            binding.btnFilterNutrition
        )

        filterButtons.forEach { button ->
            button.setOnClickListener {
                // Handle single selection manually
                filterButtons.forEach { it.isChecked = (it == button) }
                filterDoctors()
            }
        }
    }

    private fun filterDoctors() {
        val query = binding.etSearch.text.toString().lowercase()
        
        val filteredList = allDoctors.filter { doctor ->
            val matchesQuery = doctor.name.lowercase().contains(query) ||
                    doctor.specialty.lowercase().contains(query)

            val matchesCategory = when {
                binding.btnFilterGeneral.isChecked -> doctor.specialty == "General Practice"
                binding.btnFilterTherapy.isChecked -> doctor.specialty == "Therapy"
                binding.btnFilterNutrition.isChecked -> doctor.specialty == "Nutrition"
                else -> true // All selected
            }

            matchesQuery && matchesCategory
        }
        doctorAdapter.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
