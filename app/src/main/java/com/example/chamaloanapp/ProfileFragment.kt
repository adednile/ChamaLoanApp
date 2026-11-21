package com.example.chamaloanapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)

        val etProfileName = view.findViewById<EditText>(R.id.etProfileName)
        val etProfilePhone = view.findViewById<EditText>(R.id.etProfilePhone)
        val etProfileEmail = view.findViewById<EditText>(R.id.etProfileEmail)
        val etProfileNationalId = view.findViewById<EditText>(R.id.etProfileNationalId)
        val tvProfileFullName = view.findViewById<TextView>(R.id.tvProfileFullName)
        val tvProfileMemberId = view.findViewById<TextView>(R.id.tvProfileMemberId)
        val tvProfileChamaName = view.findViewById<TextView>(R.id.tvProfileChamaName)
        val tvProfileRoleBadge = view.findViewById<TextView>(R.id.tvProfileRoleBadge)
        val tvProfileAffiliation = view.findViewById<TextView>(R.id.tvProfileAffiliation)
        val tvAvatarInitials = view.findViewById<TextView>(R.id.tvAvatarInitials)
        val btnSaveProfile = view.findViewById<Button>(R.id.btnSaveProfile)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // Load saved profile values (no hardcoded fallback email)
        val savedName = prefs.getString("user_name", "User") ?: "User"
        val savedPhone = prefs.getString("user_phone", "") ?: ""
        val savedEmail = prefs.getString("user_email", "") ?: ""
        val savedMemberId = prefs.getString("member_id", "MBR-XXXX") ?: "MBR-XXXX"
        val savedRole = prefs.getString("user_role", "MEMBER") ?: "MEMBER"
        val savedChama = prefs.getString("chama_name", "Mwangaza Self-Help Group") ?: "Mwangaza Self-Help Group"
        val savedNationalId = prefs.getString("national_id", "") ?: ""

        etProfileName.setText(savedName)
        etProfilePhone.setText(savedPhone)
        etProfileEmail.setText(savedEmail)
        etProfileNationalId?.setText(savedNationalId)
        tvProfileFullName.text = savedName
        tvProfileMemberId?.text = "ID: $savedMemberId"
        tvProfileChamaName?.text = savedChama
        tvProfileAffiliation?.text = savedChama
        tvProfileRoleBadge?.text = "ROLE: $savedRole"

        fun updateAvatar(name: String) {
            val parts = name.trim().split(" ")
            val initials = if (parts.size >= 2) {
                "${parts[0].firstOrNull() ?: 'U'}${parts[1].firstOrNull() ?: 'M'}".uppercase()
            } else {
                name.take(2).uppercase().ifEmpty { "EN" }
            }
            tvAvatarInitials.text = initials
        }
        updateAvatar(savedName)

        btnSaveProfile.setOnClickListener {
            val newName = etProfileName.text.toString().trim()
            val newPhone = etProfilePhone.text.toString().trim()
            val newEmail = etProfileEmail.text.toString().trim()

            if (newName.isEmpty()) {
                etProfileName.error = "Name cannot be empty"
                return@setOnClickListener
            }

            prefs.edit()
                .putString("user_name", newName)
                .putString("user_phone", newPhone)
                .putString("user_email", newEmail)
                .apply()

            tvProfileFullName.text = newName
            updateAvatar(newName)

            Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            prefs.edit().putBoolean("is_logged_in", false).apply()
            Toast.makeText(requireContext(), "Signed out successfully.", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
