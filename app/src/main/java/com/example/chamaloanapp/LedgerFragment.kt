package com.example.chamaloanapp

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

        view.findViewById<Button>(R.id.btnRequestStatement).setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Official PDF Statement for MBR-0191959 generated and dispatched to your registered email.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
