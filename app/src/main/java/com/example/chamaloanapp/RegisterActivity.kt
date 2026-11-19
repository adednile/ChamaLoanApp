package com.example.chamaloanapp

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etRegisterName)
        val etPhone = findViewById<EditText>(R.id.etRegisterPhone)
        val etNationalId = findViewById<EditText>(R.id.etRegisterNationalId)
        val etGroupCode = findViewById<EditText>(R.id.etRegisterGroupCode)
        val etPin = findViewById<EditText>(R.id.etRegisterPin)
        val etPinConfirm = findViewById<EditText>(R.id.etRegisterPinConfirm)
        val tvError = findViewById<TextView>(R.id.tvRegisterError)
        val btnSubmit = findViewById<Button>(R.id.btnRegisterSubmit)
        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)

        val prefs = getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)

        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val nationalId = etNationalId.text.toString().trim()
            val groupCode = etGroupCode.text.toString().trim()
            val pin = etPin.text.toString().trim()
            val pinConfirm = etPinConfirm.text.toString().trim()

            if (name.isEmpty()) {
                tvError.text = "Please enter your full legal name."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (phone.isEmpty()) {
                tvError.text = "Please enter your M-Pesa registered phone number."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (nationalId.length < 6) {
                tvError.text = "Please enter a valid National ID number."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (groupCode.isEmpty()) {
                tvError.text = "Please enter your Chama group code (e.g. CH-4482)."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (pin.length != 4) {
                tvError.text = "Security PIN must be exactly 4 digits."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (pin != pinConfirm) {
                tvError.text = "PINs do not match. Please re-enter."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Generate an official Member ID format
            val generatedMemberId = "MBR-0" + (100000..999999).random().toString().take(6)

            prefs.edit()
                .putString("user_name", name)
                .putString("user_phone", phone)
                .putString("national_id", nationalId)
                .putString("group_code", groupCode)
                .putString("security_pin", pin)
                .putString("member_id", generatedMemberId)
                .putBoolean("is_logged_in", false)
                .apply()

            tvError.visibility = View.GONE
            Toast.makeText(this, "Account created! Member ID: $generatedMemberId. Please sign in.", Toast.LENGTH_LONG).show()
            finish()
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}
