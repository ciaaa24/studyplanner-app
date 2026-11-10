package com.kelompok.studyplanner.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kelompok.studyplanner.data.AuthRepository
import com.kelompok.studyplanner.databinding.ActivityLoginBinding
import com.kelompok.studyplanner.ui.MainActivity
import com.kelompok.studyplanner.util.applySystemBarsPadding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sudah login sebelumnya? Langsung ke layar utama (Firebase menyimpan sesi sendiri).
        if (AuthRepository.isLoggedIn) {
            goToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Masukkan email yang valid"
            return
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = "Kata sandi wajib diisi"
            return
        }

        setLoading(true)
        AuthRepository.login(email, password) { ok, error ->
            setLoading(false)
            if (ok) {
                goToMain()
            } else {
                Toast.makeText(this, error ?: "Gagal masuk", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
