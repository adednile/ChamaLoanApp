package com.example.chamaloanapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.Locale

// 1. Data classes matching your FastAPI schema
data class LoanApplicationRequest(
    val Gender: String = "Male",
    val Married: String = "Yes",
    val Dependents: Int = 0,
    val Education: String = "Graduate",
    val Self_Employed: String = "No",
    val ApplicantIncome: Float,
    val CoapplicantIncome: Float = 0f,
    val LoanAmount: Float,
    val MemberSavings: Float? = null,
    val Loan_Amount_Term: Float = 12f,
    val Credit_History: Float = 1.0f,
    val Property_Area: String = "Urban"
)

data class SavingsAssessment(
    val member_savings_kes: Float? = null,
    val max_borrowing_limit_kes: Float? = null,
    val savings_multiplier_compliant: Boolean = true,
    val savings_leverage_ratio: Float? = null,
    val excess_requested_kes: Float? = null
)

data class FinancialCapacityAssessment(
    val total_household_income_kes: Float? = null,
    val estimated_monthly_installment_kes: Float? = null,
    val debt_to_income_percent: Float? = null,
    val cash_flow_viable: Boolean = true
)

data class LoanPredictionResponse(
    val loan_status: String,
    val approval_decision_code: Int,
    val approval_probability: Float,
    val risk_tier: String,
    val risk_score_percent: Float? = null,
    val message: String? = null,
    val savings_assessment: SavingsAssessment? = null,
    val financial_capacity: FinancialCapacityAssessment? = null,
    val committee_action_required: String? = null,
    val evaluated_at: String? = null
)

// 2. Retrofit API Interface
interface ChamaApiService {
    @POST("predict")
    fun predictLoan(@Body request: LoanApplicationRequest): Call<LoanPredictionResponse>
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ====================================================================
        // 1. Role Switcher Binding (Member Portal vs Treasurer Console)
        // ====================================================================
        val btnTabMember = findViewById<Button>(R.id.btnTabMember)
        val btnTabTreasurer = findViewById<Button>(R.id.btnTabTreasurer)
        val layoutMemberPortal = findViewById<LinearLayout>(R.id.layoutMemberPortal)
        val layoutTreasurerConsole = findViewById<LinearLayout>(R.id.layoutTreasurerConsole)

        fun selectTab(isMember: Boolean) {
            if (isMember) {
                layoutMemberPortal.visibility = View.VISIBLE
                layoutTreasurerConsole.visibility = View.GONE
                btnTabMember.setBackgroundResource(R.drawable.bg_tab_selected)
                btnTabMember.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                btnTabTreasurer.setBackgroundResource(R.drawable.bg_tab_unselected)
                btnTabTreasurer.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            } else {
                layoutMemberPortal.visibility = View.GONE
                layoutTreasurerConsole.visibility = View.VISIBLE
                btnTabTreasurer.setBackgroundResource(R.drawable.bg_tab_selected)
                btnTabTreasurer.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                btnTabMember.setBackgroundResource(R.drawable.bg_tab_unselected)
                btnTabMember.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        }

        btnTabMember.setOnClickListener { selectTab(true) }
        btnTabTreasurer.setOnClickListener { selectTab(false) }

        // ====================================================================
        // 2. Financial Snapshot & Form Input Binding
        // ====================================================================
        val tvSnapshotSavings = findViewById<TextView>(R.id.tvSnapshotSavings)
        val tvSnapshotLimit = findViewById<TextView>(R.id.tvSnapshotLimit)

        val etApplicantIncome = findViewById<EditText>(R.id.etApplicantIncome)
        val etMemberSavings = findViewById<EditText>(R.id.etMemberSavings)
        val etCoapplicantIncome = findViewById<EditText>(R.id.etCoapplicantIncome)
        val etLoanAmount = findViewById<EditText>(R.id.etLoanAmount)
        val etLoanTerm = findViewById<EditText>(R.id.etLoanTerm)
        val rgCreditHistory = findViewById<RadioGroup>(R.id.rgCreditHistory)
        val rgPropertyArea = findViewById<RadioGroup>(R.id.rgPropertyArea)
        val btnCheckEligibility = findViewById<Button>(R.id.btnCheckEligibility)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Reactive HCD feedback: update snapshot cards when savings balance changes
        etMemberSavings.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val savings = s?.toString()?.toFloatOrNull() ?: 0f
                tvSnapshotSavings.text = String.format(Locale.US, "KES %,.0f", savings)
                tvSnapshotLimit.text = String.format(Locale.US, "KES %,.0f", savings * 3f)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // ====================================================================
        // 3. Underwriting Assessment & Repayment Preview Binding
        // ====================================================================
        val cardResult = findViewById<LinearLayout>(R.id.cardResult)
        val tvStatusBadge = findViewById<TextView>(R.id.tvStatusBadge)
        val tvRiskTierBadge = findViewById<TextView>(R.id.tvRiskTierBadge)
        val tvScoreNumber = findViewById<TextView>(R.id.tvScoreNumber)
        val tvProbabilityText = findViewById<TextView>(R.id.tvProbabilityText)
        val pbProbability = findViewById<ProgressBar>(R.id.pbProbability)
        val tvEstMonthlyPayment = findViewById<TextView>(R.id.tvEstMonthlyPayment)
        val tvTotalInterest = findViewById<TextView>(R.id.tvTotalInterest)
        val tvSavingsComplianceChip = findViewById<TextView>(R.id.tvSavingsComplianceChip)
        val tvDtiComplianceChip = findViewById<TextView>(R.id.tvDtiComplianceChip)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)

