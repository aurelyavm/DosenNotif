package com.example.dosennotif.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dosennotif.R
import com.example.dosennotif.databinding.ActivityRegisterBinding
import com.example.dosennotif.model.User

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Set up click listeners
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvLoginPrompt.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val name = binding.etName.text.toString().trim()
        val nidn = binding.etNidn.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Validate input fields
        if (name.isEmpty()) {
            binding.etName.error = getString(R.string.field_required)
            binding.etName.requestFocus()
            return
        }

        if (nidn.isEmpty()) {
            binding.etNidn.error = getString(R.string.field_required)
            binding.etNidn.requestFocus()
            return
        }

        if (email.isEmpty()) {
            binding.etEmail.error = getString(R.string.field_required)
            binding.etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = getString(R.string.invalid_email)
            binding.etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = getString(R.string.field_required)
            binding.etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.etPassword.error = getString(R.string.password_too_short)
            binding.etPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = getString(R.string.field_required)
            binding.etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = getString(R.string.password_mismatch)
            binding.etConfirmPassword.requestFocus()
            return
        }

        // Show progress
        binding.progressBar.visibility = View.VISIBLE

        // ===== TAMBAHAN: Cek apakah NIDN sudah ada =====
        checkNidnAvailability(nidn, name, email, password)
    }

    private fun checkNidnAvailability(nidn: String, name: String, email: String, password: String) {
        firestore.collection("users")
            .whereEqualTo("nidn", nidn)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // NIDN sudah ada
                    binding.progressBar.visibility = View.GONE
                    binding.etNidn.error = getString(R.string.nidn_already_registered)
                    binding.etNidn.requestFocus()
                    Toast.makeText(
                        this,
                        getString(R.string.nidn_already_exists),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // NIDN tersedia, lanjutkan registrasi
                    createUserAccount(nidn, name, email, password)
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this,
                    "Gagal memeriksa NIDN: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun createUserAccount(nidn: String, name: String, email: String, password: String) {
        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Save additional user data to Firestore
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    val user = User(
                        id = userId,
                        name = name,
                        email = email,
                        nidn = nidn,
                        createdAt = System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this,
                                getString(R.string.registration_success),
                                Toast.LENGTH_SHORT
                            ).show()

                            // Redirect to login
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            binding.progressBar.visibility = View.GONE
                            // Hapus akun yang sudah dibuat jika gagal simpan data
                            auth.currentUser?.delete()
                            Toast.makeText(
                                this,
                                getString(R.string.error_saving_user_data, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    binding.progressBar.visibility = View.GONE
                    val errorMessage = when (task.exception?.message) {
                        "The email address is already in use by another account." ->
                            "Email sudah digunakan oleh akun lain"
                        "The email address is badly formatted." ->
                            "Format email tidak valid"
                        else -> task.exception?.message ?: "Terjadi kesalahan"
                    }
                    Toast.makeText(
                        this,
                        "Registrasi gagal: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}