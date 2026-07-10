package mad.project.mdp_project

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import mad.project.mdp_project.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Jika ditolak, notifikasi habit reminder tidak akan muncul
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Minta Izin Notifikasi untuk Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.constraintMain) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // Keep bottom padding 0 for bottom nav
            insets
        }

        // Fix: Use supportFragmentManager to find NavHostFragment when using FragmentContainerView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Custom swipe logic and navigation setup
        setupBottomNavigationAndSwipe(navController)
    }

    private lateinit var gestureDetector: android.view.GestureDetector

    private fun setupBottomNavigationAndSwipe(navController: androidx.navigation.NavController) {
        val menu = binding.bottomNavigation.menu

        // Handle Click (fixes Dashboard not being clickable)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val builder = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)

            val startDest = navController.graph.startDestinationId
            if (item.itemId == startDest) {
                builder.setPopUpTo(startDest, false)
            } else {
                builder.setPopUpTo(startDest, false, true)
            }

            try {
                navController.navigate(item.itemId, null, builder.build())
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }

        // Sync BottomNavigation selection with NavController (when navigating via back button or swipe)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val size = menu.size()
            for (i in 0 until size) {
                if (menu.getItem(i).itemId == destination.id) {
                    menu.getItem(i).isChecked = true
                    break
                }
            }
        }

        // Setup Swipe Detection
        gestureDetector = android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null) {
                    val diffY = e2.y - e1.y
                    val diffX = e2.x - e1.x
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                navigateSwipe(false, navController) // Swipe right (Previous tab)
                            } else {
                                navigateSwipe(true, navController) // Swipe left (Next tab)
                            }
                        }
                    }
                }
                return false
            }
        })
    }

    private fun navigateSwipe(isNext: Boolean, navController: androidx.navigation.NavController) {
        val currentId = navController.currentDestination?.id ?: return
        val menu = binding.bottomNavigation.menu
        var currentIndex = -1
        
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).itemId == currentId) {
                currentIndex = i
                break
            }
        }

        if (currentIndex != -1) {
            val nextIndex = if (isNext) currentIndex + 1 else currentIndex - 1
            if (nextIndex in 0 until menu.size()) {
                val nextItemId = menu.getItem(nextIndex).itemId
                binding.bottomNavigation.selectedItemId = nextItemId
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (::gestureDetector.isInitialized) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }
}
