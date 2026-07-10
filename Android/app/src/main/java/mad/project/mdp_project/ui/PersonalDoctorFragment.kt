package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import mad.project.mdp_project.model.DoctorViewModel

class PersonalDoctorFragment : Fragment() {
    private val viewModel: DoctorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                DoctorConsultScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    },
                    onConsultClick = { doctor ->
                        val action = PersonalDoctorFragmentDirections
                            .actionNavPersonalDoctorToNavScheduleConsultation(
                                doctorId = doctor.id,
                                doctorName = doctor.doctorName,
                                category = doctor.category,
                                rating = doctor.rating.toFloat(),
                                description = doctor.description,
                                profileIcon = doctor.profileIcon
                            )
                        findNavController().navigate(action)
                    },
                    onHistoryClick = {
                        findNavController().navigate(
                            PersonalDoctorFragmentDirections
                                .actionNavPersonalDoctorToNavConsultationHistory()
                        )
                    }
                )
            }
        }
    }
}
