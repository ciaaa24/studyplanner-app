package com.kelompok.studyplanner.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kelompok.studyplanner.data.AuthRepository
import com.kelompok.studyplanner.databinding.ActivityRegisterBinding
import com.kelompok.studyplanner.ui.MainActivity
import com.kelompok.studyplanner.util.applySystemBarsPadding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.btnRegister.setOnClickListener { doRegister() }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun doRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirm.text.toString()

        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirm.error = null

        if (name.isEmpty()) {
            binding.tilName.error = "Nama wajib diisi"
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Masukkan email yang valid"
            return
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Minimal 6 karakter"
            return
        }
        if (password != confirm) {
            binding.tilConfirm.error = "Kata sandi tidak sama"
            return
        }

        setLoading(true)
        AuthRepository.register(name, email, password) { ok, error ->
            setLoading(false)
            if (ok) {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, error ?: "Gagal mendaftar", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }
}
