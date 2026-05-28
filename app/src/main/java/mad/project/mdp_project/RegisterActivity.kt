package mad.project.mdp_project

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import mad.project.mdp_project.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.ivBack.setOnClickListener { finish() }
        
        binding.tvLogin.setOnClickListener {
            finish()
        }

        setupPasswordVisibilityToggle()
        setupPasswordStrengthChecker()
        setupTermsCheckbox()

        binding.btnCreateAccount.setOnClickListener {
            // Implement registration logic
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
        
        // Reset bars
        binding.viewStrength1.setBackgroundColor(ContextCompat.getColor(this, R.color.edit_text_bg))
        binding.viewStrength2.setBackgroundColor(ContextCompat.getColor(this, R.color.edit_text_bg))
        binding.viewStrength3.setBackgroundColor(ContextCompat.getColor(this, R.color.edit_text_bg))

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
        if (password.isEmpty()) return 0
        if (password.length < 6) return 1
        
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isDigit() } && password.any { it.isLetter() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        
        return when {
            score >= 3 -> 3
            score >= 1 -> 2
            else -> 1
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
