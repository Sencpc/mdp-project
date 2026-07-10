package mad.project.mdp_project.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import mad.project.mdp_project.LoginActivity
import mad.project.mdp_project.R
import mad.project.mdp_project.data.SessionManager

class SettingsFragment : Fragment() {

    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        sessionManager = SessionManager(requireContext())

        view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            findNavController().popBackStack()
        }

        // Just UI placeholders for switches for now
        val switchHabit = view.findViewById<Switch>(R.id.switch_habit)
        val switchScreen = view.findViewById<Switch>(R.id.switch_screen)

        view.findViewById<MaterialButton>(R.id.btn_logout).setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        return view
    }
}
