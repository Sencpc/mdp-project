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
import androidx.navigation.fragment.navArgs
import mad.project.mdp_project.R
import mad.project.mdp_project.model.ConsultationViewModel

class ScheduleConsultationFragment : Fragment() {
    private val viewModel: ConsultationViewModel by viewModels()
    private val args: ScheduleConsultationFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ScheduleConsultationScreen(
                    doctorId = args.doctorId,
                    doctorName = args.doctorName,
                    category = args.category,
                    rating = args.rating,
                    description = args.description,
                    profileIcon = args.profileIcon,
                    viewModel = viewModel,
                    onBackClick = {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    },
                    onConfirmed = {
                        // Navigate back to Dashboard
                        findNavController().navigate(R.id.nav_dashboard)
                    }
                )
            }
        }
    }
}
