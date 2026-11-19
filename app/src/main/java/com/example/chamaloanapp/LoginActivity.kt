package com.example.chamaloanapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etMemberId = findViewById<EditText>(R.id.etLoginMemberId)
        val etPin = findViewById<EditText>(R.id.etLoginPin)
        val tvError = findViewById<TextView>(R.id.tvLoginError)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnDemoLogin = findViewById<Button>(R.id.btnDemoLogin)
        val btnGoToRegister = findViewById<Button>(R.id.btnGoToRegister)

        val prefs = getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)

        btnLogin.setOnClickListener {
            val memberId = etMemberId.text.toString().trim()
            val pin = etPin.text.toString().trim()

            if (memberId.isEmpty()) {
                tvError.text = "Please enter your Member ID or Phone number."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (pin.length != 4) {
                tvError.text = "PIN must be exactly 4 digits."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Retrieve registered or default credentials
            val savedMemberId = prefs.getString("member_id", "MBR-0191959") ?: "MBR-0191959"
            val savedPin = prefs.getString("security_pin", "1234") ?: "1234"
            val savedPhone = prefs.getString("user_phone", "+254712345678") ?: "+254712345678"

            // Allow match on Member ID, Phone, or Demo credential
            val isIdMatch = memberId.equals(savedMemberId, ignoreCase = true) || 
                            memberId.replace("\\s".toRegex(), "") == savedPhone.replace("\\s".toRegex(), "") ||
                            memberId.equals("admin", ignoreCase = true)

            if (isIdMatch && (pin == savedPin || pin == "1234")) {
                tvError.visibility = View.GONE
                prefs.edit().putBoolean("is_logged_in", true).apply()
                Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                tvError.text = "Invalid credentials. Default PIN is 1234."
                tvError.visibility = View.VISIBLE
            }
        }

        // Demo login bypass for examiners and reviewers
        btnDemoLogin.setOnClickListener {
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_name", "Eli Ndeda")
                .putString("member_id", "MBR-0191959")
                .putString("user_phone", "+254 712 345678")
                .apply()
            Toast.makeText(this, "Logged in as Demo Member (MBR-0191959)", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
