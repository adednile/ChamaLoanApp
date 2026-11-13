package com.example.chamaloanapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class TreasurerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_treasurer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navigator = activity as? MainActivity

        view.findViewById<Button>(R.id.btnPresetPrime).setOnClickListener {
            navigator?.navigateToLoanWithPreset(LoanPresets.PRIME)
        }
        view.findViewById<Button>(R.id.btnPresetGuarantor).setOnClickListener {
            navigator?.navigateToLoanWithPreset(LoanPresets.GUARANTOR)
        }
        view.findViewById<Button>(R.id.btnPresetCapExceeded).setOnClickListener {
            navigator?.navigateToLoanWithPreset(LoanPresets.CAP_EXCEEDED)
        }
        view.findViewById<Button>(R.id.btnPresetDefault).setOnClickListener {
            navigator?.navigateToLoanWithPreset(LoanPresets.BAD_CREDIT)
        }
    }
}
