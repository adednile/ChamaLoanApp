package com.example.chamaloanapp

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// ─────────────────────────────────────────────
// Request / Response Data Classes (FastAPI schema)
// ─────────────────────────────────────────────

data class LoanApplicationRequest(
    @SerializedName("Gender") val Gender: String = "Male",
    @SerializedName("Married") val Married: String = "Yes",
    @SerializedName("Dependents") val Dependents: Int = 0,
    @SerializedName("Education") val Education: String = "Graduate",
    @SerializedName("Self_Employed") val Self_Employed: String = "No",
    @SerializedName("ApplicantIncome") val ApplicantIncome: Float,
    @SerializedName("CoapplicantIncome") val CoapplicantIncome: Float = 0f,
    @SerializedName("LoanAmount") val LoanAmount: Float,
    @SerializedName("MemberSavings") val MemberSavings: Float? = null,
    @SerializedName("Loan_Amount_Term") val Loan_Amount_Term: Float = 12f,
    @SerializedName("Credit_History") val Credit_History: Float = 1.0f,
    @SerializedName("Property_Area") val Property_Area: String = "Urban"
)

data class SavingsAssessment(
    @SerializedName("member_savings_kes") val member_savings_kes: Float? = null,
    @SerializedName("max_borrowing_limit_kes") val max_borrowing_limit_kes: Float? = null,
    @SerializedName("savings_multiplier_compliant") val savings_multiplier_compliant: Boolean = true,
    @SerializedName("savings_leverage_ratio") val savings_leverage_ratio: Float? = null,
    @SerializedName("excess_requested_kes") val excess_requested_kes: Float? = null
)

data class FinancialCapacityAssessment(
    @SerializedName("total_household_income_kes") val total_household_income_kes: Float? = null,
    @SerializedName("estimated_monthly_installment_kes") val estimated_monthly_installment_kes: Float? = null,
    @SerializedName("debt_to_income_percent") val debt_to_income_percent: Float? = null,
    @SerializedName("cash_flow_viable") val cash_flow_viable: Boolean = true
)

data class LoanPredictionResponse(
    @SerializedName("loan_status") val loan_status: String,
    @SerializedName("approval_decision_code") val approval_decision_code: Int,
    @SerializedName("approval_probability") val approval_probability: Float,
    @SerializedName("risk_tier") val risk_tier: String,
    @SerializedName("risk_score_percent") val risk_score_percent: Float? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("savings_assessment") val savings_assessment: SavingsAssessment? = null,
    @SerializedName("financial_capacity") val financial_capacity: FinancialCapacityAssessment? = null,
    @SerializedName("committee_action_required") val committee_action_required: String? = null,
    @SerializedName("evaluated_at") val evaluated_at: String? = null
)

// ─────────────────────────────────────────────
// Error Response Data Classes (FastAPI 422 / 500)
// ─────────────────────────────────────────────

data class FastApiValidationError(
    @SerializedName("loc") val loc: List<String>? = null,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("type") val type: String? = null
)

data class FastApiErrorResponse(
    @SerializedName("detail") val detail: Any? = null
)

// ─────────────────────────────────────────────
// Retrofit API Interface
// ─────────────────────────────────────────────

interface ChamaApiService {
    @POST("predict")
    fun predictLoan(@Body request: LoanApplicationRequest): Call<LoanPredictionResponse>

    @GET("/")
    fun getHealthCheck(): Call<Map<String, Any>>
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
