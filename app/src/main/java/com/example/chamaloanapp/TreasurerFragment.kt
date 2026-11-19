package com.example.chamaloanapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.util.Locale

class TreasurerFragment : Fragment() {

    private var currentParsedTx: ParsedMpesaTransaction? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_treasurer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navigator = activity as? MainActivity

        // Liquidity stats
        val tvGroupPool = view.findViewById<TextView>(R.id.tvTreasuryGroupPool)
        val tvActiveDisbursed = view.findViewById<TextView>(R.id.tvTreasuryActiveDisbursed)

        // Parser views
        val etMpesaSmsInput = view.findViewById<EditText>(R.id.etMpesaSmsInput)
        val btnSampleEliDeposit = view.findViewById<Button>(R.id.btnSampleEliDeposit)
        val btnSampleAlexRepay = view.findViewById<Button>(R.id.btnSampleAlexRepay)
        val btnParseMpesaSms = view.findViewById<Button>(R.id.btnParseMpesaSms)

        // Parsed output views
        val layoutParsedResult = view.findViewById<LinearLayout>(R.id.layoutParsedResult)
        val tvParsedCode = view.findViewById<TextView>(R.id.tvParsedCode)
        val tvParsedAmount = view.findViewById<TextView>(R.id.tvParsedAmount)
        val tvParsedSender = view.findViewById<TextView>(R.id.tvParsedSender)
        val spnTargetMember = view.findViewById<Spinner>(R.id.spnTargetMember)
        val spnTransactionType = view.findViewById<Spinner>(R.id.spnTransactionType)
        val btnCommitTransaction = view.findViewById<Button>(R.id.btnCommitTransaction)

        // Dynamic log list
        val layoutReconciledList = view.findViewById<LinearLayout>(R.id.layoutReconciledList)
        val tvEmptyPlaceholder = view.findViewById<TextView>(R.id.tvEmptyReconcilePlaceholder)

        fun updateLiquidityDisplay() {
            tvGroupPool.text = String.format(Locale.US, "KES %,.0f", ChamaLedgerRepository.groupTotalSavingsPoolKes)
            tvActiveDisbursed.text = String.format(Locale.US, "KES %,.0f", ChamaLedgerRepository.groupActiveDisbursedKes)
        }
        updateLiquidityDisplay()

        // Setup Spinners
        val memberDisplayList = ChamaLedgerRepository.members.map { "${it.fullName} (${it.memberId})" }
        val memberAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, memberDisplayList)
        spnTargetMember.adapter = memberAdapter

        val typeDisplayList = TransactionType.values().map { it.label }
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, typeDisplayList)
        spnTransactionType.adapter = typeAdapter

        // Quick sample templates
        btnSampleEliDeposit.setOnClickListener {
            etMpesaSmsInput.setText(
                "QK99248M Confirmed. Ksh5,000.00 received from ELI NDEDA 0712345678 on 19/11/26 at 2:45 PM. New M-PESA balance is Ksh 142,500.00."
            )
        }

        btnSampleAlexRepay.setOnClickListener {
            etMpesaSmsInput.setText(
                "RK44102B Confirmed. Ksh10,000.00 received from ALEX MUTITU 0722123456 on 19/11/26 at 11:20 AM. New M-PESA balance is Ksh 152,500.00."
            )
        }

        // Parsing action
        btnParseMpesaSms.setOnClickListener {
            val raw = etMpesaSmsInput.text.toString().trim()
            if (raw.isEmpty()) {
                etMpesaSmsInput.error = "Please paste an M-Pesa SMS text first."
                return@setOnClickListener
            }

            val parsed = MpesaParserEngine.parseSms(raw)
            if (parsed == null) {
                Toast.makeText(requireContext(), "Could not parse M-Pesa message. Ensure it contains a valid Ksh amount.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            currentParsedTx = parsed

            // Attempt automatic member matching
            val matchedMember = ChamaLedgerRepository.matchMember(parsed.senderPhone, parsed.senderName)
            parsed.matchedMember = matchedMember

            tvParsedCode.text = parsed.transactionCode
            tvParsedAmount.text = String.format(Locale.US, "KES %,.2f", parsed.amountKes)
            tvParsedSender.text = "${parsed.senderName} (${parsed.senderPhone.ifEmpty { "Phone hidden" }})"

            // Select matched member in spinner
            if (matchedMember != null) {
                val index = ChamaLedgerRepository.members.indexOf(matchedMember)
                if (index >= 0) spnTargetMember.setSelection(index)
            }

            // Auto-select transaction category based on member loan status
            if (matchedMember != null && matchedMember.activeLoanBalanceKes > 0) {
                spnTransactionType.setSelection(TransactionType.LOAN_REPAYMENT.ordinal)
            } else {
                spnTransactionType.setSelection(TransactionType.SHARE_CONTRIBUTION.ordinal)
            }

            layoutParsedResult.visibility = View.VISIBLE
            Toast.makeText(
                requireContext(),
                if (matchedMember != null) "✓ Auto-matched to: ${matchedMember.fullName}" else "M-Pesa parsed! Please confirm member from dropdown.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Commit and reconcile transaction
        btnCommitTransaction.setOnClickListener {
            val tx = currentParsedTx ?: return@setOnClickListener
            val selectedMemberIndex = spnTargetMember.selectedItemPosition
            if (selectedMemberIndex < 0 || selectedMemberIndex >= ChamaLedgerRepository.members.size) return@setOnClickListener

            val targetMember = ChamaLedgerRepository.members[selectedMemberIndex]
            val selectedType = TransactionType.values()[spnTransactionType.selectedItemPosition]

            // Apply ledger update
            ChamaLedgerRepository.applyTransaction(tx, targetMember, selectedType)

            // Update liquidity figures
            updateLiquidityDisplay()

            // Reset parser form
            layoutParsedResult.visibility = View.GONE
            etMpesaSmsInput.text.clear()
            currentParsedTx = null

            // Add item to dynamic transaction log
            tvEmptyPlaceholder.visibility = View.GONE

            val logItemView = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_2, layoutReconciledList, false)
            val text1 = logItemView.findViewById<TextView>(android.R.id.text1)
            val text2 = logItemView.findViewById<TextView>(android.R.id.text2)

            text1.text = "${tx.transactionCode} • ${targetMember.fullName} (+KES %,.0f)".format(tx.amountKes)
            text1.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            text1.textSize = 12f

            text2.text = "Type: ${selectedType.label} • New Savings: KES %,.0f".format(targetMember.totalSavingsKes)
            text2.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
            text2.textSize = 10f

            layoutReconciledList.addView(logItemView, 0)

            Toast.makeText(
                requireContext(),
                "Posted KES %,.0f to ${targetMember.fullName}'s ${selectedType.label}!".format(tx.amountKes),
                Toast.LENGTH_LONG
            ).show()
        }

        // Examiner Preset Demonstration Buttons
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
