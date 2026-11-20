import os
import joblib
import numpy as np
import pandas as pd
from datetime import datetime

# Load the trained Random Forest model
MODEL_PATH = os.path.join(os.path.dirname(__file__), "chama_loan_rf_model.pkl")
rf_model = None

def get_model():
    global rf_model
    if rf_model is None:
        if os.path.exists(MODEL_PATH):
            rf_model = joblib.load(MODEL_PATH)
        else:
            raise FileNotFoundError(f"Model file not found at {MODEL_PATH}")
    return rf_model

def evaluate_chama_loan(data: dict) -> dict:
    """
    Evaluates loan eligibility combining:
    1. Kenyan Chama Bylaws Engine (3x Savings Cap, DTI <= 45%, Credit Default Knockout)
    2. Random Forest ML Risk Model (Trained Scikit-Learn Classifier)
    """
    applicant_income = float(data.get("ApplicantIncome", 0.0))
    coapplicant_income = float(data.get("CoapplicantIncome", 0.0))
    loan_amount_k = float(data.get("LoanAmount", 0.0)) # In thousands KES (e.g. 100.0 = 100,000 KES)
    member_savings_input = data.get("MemberSavings")
    term_months = float(data.get("Loan_Amount_Term", 12.0))
    credit_history = float(data.get("Credit_History", 1.0))
    gender = str(data.get("Gender", "Male"))
    married = str(data.get("Married", "Yes"))
    dependents = str(data.get("Dependents", "0"))
    education = str(data.get("Education", "Graduate"))
    self_employed = str(data.get("Self_Employed", "No"))
    property_area = str(data.get("Property_Area", "Urban"))

    # Convert loan amount to raw KES
    loan_kes = loan_amount_k * 1000.0

    # Savings in raw KES
    if member_savings_input is not None:
        raw_savings = float(member_savings_input)
        member_savings_kes = raw_savings * 1000.0 if raw_savings < 1000.0 else raw_savings
    else:
        member_savings_kes = 40000.0

    # 1. Chama Bylaws: 3x Savings Borrowing Cap
    max_borrowing_limit_kes = member_savings_kes * 3.0
    savings_compliant = loan_kes <= max_borrowing_limit_kes
    savings_leverage_ratio = round(loan_kes / member_savings_kes, 2) if member_savings_kes > 0 else 999.0
    excess_requested_kes = max(0.0, loan_kes - max_borrowing_limit_kes)

    # 2. Financial Capacity & Debt-to-Income (DTI)
    total_household_income_kes = applicant_income + coapplicant_income
    
    # 10% annual reducing balance EMI calculation
    r = 0.10 / 12.0
    n = max(1.0, term_months)
    p = loan_kes
    if n > 0 and r > 0:
        emi_kes = (p * r * ((1.0 + r) ** n)) / (((1.0 + r) ** n) - 1.0)
    else:
        emi_kes = p / n

    dti_percent = round((emi_kes / total_household_income_kes) * 100.0, 1) if total_household_income_kes > 0 else 100.0
    cash_flow_viable = dti_percent <= 45.0

    # 3. ML Model Feature Vector Construction
    # Feature engineering matching training pipeline
    total_income_model = total_household_income_kes
    loan_to_income = (loan_amount_k * 1000.0) / (total_income_model + 1.0)
    emi_model = (loan_amount_k * 1000.0) / max(1.0, term_months)
    emi_to_income = emi_model / (total_income_model + 1.0)
    balance_income = total_income_model - emi_model
    total_income_log = np.log1p(total_income_model)
    loan_amount_log = np.log1p(loan_amount_k)

    features = {
        'LoanAmount': loan_amount_k,
        'Loan_Amount_Term': term_months,
        'Credit_History': credit_history,
        'TotalIncome': total_income_model,
        'Loan_to_Income': loan_to_income,
        'EMI': emi_model,
        'EMI_to_Income': emi_to_income,
        'Balance_Income': balance_income,
        'TotalIncome_Log': total_income_log,
        'LoanAmount_Log': loan_amount_log,
        'Gender_Male': 1 if gender.lower() == 'male' else 0,
        'Married_Yes': 1 if married.lower() == 'yes' else 0,
        'Dependents_1': 1 if str(dependents) == '1' else 0,
        'Dependents_2': 1 if str(dependents) == '2' else 0,
        'Dependents_3+': 1 if str(dependents) in ['3', '3+'] else 0,
        'Education_Not Graduate': 1 if education.lower() == 'not graduate' else 0,
        'Self_Employed_Yes': 1 if self_employed.lower() == 'yes' else 0,
        'Property_Area_Semiurban': 1 if property_area.lower() == 'semiurban' else 0,
        'Property_Area_Urban': 1 if property_area.lower() == 'urban' else 0
    }

    feature_cols = [
        'LoanAmount', 'Loan_Amount_Term', 'Credit_History', 'TotalIncome',
        'Loan_to_Income', 'EMI', 'EMI_to_Income', 'Balance_Income', 'TotalIncome_Log',
        'LoanAmount_Log', 'Gender_Male', 'Married_Yes', 'Dependents_1',
        'Dependents_2', 'Dependents_3+', 'Education_Not Graduate',
        'Self_Employed_Yes', 'Property_Area_Semiurban', 'Property_Area_Urban'
    ]

    model = get_model()
    X = pd.DataFrame([[features[c] for c in feature_cols]], columns=feature_cols)

    # ML Inference
    ml_prob = float(model.predict_proba(X)[0, 1])

    # 4. Underwriting Rules Synthesis
    # Rule A: Delinquent credit default (Credit_History == 0) -> Immediate knockout
    # Rule B: 3x Savings cap exceeded -> Immediate knockout (Bylaw violation)
    # Rule C: DTI > 45% -> Risk flag / overleveraged
    # Rule D: Strong Guarantor -> Rescues lower individual income if savings cap is met

    if credit_history == 0.0:
        loan_status = "Rejected"
        decision_code = 0
        approval_prob = min(ml_prob, 0.15)
        risk_tier = "High Risk"
        message = "Delinquent credit default history on file. Knockout policy applies."
        committee_action = "Rejected by Credit Policy: Outstanding default must be cleared before applying."
    elif not savings_compliant:
        loan_status = "Rejected"
        decision_code = 0
        approval_prob = min(ml_prob, 0.25)
        risk_tier = "High Risk"
        message = f"Exceeds Chama 3x Savings Cap (Requested KES {loan_kes:,.0f} vs Max KES {max_borrowing_limit_kes:,.0f})."
        committee_action = f"Bylaw Violation: Member must deposit additional KES {excess_requested_kes / 3.0:,.0f} in savings to qualify."
    elif not cash_flow_viable:
        loan_status = "Rejected"
        decision_code = 0
        approval_prob = min(ml_prob, 0.40)
        risk_tier = "Medium Risk"
        message = f"Debt-to-Income ratio ({dti_percent}%) exceeds maximum 45% threshold."
        committee_action = "Credit Committee review required for excessive debt-to-income ratio."
    else:
        # All bylaws passed, evaluate ML probability
        if ml_prob >= 0.70:
            loan_status = "Approved"
            decision_code = 1
            approval_prob = ml_prob
            risk_tier = "Low Risk"
            message = "Prime borrower profile. Meets all Chama bylaws and credit criteria."
            committee_action = "Auto-approved for immediate loan disbursement via M-Pesa."
        elif ml_prob >= 0.45:
            loan_status = "Approved"
            decision_code = 1
            approval_prob = ml_prob
            risk_tier = "Medium Risk"
            if coapplicant_income > 0:
                message = "Approved with Social Guarantor rescue."
                committee_action = "Guarantor deed verified. Approved for disbursement."
            else:
                message = "Approved with standard terms."
                committee_action = "Approved subject to Chama treasurer sign-off."
        else:
            loan_status = "Rejected"
            decision_code = 0
            approval_prob = ml_prob
            risk_tier = "High Risk"
            message = "ML model risk score indicates high default probability."
            committee_action = "Rejected based on credit scoring assessment."

    return {
        "loan_status": loan_status,
        "approval_decision_code": decision_code,
        "approval_probability": round(approval_prob, 4),
        "risk_tier": risk_tier,
        "risk_score_percent": round(approval_prob * 100.0, 1),
        "message": message,
        "savings_assessment": {
            "member_savings_kes": member_savings_kes,
            "max_borrowing_limit_kes": max_borrowing_limit_kes,
            "savings_multiplier_compliant": savings_compliant,
            "savings_leverage_ratio": savings_leverage_ratio,
            "excess_requested_kes": excess_requested_kes
        },
        "financial_capacity": {
            "total_household_income_kes": total_household_income_kes,
            "estimated_monthly_installment_kes": round(emi_kes, 2),
            "debt_to_income_percent": dti_percent,
            "cash_flow_viable": cash_flow_viable
        },
        "committee_action_required": committee_action,
        "evaluated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    }
