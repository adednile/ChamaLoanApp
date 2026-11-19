package com.example.chamaloanapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hold splash screen for 1.8 seconds then route to appropriate screen
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)

            val intent = if (isLoggedIn) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 1800)
    }
}
