package com.example.chamaloanapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    val CoapplicantIncome: Float,
    val LoanAmount: Float,
    val Loan_Amount_Term: Float,
    val Credit_History: Float = 1.0f,
    val Property_Area: String = "Urban"
)

data class LoanPredictionResponse(
    val loan_status: String,
    val approval_decision_code: Int,
    val approval_probability: Float,
    val risk_tier: String
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

        val etApplicantIncome = findViewById<EditText>(R.id.etApplicantIncome)
        val etCoapplicantIncome = findViewById<EditText>(R.id.etCoapplicantIncome)
        val etLoanAmount = findViewById<EditText>(R.id.etLoanAmount)
        val etLoanTerm = findViewById<EditText>(R.id.etLoanTerm)
        val btnCheckEligibility = findViewById<Button>(R.id.btnCheckEligibility)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        // Setup Retrofit client
        // Use "http://10.0.2.2:8000/" for Android Emulator pointing to local FastAPI
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ChamaApiService::class.java)

        btnCheckEligibility.setOnClickListener {
            val incomeStr = etApplicantIncome.text.toString()
            val coIncomeStr = etCoapplicantIncome.text.toString()
            val loanAmtStr = etLoanAmount.text.toString()
            val termStr = etLoanTerm.text.toString()

            if (incomeStr.isEmpty() || loanAmtStr.isEmpty() || termStr.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoanApplicationRequest(
                ApplicantIncome = incomeStr.toFloat(),
                CoapplicantIncome = if (coIncomeStr.isNotEmpty()) coIncomeStr.toFloat() else 0f,
                LoanAmount = loanAmtStr.toFloat(),
                Loan_Amount_Term = termStr.toFloat()
            )

            tvResult.text = "Processing prediction..."

            // Make asynchronous network call
            apiService.predictLoan(request).enqueue(object : Callback<LoanPredictionResponse> {
                override fun onResponse(call: Call<LoanPredictionResponse>, response: Response<LoanPredictionResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        tvResult.text = """
                            Status: ${res.loan_status}
                            Approval Probability: ${String.format("%.2f", res.approval_probability * 100)}%
                            Risk Tier: ${res.risk_tier}
                        """.trimIndent()
                    } else {
                        tvResult.text = "Error: Server returned code ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<LoanPredictionResponse>, t: Throwable) {
                    tvResult.text = "Connection Failed: ${t.localizedMessage}"
                }
            })
        }
    }
}