        // ====================================================================
        // 4. Treasurer Console Demonstration Preset Buttons
        // ====================================================================
        val btnPresetPrime = findViewById<Button>(R.id.btnPresetPrime)
        val btnPresetGuarantor = findViewById<Button>(R.id.btnPresetGuarantor)
        val btnPresetCapExceeded = findViewById<Button>(R.id.btnPresetCapExceeded)
        val btnPresetDefault = findViewById<Button>(R.id.btnPresetDefault)

        btnPresetPrime.setOnClickListener {
            selectTab(true)
            etApplicantIncome.setText("50000")
            etMemberSavings.setText("40000")
            etCoapplicantIncome.setText("0")
            etLoanAmount.setText("100000")
            etLoanTerm.setText("12")
            rgCreditHistory.check(R.id.rbCreditClean)
            rgPropertyArea.check(R.id.rbAreaSemiurban)
            Toast.makeText(this, "Loaded: Prime Member Scenario", Toast.LENGTH_SHORT).show()
            btnCheckEligibility.performClick()
        }

        btnPresetGuarantor.setOnClickListener {
            selectTab(true)
            etApplicantIncome.setText("22000")
            etMemberSavings.setText("30000")
            etCoapplicantIncome.setText("35000")
            etLoanAmount.setText("80000")
            etLoanTerm.setText("12")
            rgCreditHistory.check(R.id.rbCreditClean)
            rgPropertyArea.check(R.id.rbAreaSemiurban)
            Toast.makeText(this, "Loaded: Guarantor Rescue Scenario", Toast.LENGTH_SHORT).show()
            btnCheckEligibility.performClick()
        }

        btnPresetCapExceeded.setOnClickListener {
            selectTab(true)
            etApplicantIncome.setText("60000")
            etMemberSavings.setText("25000")
            etCoapplicantIncome.setText("0")
            etLoanAmount.setText("150000")
            etLoanTerm.setText("12")
            rgCreditHistory.check(R.id.rbCreditClean)
            rgPropertyArea.check(R.id.rbAreaUrban)
            Toast.makeText(this, "Loaded: 3x Cap Exceeded Scenario", Toast.LENGTH_SHORT).show()
            btnCheckEligibility.performClick()
        }

        btnPresetDefault.setOnClickListener {
            selectTab(true)
            etApplicantIncome.setText("45000")
            etMemberSavings.setText("30000")
            etCoapplicantIncome.setText("0")
            etLoanAmount.setText("50000")
            etLoanTerm.setText("12")
            rgCreditHistory.check(R.id.rbCreditDefault)
            rgPropertyArea.check(R.id.rbAreaRural)
            Toast.makeText(this, "Loaded: Bad Credit Default Scenario", Toast.LENGTH_SHORT).show()
            btnCheckEligibility.performClick()
        }

