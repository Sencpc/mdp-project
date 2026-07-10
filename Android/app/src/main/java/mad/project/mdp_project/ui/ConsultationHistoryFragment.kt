package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import mad.project.mdp_project.model.ConsultationHistoryViewModel

class ConsultationHistoryFragment : Fragment() {
    private val viewModel: ConsultationHistoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ConsultationHistoryScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                )
            }
        }
    }
}
