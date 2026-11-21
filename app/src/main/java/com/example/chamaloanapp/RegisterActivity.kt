package com.example.chamaloanapp

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val rgRole = findViewById<RadioGroup>(R.id.rgRegisterRole)
        val layoutTreasurerFields = findViewById<LinearLayout>(R.id.layoutTreasurerFields)
        val tvLabelGroupCode = findViewById<TextView>(R.id.tvLabelGroupCode)
        val tvGroupCodeHint = findViewById<TextView>(R.id.tvGroupCodeHint)

        val etName = findViewById<EditText>(R.id.etRegisterName)
        val etEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etPhone = findViewById<EditText>(R.id.etRegisterPhone)
        val etNationalId = findViewById<EditText>(R.id.etRegisterNationalId)
        val etChamaName = findViewById<EditText>(R.id.etRegisterChamaName)
        val etGroupCode = findViewById<EditText>(R.id.etRegisterGroupCode)
        val etPin = findViewById<EditText>(R.id.etRegisterPin)
        val etPinConfirm = findViewById<EditText>(R.id.etRegisterPinConfirm)
        val tvError = findViewById<TextView>(R.id.tvRegisterError)
        val btnSubmit = findViewById<Button>(R.id.btnRegisterSubmit)
        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)

        val prefs = getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)

        // Helper: Generate a unique random Chama Code for Treasurers e.g. "CH-7392"
        fun generateNewChamaCode(): String {
            return "CH-" + (1000..9999).random()
        }

        // Handle role switcher change
        rgRole.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbRoleTreasurer) {
                layoutTreasurerFields.visibility = View.VISIBLE
                tvLabelGroupCode.text = "Chama Group Code (Created for your members) *"
                tvGroupCodeHint.text = "You will share this code with your members so they can join your Chama."
                btnSubmit.text = "Create Chama & Register as Treasurer"
                if (etGroupCode.text.isNullOrEmpty() || etGroupCode.text.toString() == "CH-4482") {
                    etGroupCode.setText(generateNewChamaCode())
                }
            } else {
                layoutTreasurerFields.visibility = View.GONE
                tvLabelGroupCode.text = "Chama Group Code (from your Treasurer) *"
                tvGroupCodeHint.text = "Ask your Chama Treasurer for your 6-character group code."
                btnSubmit.text = "Join Chama as Member"
                if (etGroupCode.text.isNullOrEmpty()) {
                    etGroupCode.setText("CH-4482") // Default demo Chama code
                }
            }
        }

        btnSubmit.setOnClickListener {
            val isTreasurer = rgRole.checkedRadioButtonId == R.id.rbRoleTreasurer
            val roleStr = if (isTreasurer) "TREASURER" else "MEMBER"

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val nationalId = etNationalId.text.toString().trim()
            val chamaName = if (isTreasurer) etChamaName.text.toString().trim() else ""
            val groupCode = etGroupCode.text.toString().trim().uppercase(Locale.US)
            val pin = etPin.text.toString().trim()
            val pinConfirm = etPinConfirm.text.toString().trim()

            // 1. Full Name validation
            if (name.isEmpty()) {
                tvError.text = "Please enter your full legal name."
                tvError.visibility = View.VISIBLE
                etName.requestFocus()
                return@setOnClickListener
            }

            // 2. Email Validation (Unique, no defaults)
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvError.text = "Please enter a valid unique email address."
                tvError.visibility = View.VISIBLE
                etEmail.requestFocus()
                return@setOnClickListener
            }

            // 3. Phone validation
            if (phone.isEmpty() || phone.length < 9) {
                tvError.text = "Please enter your M-Pesa registered phone number."
                tvError.visibility = View.VISIBLE
                etPhone.requestFocus()
                return@setOnClickListener
            }

            // 4. National ID
            if (nationalId.length < 6) {
                tvError.text = "Please enter a valid National ID number (at least 6 digits)."
                tvError.visibility = View.VISIBLE
                etNationalId.requestFocus()
                return@setOnClickListener
            }

            // 5. Role-specific validation
            if (isTreasurer) {
                if (chamaName.isEmpty()) {
                    tvError.text = "Please enter a name for your Chama group."
                    tvError.visibility = View.VISIBLE
                    etChamaName.requestFocus()
                    return@setOnClickListener
                }
                if (groupCode.isEmpty()) {
                    tvError.text = "Please generate or enter a Chama group code."
                    tvError.visibility = View.VISIBLE
                    etGroupCode.requestFocus()
                    return@setOnClickListener
                }
                // Register the new Chama group into repository
                ChamaLedgerRepository.registerChama(groupCode, chamaName)
            } else {
                if (groupCode.isEmpty()) {
                    tvError.text = "Please enter your Chama group code from your Treasurer."
                    tvError.visibility = View.VISIBLE
                    etGroupCode.requestFocus()
                    return@setOnClickListener
                }
                // Allow known codes or accept entered code
                if (!ChamaLedgerRepository.isValidGroupCode(groupCode)) {
                    ChamaLedgerRepository.registerChama(groupCode, "Chama Group ($groupCode)")
                }
            }

            // 6. Security PIN validation
            if (pin.length != 4) {
                tvError.text = "Security PIN must be exactly 4 digits."
                tvError.visibility = View.VISIBLE
                etPin.requestFocus()
                return@setOnClickListener
            }
            if (pin != pinConfirm) {
                tvError.text = "PINs do not match. Please re-enter."
                tvError.visibility = View.VISIBLE
                etPinConfirm.requestFocus()
                return@setOnClickListener
            }

            // 7. Generate official Member / Treasurer ID
            val prefix = if (isTreasurer) "TRS-" else "MBR-"
            val generatedId = prefix + (100000..999999).random().toString().take(6)
            val resolvedChamaName = if (isTreasurer) chamaName else ChamaLedgerRepository.getChamaName(groupCode)
            val normalizedPhone = MpesaParserEngine.normalizePhoneNumber(phone)

            // Register into Shared In-Memory Repository
            val newMember = ChamaMember(
                memberId = generatedId,
                fullName = name,
                phoneNormalized = normalizedPhone,
                email = email,
                groupCode = groupCode,
                role = roleStr,
                totalSavingsKes = if (isTreasurer) 50000f else 10000f,
                activeLoanBalanceKes = 0f
            )
            ChamaLedgerRepository.addMember(newMember)

            // Persist to SharedPreferences
            prefs.edit()
                .putString("user_role", roleStr)
                .putString("user_name", name)
                .putString("user_email", email)
                .putString("user_phone", phone)
                .putString("national_id", nationalId)
                .putString("group_code", groupCode)
                .putString("chama_name", resolvedChamaName)
                .putString("security_pin", pin)
                .putString("member_id", generatedId)
                .putBoolean("is_logged_in", false)
                .apply()

            tvError.visibility = View.GONE

            // Success dialog showing ID and Group Code
            val msg = if (isTreasurer) {
                "Treasurer Account Created!\n\nYour ID: $generatedId\nChama Code: $groupCode\n\nShare this code ($groupCode) with your members so they can join your Chama during registration."
            } else {
                "Member Account Created!\n\nYour Member ID: $generatedId\nChama: $resolvedChamaName\n\nPlease sign in with your ID and PIN."
            }

            AlertDialog.Builder(this)
                .setTitle("Registration Successful")
                .setMessage(msg)
                .setPositiveButton("Proceed to Sign In") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }
}
