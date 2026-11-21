package com.example.chamaloanapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
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
import androidx.appcompat.app.AlertDialog
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
        val prefs = requireContext().getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)

        val currentChamaName = prefs.getString("chama_name", "Mwangaza Chama") ?: "Mwangaza Chama"
        val currentGroupCode = prefs.getString("group_code", "CH-4482") ?: "CH-4482"

        // Headers
        view.findViewById<TextView>(R.id.tvTreasuryHeaderSubtitle)?.text =
            "TREASURY CONSOLE • ${currentChamaName.uppercase(Locale.US)}"
        view.findViewById<TextView>(R.id.tvTreasuryChamaName)?.text = currentChamaName
        view.findViewById<TextView>(R.id.tvTreasuryGroupCode)?.text = currentGroupCode

        // Liquidity stats
        val tvGroupPool = view.findViewById<TextView>(R.id.tvTreasuryGroupPool)
        val tvActiveDisbursed = view.findViewById<TextView>(R.id.tvTreasuryActiveDisbursed)

        fun updateLiquidityDisplay() {
            tvGroupPool.text = String.format(Locale.US, "KES %,.0f", ChamaLedgerRepository.groupTotalSavingsPoolKes)
            tvActiveDisbursed.text = String.format(Locale.US, "KES %,.0f", ChamaLedgerRepository.groupActiveDisbursedKes)
        }
        updateLiquidityDisplay()

        // ── 1. Share & Copy Chama Code ──
        val btnShareGroupCode = view.findViewById<Button>(R.id.btnShareGroupCode)
        val btnCopyGroupCode = view.findViewById<Button>(R.id.btnCopyGroupCode)

        btnShareGroupCode?.setOnClickListener {
            val shareText = "Join our Chama ($currentChamaName) on ChamaLoanApp! Use Group Code: $currentGroupCode during registration."
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Chama Code")
            startActivity(shareIntent)
        }

        btnCopyGroupCode?.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Chama Code", currentGroupCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Chama Code $currentGroupCode copied to clipboard!", Toast.LENGTH_SHORT).show()
        }

        // ── 2. Member Management & Roster ──
        val layoutChamaMemberList = view.findViewById<LinearLayout>(R.id.layoutChamaMemberList)
        val tvMemberCountBadge = view.findViewById<TextView>(R.id.tvMemberCountBadge)
        val tvEmptyMembersPlaceholder = view.findViewById<TextView>(R.id.tvEmptyMembersPlaceholder)

        // Spinners
        val spnTargetMember = view.findViewById<Spinner>(R.id.spnTargetMember)
        val spnTransactionType = view.findViewById<Spinner>(R.id.spnTransactionType)

        val typeDisplayList = TransactionType.values().map { it.label }
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, typeDisplayList)
        spnTransactionType.adapter = typeAdapter

        fun refreshMemberSpinner() {
            val memberDisplayList = ChamaLedgerRepository.members.map { "${it.fullName} (${it.memberId})" }
            val memberAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, memberDisplayList)
            spnTargetMember.adapter = memberAdapter
        }
        refreshMemberSpinner()

        fun refreshMemberList() {
            layoutChamaMemberList?.removeAllViews()
            val members = ChamaLedgerRepository.getMembersForGroup(currentGroupCode)
            tvMemberCountBadge?.text = "${members.size} MEMBERS"

            if (members.isEmpty()) {
                tvEmptyMembersPlaceholder?.visibility = View.VISIBLE
            } else {
                tvEmptyMembersPlaceholder?.visibility = View.GONE

                members.forEach { member ->
                    // Card container for member
                    val memberCard = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(R.drawable.bg_card_white)
                        elevation = 2f
                        setPadding(32, 28, 32, 28)
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        params.bottomMargin = 24
                        layoutParams = params
                    }

                    // Row 1: Name, ID and Remove button
                    val headerRow = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    // Avatar Circle
                    val avatar = TextView(requireContext()).apply {
                        val dpSize = (40 * resources.displayMetrics.density).toInt()
                        layoutParams = LinearLayout.LayoutParams(dpSize, dpSize).apply {
                            marginEnd = (12 * resources.displayMetrics.density).toInt()
                        }
                        setBackgroundResource(R.drawable.bg_score_circle)
                        gravity = Gravity.CENTER
                        val parts = member.fullName.trim().split(" ")
                        text = if (parts.size >= 2) "${parts[0].first()}${parts[1].first()}".uppercase() else member.fullName.take(2).uppercase()
                        setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    val infoCol = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    val tvName = TextView(requireContext()).apply {
                        text = member.fullName
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        textSize = 14f
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    val tvMeta = TextView(requireContext()).apply {
                        val cleanPhone = if (member.phoneNormalized.startsWith("254")) "+${member.phoneNormalized}" else member.phoneNormalized
                        text = "${member.memberId} • $cleanPhone"
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        textSize = 11f
                    }

                    val tvEmail = TextView(requireContext()).apply {
                        text = member.email.ifEmpty { "No email registered" }
                        setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                        textSize = 10f
                    }

                    infoCol.addView(tvName)
                    infoCol.addView(tvMeta)
                    infoCol.addView(tvEmail)

                    // Remove Member Button
                    val btnRemove = Button(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            (36 * resources.displayMetrics.density).toInt()
                        )
                        text = "Remove"
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(context, R.color.status_rejected))
                        backgroundTintList = ContextCompat.getColorStateList(context, R.color.status_rejected_bg)
                        setOnClickListener {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Remove Member?")
                                .setMessage("Are you sure you want to remove ${member.fullName} (${member.memberId}) from $currentChamaName?\n\nThis will revoke their membership from this Chama.")
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton("Remove") { _, _ ->
                                    val removed = ChamaLedgerRepository.removeMember(member.memberId)
                                    if (removed) {
                                        Toast.makeText(requireContext(), "${member.fullName} was removed from the Chama.", Toast.LENGTH_SHORT).show()
                                        refreshMemberList()
                                        refreshMemberSpinner()
                                    }
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }

                    headerRow.addView(avatar)
                    headerRow.addView(infoCol)
                    headerRow.addView(btnRemove)

                    // Row 2: Financial Stats
                    val statsRow = LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(0, 16, 0, 0)
                    }

                    val tvSavings = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        text = String.format(Locale.US, "Savings: KES %,.0f", member.totalSavingsKes)
                        setTextColor(ContextCompat.getColor(context, R.color.status_approved))
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    val tvLoans = TextView(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        text = String.format(Locale.US, "Active Loan: KES %,.0f", member.activeLoanBalanceKes)
                        setTextColor(ContextCompat.getColor(context, if (member.activeLoanBalanceKes > 0) R.color.status_warning else R.color.text_muted))
                        textSize = 11f
                        typeface = Typeface.DEFAULT_BOLD
                        gravity = Gravity.END
                    }

                    statsRow.addView(tvSavings)
                    statsRow.addView(tvLoans)

                    memberCard.addView(headerRow)
                    memberCard.addView(statsRow)

                    layoutChamaMemberList.addView(memberCard)
                }
            }
        }
        refreshMemberList()

        // ── 3. M-Pesa SMS Parser Views ──
        val etMpesaSmsInput = view.findViewById<EditText>(R.id.etMpesaSmsInput)
        val btnSampleEliDeposit = view.findViewById<Button>(R.id.btnSampleEliDeposit)
        val btnSampleAlexRepay = view.findViewById<Button>(R.id.btnSampleAlexRepay)
        val btnParseMpesaSms = view.findViewById<Button>(R.id.btnParseMpesaSms)

        // Parsed output views
        val layoutParsedResult = view.findViewById<LinearLayout>(R.id.layoutParsedResult)
        val tvParsedCode = view.findViewById<TextView>(R.id.tvParsedCode)
        val tvParsedAmount = view.findViewById<TextView>(R.id.tvParsedAmount)
        val tvParsedSender = view.findViewById<TextView>(R.id.tvParsedSender)
        val btnCommitTransaction = view.findViewById<Button>(R.id.btnCommitTransaction)

        // Dynamic log list
        val layoutReconciledList = view.findViewById<LinearLayout>(R.id.layoutReconciledList)
        val tvEmptyPlaceholder = view.findViewById<TextView>(R.id.tvEmptyReconcilePlaceholder)

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

            // Refresh member directory display to reflect updated balance
            refreshMemberList()

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
