package mad.project.mdp_project

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.databinding.ActivityLoginBinding
import mad.project.mdp_project.model.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false
    private lateinit var sessionManager: SessionManager
    private val viewModel: LoginViewModel by viewModels { LoginViewModel.Factory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sessionManager = SessionManager(this)
        
        // Auto-login if session exists
        if (sessionManager.getUserId() != -1) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        setupPasswordVisibilityToggle()

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.login(username, password)
        }

        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { user ->
                sessionManager.saveSession(user.id, user.username)
                Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            result.onFailure { error ->
                Toast.makeText(this, error.message ?: "Login gagal", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.btnLogin.alpha = if (isLoading) 0.5f else 1.0f
        }

        // Smart loading overlay — only shown after 500ms delay
        viewModel.showLoadingOverlay.observe(this) { show ->
            if (show) {
                binding.loadingOverlay.alpha = 0f
                binding.loadingOverlay.visibility = View.VISIBLE
                binding.loadingOverlay.animate().alpha(1f).setDuration(200).start()
            } else {
                binding.loadingOverlay.animate().alpha(0f).setDuration(150).withEndAction {
                    binding.loadingOverlay.visibility = View.GONE
                }.start()
            }
        }
    }

    private fun setupPasswordVisibilityToggle() {
        binding.ivPasswordVisibility.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.ivPasswordVisibility.setImageResource(R.drawable.ic_visibility)
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.ivPasswordVisibility.setImageResource(R.drawable.ic_visibility_off)
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }
}
