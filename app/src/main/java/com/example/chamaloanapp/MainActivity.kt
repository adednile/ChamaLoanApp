package com.example.chamaloanapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
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
    val Loan_Amount_Term: Float = 360f,
    val Credit_History: Float = 1.0f,
    val Property_Area: String = "Urban"
)

data class LoanPredictionResponse(
    val loan_status: String,
    val approval_decision_code: Int,
    val approval_probability: Float,
    val risk_tier: String,
    val risk_score_percent: Float? = null,
    val message: String? = null
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

        // Bind form inputs
        val etApplicantIncome = findViewById<EditText>(R.id.etApplicantIncome)
        val etMemberSavings = findViewById<EditText>(R.id.etMemberSavings)
        val etCoapplicantIncome = findViewById<EditText>(R.id.etCoapplicantIncome)
        val etLoanAmount = findViewById<EditText>(R.id.etLoanAmount)
        val etLoanTerm = findViewById<EditText>(R.id.etLoanTerm)
        val rgCreditHistory = findViewById<RadioGroup>(R.id.rgCreditHistory)
        val btnCheckEligibility = findViewById<Button>(R.id.btnCheckEligibility)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        // Bind dynamic assessment card components
        val cardResult = findViewById<LinearLayout>(R.id.cardResult)
        val tvStatusBadge = findViewById<TextView>(R.id.tvStatusBadge)
        val tvRiskTierBadge = findViewById<TextView>(R.id.tvRiskTierBadge)
        val tvProbabilityText = findViewById<TextView>(R.id.tvProbabilityText)
        val pbProbability = findViewById<ProgressBar>(R.id.pbProbability)
        val tvMessage = findViewById<TextView>(R.id.tvMessage)

        // Setup Retrofit client (10.0.2.2 maps to host machine localhost from Android Emulator)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ChamaApiService::class.java)

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
            val loanTerm = if (termStr.isNotEmpty()) termStr.toFloatOrNull() ?: 360f else 360f

            // Credit history radio selection
            val creditHistory = if (rgCreditHistory.checkedRadioButtonId == R.id.rbCreditClean) 1.0f else 0.0f

            // Model expects LoanAmount in thousands (if entered in thousands vs absolute KES)
            // If borrower enters e.g. 50,000 KES, convert to 50.0k for model feature space
            val normalizedLoanAmount = if (loanAmount >= 1000f) loanAmount / 1000f else loanAmount
            val normalizedSavings = memberSavings?.let { if (it >= 1000f) it / 1000f else it }

            val request = LoanApplicationRequest(
                ApplicantIncome = applicantIncome,
                CoapplicantIncome = coapplicantIncome,
                LoanAmount = normalizedLoanAmount,
                MemberSavings = normalizedSavings,
                Loan_Amount_Term = loanTerm,
                Credit_History = creditHistory
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
                        if (res.loan_status.equals("Approved", ignoreCase = true)) {
                            tvStatusBadge.text = "APPROVED"
                            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_approved)
                            tvStatusBadge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_approved))
                        } else {
                            tvStatusBadge.text = "REJECTED"
                            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_rejected)
                            tvStatusBadge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.status_rejected))
                        }

                        // 2. Risk Tier Badge
                        tvRiskTierBadge.text = res.risk_tier
                        val tierColor = when (res.risk_tier.lowercase()) {
                            "low risk" -> R.color.status_approved
                            "medium risk" -> R.color.status_warning
                            else -> R.color.status_rejected
                        }
                        tvRiskTierBadge.setTextColor(ContextCompat.getColor(this@MainActivity, tierColor))

                        // 3. Probability Progress Bar
                        val probPercent = (res.approval_probability * 100).toInt()
                        pbProbability.progress = probPercent
                        tvProbabilityText.text = "Model Approval Confidence: ${String.format("%.1f", res.approval_probability * 100)}%"

                        // 4. Committee Directive Message
                        tvMessage.text = res.message ?: "Underwriting decision calculated based on Chama bylaws."

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