        // ====================================================================
        // 5. Retrofit Client Configuration
        // ====================================================================
        // 10.0.2.2 maps directly to the host machine's localhost from Android Emulator
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ChamaApiService::class.java)

        // ====================================================================
        // 6. Underwriting Submission & Dynamic Result Rendering
        // ====================================================================
        btnCheckEligibility.setOnClickListener {
            val incomeStr = etApplicantIncome.text.toString().trim()
            val savingsStr = etMemberSavings.text.toString().trim()
            val coIncomeStr = etCoapplicantIncome.text.toString().trim()
            val loanAmtStr = etLoanAmount.text.toString().trim()
            val termStr = etLoanTerm.text.toString().trim()

            // Validate mandatory fields
            if (incomeStr.isEmpty()) {
                etApplicantIncome.error = "Applicant income is required"
                etApplicantIncome.requestFocus()
                return@setOnClickListener
            }
            if (loanAmtStr.isEmpty()) {
                etLoanAmount.error = "Loan amount is required"
                etLoanAmount.requestFocus()
                return@setOnClickListener
            }

            val applicantIncome = incomeStr.toFloatOrNull() ?: 0f
            val coapplicantIncome = if (coIncomeStr.isNotEmpty()) coIncomeStr.toFloatOrNull() ?: 0f else 0f
            val loanAmount = loanAmtStr.toFloatOrNull() ?: 0f
            val memberSavings = if (savingsStr.isNotEmpty()) savingsStr.toFloatOrNull() else null
            val loanTerm = if (termStr.isNotEmpty()) termStr.toFloatOrNull() ?: 12f else 12f

            // Credit history radio selection
            val creditHistory = if (rgCreditHistory.checkedRadioButtonId == R.id.rbCreditClean) 1.0f else 0.0f

            // Property Area selection
            val propertyArea = when (rgPropertyArea.checkedRadioButtonId) {
                R.id.rbAreaSemiurban -> "Semiurban"
                R.id.rbAreaRural -> "Rural"
                else -> "Urban"
            }

            // Normalization: Model expects LoanAmount & MemberSavings in thousands KES
            val normalizedLoanAmount = if (loanAmount >= 1000f) loanAmount / 1000f else loanAmount
            val normalizedSavings = memberSavings?.let { if (it >= 1000f) it / 1000f else it }

            val request = LoanApplicationRequest(
                ApplicantIncome = applicantIncome,
                CoapplicantIncome = coapplicantIncome,
                LoanAmount = normalizedLoanAmount,
                MemberSavings = normalizedSavings,
                Loan_Amount_Term = loanTerm,
                Credit_History = creditHistory,
                Property_Area = propertyArea
            )

            // UI state: loading
            progressBar.visibility = View.VISIBLE
            btnCheckEligibility.isEnabled = false
            cardResult.visibility = View.GONE

            apiService.predictLoan(request).enqueue(object : Callback<LoanPredictionResponse> {
                override fun onResponse(call: Call<LoanPredictionResponse>, response: Response<LoanPredictionResponse>) {
                    progressBar.visibility = View.GONE
                    btnCheckEligibility.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        cardResult.visibility = View.VISIBLE

                        // 1. Status Badge
                        val isApproved = res.loan_status.equals("Approved", ignoreCase = true)
                        if (isApproved) {
                            tvStatusBadge.text = "APPROVED"
                            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_approved)
                            tvStatusBadge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_approved))
                        } else {
                            tvStatusBadge.text = "REJECTED"
                            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rejected)
                            tvStatusBadge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_rejected))
                        }

                        // 2. Risk Tier Badge
                        tvRiskTierBadge.text = "${res.risk_tier} Profile"
                        val tierColor = when (res.risk_tier.lowercase()) {
                            "low risk" -> R.color.status_approved
                            "medium risk" -> R.color.status_warning
                            else -> R.color.status_rejected
                        }
                        tvRiskTierBadge.setTextColor(ContextCompat.getColor(this@MainActivity, tierColor))

                        // 3. Score Gauge (Out of 10)
                        val scoreOutOf10 = res.approval_probability * 10f
                        tvScoreNumber.text = String.format(Locale.US, "%.1f", scoreOutOf10)
                        val scoreColor = when {
                            scoreOutOf10 >= 7.0f -> ContextCompat.getColor(this@MainActivity, R.color.status_approved)
                            scoreOutOf10 >= 5.0f -> ContextCompat.getColor(this@MainActivity, R.color.status_warning)
                            else -> ContextCompat.getColor(this@MainActivity, R.color.status_rejected)
                        }
                        tvScoreNumber.setTextColor(scoreColor)

                        // 4. Probability Progress Bar
                        val probPercent = (res.approval_probability * 100).toInt()
                        pbProbability.progress = probPercent
                        val progressTint = if (isApproved) R.color.status_approved else R.color.status_rejected
                        pbProbability.progressTintList = ContextCompat.getColorStateList(this@MainActivity, progressTint)
                        tvProbabilityText.text = "Model Approval Confidence: ${String.format(Locale.US, "%.1f", res.approval_probability * 100)}%"

                        // 5. Repayment Preview (Amortized at 10% annual reducing balance)
                        val actualPrincipalKes = if (loanAmount < 1000f) loanAmount * 1000f else loanAmount
                        val monthlyInstallment = res.financial_capacity?.estimated_monthly_installment_kes
                            ?: run {
                                val monthlyRate = 0.10 / 12.0
                                val n = loanTerm.toDouble()
                                val p = actualPrincipalKes.toDouble()
                                if (n > 0) {
                                    val emi = p * (monthlyRate * Math.pow(1.0 + monthlyRate, n)) / (Math.pow(1.0 + monthlyRate, n) - 1.0)
                                    emi.toFloat()
                                } else {
                                    (p / 12).toFloat()
                                }
                            }
                        val totalRepayment = monthlyInstallment * loanTerm
                        val totalInterest = maxOf(0f, totalRepayment - actualPrincipalKes)

                        tvEstMonthlyPayment.text = String.format(Locale.US, "KES %,.2f", monthlyInstallment)
                        tvTotalInterest.text = String.format(Locale.US, "KES %,.2f", totalInterest)

                        // 6. Savings Cap Bylaw Compliance Check
                        val memberSavingsKes = memberSavings?.let { if (it < 1000f) it * 1000f else it } ?: 40000f
                        val maxEligible = res.savings_assessment?.max_borrowing_limit_kes ?: (memberSavingsKes * 3f)
                        val savingsCompliant = res.savings_assessment?.savings_multiplier_compliant ?: (actualPrincipalKes <= maxEligible)
                        val leverage = if (memberSavingsKes > 0) actualPrincipalKes / memberSavingsKes else 0f

                        if (savingsCompliant) {
                            tvSavingsComplianceChip.text = String.format(
                                Locale.US,
                                "• Savings Cap: Requested KES %,.0f vs Max Eligible KES %,.0f (%.1fx Leverage - Compliant)",
                                actualPrincipalKes, maxEligible, leverage
                            )
                            tvSavingsComplianceChip.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_approved))
                        } else {
                            val excess = actualPrincipalKes - maxEligible
                            tvSavingsComplianceChip.text = String.format(
                                Locale.US,
                                "• Savings Cap Exceeded: Requested KES %,.0f exceeds 3x limit by KES %,.0f (Max Eligible KES %,.0f)",
                                actualPrincipalKes, excess, maxEligible
                            )
                            tvSavingsComplianceChip.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_rejected))
                        }

                        // 7. DTI Compliance Check
                        val householdIncome = applicantIncome + coapplicantIncome
                        val dti = res.financial_capacity?.debt_to_income_percent ?: if (householdIncome > 0) (monthlyInstallment / householdIncome) * 100f else 100f
                        val dtiViable = res.financial_capacity?.cash_flow_viable ?: (dti <= 45f)

                        if (dtiViable) {
                            tvDtiComplianceChip.text = String.format(
                                Locale.US,
                                "• Monthly DTI: %.1f%% of household income (Healthy < 45%% threshold)",
                                dti
                            )
                            tvDtiComplianceChip.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_approved))
                        } else {
                            tvDtiComplianceChip.text = String.format(
                                Locale.US,
                                "• Monthly DTI Warning: %.1f%% of household income (Exceeds 45%% threshold)",
                                dti
                            )
                            tvDtiComplianceChip.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_warning))
                        }

                        // 8. Committee Directive
                        val directive = res.committee_action_required ?: res.message ?: "Underwriting decision calculated based on Chama bylaws."
                        tvMessage.text = directive

                    } else {
                        Toast.makeText(this@MainActivity, "Server Error: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoanPredictionResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnCheckEligibility.isEnabled = true
                    Toast.makeText(
                        this@MainActivity,
                        "Connection Failed: Ensure Python FastAPI is running on port 8000.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }
}
