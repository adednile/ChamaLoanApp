package com.example.chamaloanapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// ─────────────────────────────────────────────
// Request / Response Data Classes (FastAPI schema)
// ─────────────────────────────────────────────

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

// ─────────────────────────────────────────────
// Retrofit API Interface
// ─────────────────────────────────────────────

interface ChamaApiService {
    @POST("predict")
    fun predictLoan(@Body request: LoanApplicationRequest): Call<LoanPredictionResponse>
}

// ─────────────────────────────────────────────
// Preset data passed from Treasurer → Loan screen
// ─────────────────────────────────────────────

data class LoanPreset(
    val label: String,
    val applicantIncome: String,
    val memberSavings: String,
    val coApplicantIncome: String,
    val loanAmount: String,
    val loanTerm: String,
    val creditHistory: Int,   // 0 = clean (rbCreditClean), 1 = default (rbCreditDefault)
    val propertyArea: Int     // 0 = semiurban, 1 = urban, 2 = rural
)

object LoanPresets {
    val PRIME = LoanPreset("Prime Member", "50000", "40000", "0", "100000", "12", 0, 0)
    val GUARANTOR = LoanPreset("Guarantor Rescue", "22000", "30000", "35000", "80000", "12", 0, 0)
    val CAP_EXCEEDED = LoanPreset("3x Cap Exceeded", "60000", "25000", "0", "150000", "12", 0, 1)
    val BAD_CREDIT = LoanPreset("Bad Credit Default", "45000", "30000", "0", "50000", "12", 1, 2)
}
