package com.example.chamaloanapp

import android.animation.ValueAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

class LoanFragment : Fragment() {

    private var selectedTermMonths: Float = 12f
    private var requestedPrincipalKes: Float = 100000f

    private val apiService: ChamaApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChamaApiService::class.java)
    }

    companion object {
        fun newInstance(preset: LoanPreset? = null): LoanFragment {
            val fragment = LoanFragment()
            if (preset != null) {
                val args = Bundle()
                args.putString("label", preset.label)
                args.putString("applicantIncome", preset.applicantIncome)
                args.putString("memberSavings", preset.memberSavings)
                args.putString("coApplicantIncome", preset.coApplicantIncome)
                args.putString("loanAmount", preset.loanAmount)
                args.putString("loanTerm", preset.loanTerm)
                args.putInt("creditHistory", preset.creditHistory)
                args.putInt("propertyArea", preset.propertyArea)
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_loan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Snapshot views
        val tvSnapshotSavings = view.findViewById<TextView>(R.id.tvSnapshotSavings)
        val tvSnapshotLimit = view.findViewById<TextView>(R.id.tvSnapshotLimit)

        // Interactive loan slider & term chips
        val tvLoanAmountLive = view.findViewById<TextView>(R.id.tvLoanAmountLive)
        val sbLoanAmount = view.findViewById<SeekBar>(R.id.sbLoanAmount)
        val tvLiveEmi = view.findViewById<TextView>(R.id.tvLiveEmi)
        val tvLiveInterest = view.findViewById<TextView>(R.id.tvLiveInterest)

        val chipTerm3 = view.findViewById<TextView>(R.id.chipTerm3)
        val chipTerm6 = view.findViewById<TextView>(R.id.chipTerm6)
        val chipTerm12 = view.findViewById<TextView>(R.id.chipTerm12)
        val chipTerm24 = view.findViewById<TextView>(R.id.chipTerm24)

        // Form fields
        val etApplicantIncome = view.findViewById<EditText>(R.id.etApplicantIncome)
        val etMemberSavings = view.findViewById<EditText>(R.id.etMemberSavings)
        val etCoapplicantIncome = view.findViewById<EditText>(R.id.etCoapplicantIncome)
        val rgCreditHistory = view.findViewById<RadioGroup>(R.id.rgCreditHistory)
        val rgPropertyArea = view.findViewById<RadioGroup>(R.id.rgPropertyArea)
        val btnCheckEligibility = view.findViewById<Button>(R.id.btnCheckEligibility)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        // Result card views
        val cardResult = view.findViewById<LinearLayout>(R.id.cardResult)
        val tvStatusBadge = view.findViewById<TextView>(R.id.tvStatusBadge)
        val tvRiskTierBadge = view.findViewById<TextView>(R.id.tvRiskTierBadge)
        val tvScoreNumber = view.findViewById<TextView>(R.id.tvScoreNumber)
        val tvProbabilityText = view.findViewById<TextView>(R.id.tvProbabilityText)
        val pbProbability = view.findViewById<ProgressBar>(R.id.pbProbability)
        val tvEstMonthlyPayment = view.findViewById<TextView>(R.id.tvEstMonthlyPayment)
        val tvTotalInterest = view.findViewById<TextView>(R.id.tvTotalInterest)
        val tvSavingsComplianceChip = view.findViewById<TextView>(R.id.tvSavingsComplianceChip)
        val tvDtiComplianceChip = view.findViewById<TextView>(R.id.tvDtiComplianceChip)
        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)

        // Real-time amortization math calculation
        fun updateRealTimeMath() {
            val r = 0.10 / 12.0 // 10% annual reducing balance
            val n = selectedTermMonths.toDouble()
            val p = requestedPrincipalKes.toDouble()

            val emi = if (n > 0) {
                (p * r * Math.pow(1.0 + r, n) / (Math.pow(1.0 + r, n) - 1.0)).toFloat()
            } else {
                (p / 12.0).toFloat()
            }
            val totalInterest = maxOf(0f, (emi * selectedTermMonths) - requestedPrincipalKes)

            tvLiveEmi.text = String.format(Locale.US, "KES %,.2f", emi)
            tvLiveInterest.text = String.format(Locale.US, "KES %,.2f", totalInterest)

            // Dynamic color cue if requested loan exceeds 3x savings
            val currentSavings = etMemberSavings.text.toString().toFloatOrNull() ?: 40000f
            val maxLimit = currentSavings * 3f
            if (requestedPrincipalKes > maxLimit) {
                tvLoanAmountLive.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rejected))
            } else {
                tvLoanAmountLive.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
            }
        }

        // Setup term chip selection styling
        fun selectTerm(term: Float, selectedChip: TextView) {
            selectedTermMonths = term
            val chips = listOf(chipTerm3, chipTerm6, chipTerm12, chipTerm24)
            chips.forEach { chip ->
                val isCurrent = chip == selectedChip
                chip.setBackgroundResource(if (isCurrent) R.drawable.bg_term_chip_selected else R.drawable.bg_term_chip_unselected)
                chip.setTextColor(if (isCurrent) ContextCompat.getColor(requireContext(), android.R.color.white) else ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            updateRealTimeMath()
        }

        chipTerm3.setOnClickListener { selectTerm(3f, chipTerm3) }
        chipTerm6.setOnClickListener { selectTerm(6f, chipTerm6) }
        chipTerm12.setOnClickListener { selectTerm(12f, chipTerm12) }
        chipTerm24.setOnClickListener { selectTerm(24f, chipTerm24) }

        // Setup SeekBar listener (Live Slider)
        sbLoanAmount.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Minimum loan 5,000 KES, each tick = 1,000 KES
                val loanAmt = maxOf(5000f, progress * 1000f)
                requestedPrincipalKes = loanAmt
                tvLoanAmountLive.text = String.format(Locale.US, "KES %,.0f", loanAmt)
                updateRealTimeMath()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Live savings snapshot update
        etMemberSavings.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val savings = s?.toString()?.toFloatOrNull() ?: 0f
                tvSnapshotSavings.text = String.format(Locale.US, "KES %,.0f", savings)
                tvSnapshotLimit.text = String.format(Locale.US, "KES %,.0f", savings * 3f)
                updateRealTimeMath()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Initial math evaluation
        updateRealTimeMath()

        // Handle pre-filled preset arguments
        arguments?.let { args ->
            etApplicantIncome.setText(args.getString("applicantIncome", "50000"))
            etMemberSavings.setText(args.getString("memberSavings", "40000"))
            etCoapplicantIncome.setText(args.getString("coApplicantIncome", "0"))
            
            val loanAmtStr = args.getString("loanAmount", "100000")
            val loanAmt = loanAmtStr.toFloatOrNull() ?: 100000f
            requestedPrincipalKes = loanAmt
            sbLoanAmount.progress = (loanAmt / 1000f).toInt()
            tvLoanAmountLive.text = String.format(Locale.US, "KES %,.0f", loanAmt)

            val termStr = args.getString("loanTerm", "12")
            when (termStr) {
                "3" -> selectTerm(3f, chipTerm3)
                "6" -> selectTerm(6f, chipTerm6)
                "24" -> selectTerm(24f, chipTerm24)
                else -> selectTerm(12f, chipTerm12)
            }

            when (args.getInt("creditHistory", 0)) {
                0 -> rgCreditHistory.check(R.id.rbCreditClean)
                1 -> rgCreditHistory.check(R.id.rbCreditDefault)
            }
            when (args.getInt("propertyArea", 0)) {
                0 -> rgPropertyArea.check(R.id.rbAreaSemiurban)
                1 -> rgPropertyArea.check(R.id.rbAreaUrban)
                2 -> rgPropertyArea.check(R.id.rbAreaRural)
            }

            val label = args.getString("label", "")
            if (label.isNotEmpty()) {
                Toast.makeText(requireContext(), "Loaded preset: $label", Toast.LENGTH_SHORT).show()
                btnCheckEligibility.post { btnCheckEligibility.performClick() }
            }
        }

        // Submit to AI Underwriting Engine
        btnCheckEligibility.setOnClickListener {
            val incomeStr = etApplicantIncome.text.toString().trim()
            val savingsStr = etMemberSavings.text.toString().trim()
            val coIncomeStr = etCoapplicantIncome.text.toString().trim()

            if (incomeStr.isEmpty()) { etApplicantIncome.error = "Required"; return@setOnClickListener }

            val applicantIncome = incomeStr.toFloatOrNull() ?: 0f
            val coapplicantIncome = coIncomeStr.toFloatOrNull() ?: 0f
            val memberSavings = savingsStr.toFloatOrNull()
            val creditHistory = if (rgCreditHistory.checkedRadioButtonId == R.id.rbCreditClean) 1.0f else 0.0f
            val propertyArea = when (rgPropertyArea.checkedRadioButtonId) {
                R.id.rbAreaSemiurban -> "Semiurban"
                R.id.rbAreaRural -> "Rural"
                else -> "Urban"
            }

            val normalizedLoan = requestedPrincipalKes / 1000f
            val normalizedSavings = memberSavings?.let { if (it >= 1000f) it / 1000f else it }

            val request = LoanApplicationRequest(
                ApplicantIncome = applicantIncome,
                CoapplicantIncome = coapplicantIncome,
                LoanAmount = normalizedLoan,
                MemberSavings = normalizedSavings,
                Loan_Amount_Term = selectedTermMonths,
                Credit_History = creditHistory,
                Property_Area = propertyArea
            )

            progressBar.visibility = View.VISIBLE
            btnCheckEligibility.isEnabled = false
            cardResult.visibility = View.GONE

            apiService.predictLoan(request).enqueue(object : Callback<LoanPredictionResponse> {
                override fun onResponse(call: Call<LoanPredictionResponse>, response: Response<LoanPredictionResponse>) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    btnCheckEligibility.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        cardResult.visibility = View.VISIBLE
                        val ctx = requireContext()

                        val isApproved = res.loan_status.equals("Approved", ignoreCase = true)
                        tvStatusBadge.text = if (isApproved) "APPROVED" else "REJECTED"
                        tvStatusBadge.setBackgroundResource(if (isApproved) R.drawable.bg_badge_approved else R.drawable.bg_badge_rejected)
                        tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, if (isApproved) R.color.status_approved else R.color.status_rejected))

                        tvRiskTierBadge.text = "${res.risk_tier} Profile"
                        val tierColor = when (res.risk_tier.lowercase()) {
                            "low risk" -> R.color.status_approved
                            "medium risk" -> R.color.status_warning
                            else -> R.color.status_rejected
                        }
                        tvRiskTierBadge.setTextColor(ContextCompat.getColor(ctx, tierColor))

                        // Animated score counter (0.0 -> score)
                        val targetScore = res.approval_probability * 10f
                        val animator = ValueAnimator.ofFloat(0f, targetScore)
                        animator.duration = 800
                        animator.addUpdateListener { anim ->
                            val current = anim.animatedValue as Float
                            tvScoreNumber.text = String.format(Locale.US, "%.1f", current)
                        }
                        animator.start()

                        val scoreColor = when {
                            targetScore >= 7f -> ContextCompat.getColor(ctx, R.color.status_approved)
                            targetScore >= 5f -> ContextCompat.getColor(ctx, R.color.status_warning)
                            else -> ContextCompat.getColor(ctx, R.color.status_rejected)
                        }
                        tvScoreNumber.setTextColor(scoreColor)

                        val prob = (res.approval_probability * 100).toInt()
                        pbProbability.progress = prob
                        pbProbability.progressTintList = ContextCompat.getColorStateList(ctx, if (isApproved) R.color.status_approved else R.color.status_rejected)
                        tvProbabilityText.text = "Model Approval Confidence: ${String.format(Locale.US, "%.1f", res.approval_probability * 100)}%"

                        val emi = res.financial_capacity?.estimated_monthly_installment_kes ?: run {
                            val r = 0.10 / 12.0
                            val n = selectedTermMonths.toDouble()
                            val p = requestedPrincipalKes.toDouble()
                            if (n > 0) (p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1)).toFloat() else (p / 12).toFloat()
                        }
                        val totalInterest = maxOf(0f, emi * selectedTermMonths - requestedPrincipalKes)
                        tvEstMonthlyPayment.text = String.format(Locale.US, "KES %,.2f", emi)
                        tvTotalInterest.text = String.format(Locale.US, "KES %,.2f", totalInterest)

                        val savingsKes = memberSavings?.let { if (it < 1000f) it * 1000f else it } ?: 40000f
                        val maxEligible = res.savings_assessment?.max_borrowing_limit_kes ?: (savingsKes * 3f)
                        val savingsOk = res.savings_assessment?.savings_multiplier_compliant ?: (requestedPrincipalKes <= maxEligible)
                        val leverage = if (savingsKes > 0) requestedPrincipalKes / savingsKes else 0f
                        if (savingsOk) {
                            tvSavingsComplianceChip.text = String.format(Locale.US, "• Savings Cap: KES %,.0f vs Max KES %,.0f (%.1fx — Compliant)", requestedPrincipalKes, maxEligible, leverage)
                            tvSavingsComplianceChip.setTextColor(ContextCompat.getColor(ctx, R.color.status_approved))
                        } else {
                            tvSavingsComplianceChip.text = String.format(Locale.US, "• Savings Cap Breached: Excess KES %,.0f (Max KES %,.0f)", requestedPrincipalKes - maxEligible, maxEligible)
                            tvSavingsComplianceChip.setTextColor(ContextCompat.getColor(ctx, R.color.status_rejected))
                        }

                        val householdIncome = applicantIncome + coapplicantIncome
                        val dti = res.financial_capacity?.debt_to_income_percent ?: if (householdIncome > 0) (emi / householdIncome) * 100f else 100f
                        val dtiOk = res.financial_capacity?.cash_flow_viable ?: (dti <= 45f)
                        if (dtiOk) {
                            tvDtiComplianceChip.text = String.format(Locale.US, "• DTI Ratio: %.1f%% of household income (< 45%% — Healthy)", dti)
                            tvDtiComplianceChip.setTextColor(ContextCompat.getColor(ctx, R.color.status_approved))
                        } else {
                            tvDtiComplianceChip.text = String.format(Locale.US, "• DTI Warning: %.1f%% (Exceeds 45%% threshold)", dti)
                            tvDtiComplianceChip.setTextColor(ContextCompat.getColor(ctx, R.color.status_warning))
                        }

                        tvMessage.text = res.committee_action_required ?: res.message ?: "Decision computed based on Chama bylaws."

                    } else {
                        Toast.makeText(requireContext(), "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoanPredictionResponse>, t: Throwable) {
                    if (!isAdded) return
                    progressBar.visibility = View.GONE
                    btnCheckEligibility.isEnabled = true
                    Toast.makeText(requireContext(), "Connection failed: Ensure FastAPI is running on port 8000.", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
