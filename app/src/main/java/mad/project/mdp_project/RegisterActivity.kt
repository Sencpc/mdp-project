package mad.project.mdp_project

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.User
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

        val db = AppDatabase.getDatabase(this)
        val userDao = db.userDao()

        binding.btnCreateAccount.setOnClickListener {
            val fullName = binding.etFullName.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 8) {
                Toast.makeText(this, "Password minimal 8 karakter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = userDao.getUserByUsername(username)
                if (existingUser != null) {
                    Toast.makeText(this@RegisterActivity, "Username sudah digunakan", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = User(
                        username = username,
                        password = password,
                        fullName = fullName
                    )
                    userDao.insertUser(newUser)
                    Toast.makeText(this@RegisterActivity, "Registrasi Berhasil!", Toast.LENGTH_SHORT).show()
                    finish()
                }
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
