package mad.project.mdp_project.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import mad.project.mdp_project.databinding.FragmentPersonalDoctorBinding
import mad.project.mdp_project.model.DoctorViewModel

class PersonalDoctorFragment : Fragment() {
    private var _binding: FragmentPersonalDoctorBinding? = null
    private val binding get() = _binding!!
    private val doctorAdapter = DoctorAdapter()
    private val viewModel: DoctorViewModel by viewModels()

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
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvDoctors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDoctors.adapter = doctorAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
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
                val filter = when (button) {
                    binding.btnFilterGeneral -> "General Practice"
                    binding.btnFilterTherapy -> "Therapy"
                    binding.btnFilterNutrition -> "Nutrition"
                    else -> "All"
                }
                viewModel.setFilter(filter)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.filteredDoctors.observe(viewLifecycleOwner) { doctors ->
            doctorAdapter.submitList(doctors)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
