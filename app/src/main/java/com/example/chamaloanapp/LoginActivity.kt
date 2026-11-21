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
        val btnDemoMember = findViewById<Button>(R.id.btnDemoMember)
        val btnDemoTreasurer = findViewById<Button>(R.id.btnDemoTreasurer)
        val btnGoToRegister = findViewById<Button>(R.id.btnGoToRegister)

        val prefs = getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)

        btnLogin.setOnClickListener {
            val inputIdentifier = etMemberId.text.toString().trim()
            val pin = etPin.text.toString().trim()

            if (inputIdentifier.isEmpty()) {
                tvError.text = "Please enter your Member ID, Treasurer ID, Phone, or Email."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (pin.length != 4) {
                tvError.text = "PIN must be exactly 4 digits."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // 1. Check against locally registered user in SharedPreferences
            val savedMemberId = prefs.getString("member_id", "") ?: ""
            val savedPin = prefs.getString("security_pin", "1234") ?: "1234"
            val savedPhone = prefs.getString("user_phone", "") ?: ""
            val savedEmail = prefs.getString("user_email", "") ?: ""
            val savedRole = prefs.getString("user_role", "MEMBER") ?: "MEMBER"

            val normInputPhone = MpesaParserEngine.normalizePhoneNumber(inputIdentifier)
            val normSavedPhone = MpesaParserEngine.normalizePhoneNumber(savedPhone)

            val isLocalMatch = (savedMemberId.isNotEmpty() && inputIdentifier.equals(savedMemberId, ignoreCase = true)) ||
                    (savedEmail.isNotEmpty() && inputIdentifier.equals(savedEmail, ignoreCase = true)) ||
                    (normSavedPhone.isNotEmpty() && normInputPhone == normSavedPhone)

            // 2. Check against repository roster members
            val repoMember = ChamaLedgerRepository.members.find {
                it.memberId.equals(inputIdentifier, ignoreCase = true) ||
                        it.email.equals(inputIdentifier, ignoreCase = true) ||
                        it.phoneNormalized == normInputPhone
            }

            if (isLocalMatch && (pin == savedPin || pin == "1234")) {
                tvError.visibility = View.GONE
                prefs.edit().putBoolean("is_logged_in", true).apply()
                Toast.makeText(this, "Signed in as $savedRole", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else if (repoMember != null && pin == "1234") {
                tvError.visibility = View.GONE
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("user_role", repoMember.role)
                    .putString("user_name", repoMember.fullName)
                    .putString("user_email", repoMember.email)
                    .putString("user_phone", repoMember.phoneNormalized)
                    .putString("member_id", repoMember.memberId)
                    .putString("group_code", repoMember.groupCode)
                    .putString("chama_name", ChamaLedgerRepository.getChamaName(repoMember.groupCode))
                    .apply()
                Toast.makeText(this, "Signed in as ${repoMember.role}: ${repoMember.fullName}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                tvError.text = "Invalid credentials. Default test PIN is 1234."
                tvError.visibility = View.VISIBLE
            }
        }

        // Demo Login 1: Chama Member (No Treasury Access)
        btnDemoMember.setOnClickListener {
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_role", "MEMBER")
                .putString("user_name", "Eli Ndeda")
                .putString("user_email", "elindeda@example.com")
                .putString("user_phone", "+254 712 345678")
                .putString("member_id", "MBR-0191959")
                .putString("group_code", "CH-4482")
                .putString("chama_name", "Mwangaza Chama")
                .apply()
            Toast.makeText(this, "Logged in as Member: Eli Ndeda (Treasury Access Disabled)", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Demo Login 2: Chama Treasurer (Has Full Treasury & Member Management Access)
        btnDemoTreasurer.setOnClickListener {
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_role", "TREASURER")
                .putString("user_name", "Sarah Mwangi (Treasurer)")
                .putString("user_email", "treasurer@mwangaza.org")
                .putString("user_phone", "+254 722 998877")
                .putString("member_id", "TRS-0191999")
                .putString("group_code", "CH-4482")
                .putString("chama_name", "Mwangaza Chama")
                .apply()
            Toast.makeText(this, "Logged in as Treasurer: Full Admin & Reconciler Access", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
