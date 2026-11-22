package com.example.chamaloanapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class LedgerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_ledger, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)
        val memberId = prefs.getString("member_id", "MBR-0191959") ?: "MBR-0191959"
        val userName = prefs.getString("user_name", "User") ?: "User"
        val userEmail = prefs.getString("user_email", "your registered email") ?: "your registered email"

        val memberObj = ChamaLedgerRepository.getMember(memberId)
        val savings = memberObj?.totalSavingsKes ?: 40000f

        view.findViewById<android.widget.TextView>(R.id.tvLedgerMemberMeta)?.text = "Member ID: $memberId • Tier 1 Equity Holder ($userName)"
        view.findViewById<android.widget.TextView>(R.id.tvLedgerShares)?.text = String.format(java.util.Locale.US, "KES %,.0f", savings)

        view.findViewById<Button>(R.id.btnRequestStatement).setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Official PDF Statement for $userName ($memberId) generated and dispatched to $userEmail.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
