package mad.project.mdp_project

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import mad.project.mdp_project.databinding.ActivityRegisterBinding
import mad.project.mdp_project.model.RegisterViewModel

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isPasswordVisible = false
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }

        setupPasswordVisibilityToggle()
        setupPasswordStrengthChecker()
        setupTermsCheckbox()

        binding.btnCreateAccount.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.register(fullName, username, password)
        }
    }

    private fun observeViewModel() {
        viewModel.registerResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show()
                finish()
            }
            result.onFailure { error ->
                Toast.makeText(this, error.message ?: "Registrasi gagal", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnCreateAccount.isEnabled = !isLoading && binding.cbTerms.isChecked
            binding.btnCreateAccount.alpha = if (isLoading) 0.5f else if (binding.cbTerms.isChecked) 1.0f else 0.5f
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

    private fun setupPasswordStrengthChecker() {
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                updatePasswordStrength(password)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updatePasswordStrength(password: String) {
        val strength = calculateStrength(password)
        
        // Use default android colors if app colors are not found
        val gray = ContextCompat.getColor(this, android.R.color.darker_gray)

        binding.viewStrength1.setBackgroundColor(gray)
        binding.viewStrength2.setBackgroundColor(gray)
        binding.viewStrength3.setBackgroundColor(gray)

        when (strength) {
            1 -> {
                binding.viewStrength1.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
            2 -> {
                binding.viewStrength1.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                binding.viewStrength2.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            }
            3 -> {
                binding.viewStrength1.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                binding.viewStrength2.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                binding.viewStrength3.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }
        }
    }

    private fun calculateStrength(password: String): Int {
        val length = password.length
        return when {
            length >= 8 -> 3
            length >= 4 -> 2
            length >= 1 -> 1
            else -> 0
        }
    }

    private fun setupTermsCheckbox() {
        binding.btnCreateAccount.isEnabled = false
        binding.cbTerms.setOnCheckedChangeListener { _, isChecked ->
            binding.btnCreateAccount.isEnabled = isChecked
            if (isChecked) {
                binding.btnCreateAccount.alpha = 1.0f
            } else {
                binding.btnCreateAccount.alpha = 0.5f
            }
        }
    }
}
