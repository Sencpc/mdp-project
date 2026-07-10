package mad.project.mdp_project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import mad.project.mdp_project.R
import mad.project.mdp_project.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragments = listOf(
            DashboardFragment(),
            HabitsFragment(),
            ScreenTimeFragment(),
            SleepTrackerFragment(),
            NutritionLogFragment()
        )

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        binding.viewPager.adapter = adapter
        
        // Sync ViewPager2 with BottomNavigationView
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val itemId = when (position) {
                    0 -> R.id.nav_dashboard
                    1 -> R.id.nav_habits
                    2 -> R.id.nav_screen
                    3 -> R.id.nav_sleep
                    4 -> R.id.nav_nutrition_log
                    else -> R.id.nav_dashboard
                }
                if (binding.bottomNavigation.selectedItemId != itemId) {
                    binding.bottomNavigation.selectedItemId = itemId
                }
            }
        })

        // Sync BottomNavigationView with ViewPager2
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {
                R.id.nav_dashboard -> 0
                R.id.nav_habits -> 1
                R.id.nav_screen -> 2
                R.id.nav_sleep -> 3
                R.id.nav_nutrition_log -> 4
                else -> 0
            }
            if (binding.viewPager.currentItem != position) {
                binding.viewPager.currentItem = position
            }
            true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
