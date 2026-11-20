from engine import evaluate_chama_loan

scenarios = [
    {
        "name": "1. Prime Member",
        "data": {
            "ApplicantIncome": 50000.0,
            "MemberSavings": 40000.0,
            "CoapplicantIncome": 0.0,
            "LoanAmount": 100.0,
            "Loan_Amount_Term": 12.0,
            "Credit_History": 1.0,
            "Property_Area": "Urban"
        },
        "expected_status": "Approved",
        "expected_risk": "Low Risk"
    },
    {
        "name": "2. Guarantor Rescue",
        "data": {
            "ApplicantIncome": 22000.0,
            "MemberSavings": 30000.0,
            "CoapplicantIncome": 35000.0,
            "LoanAmount": 80.0,
            "Loan_Amount_Term": 12.0,
            "Credit_History": 1.0,
            "Property_Area": "Semiurban"
        },
        "expected_status": "Approved",
        "expected_risk": "Low Risk"
    },
    {
        "name": "3. 3x Cap Exceeded",
        "data": {
            "ApplicantIncome": 60000.0,
            "MemberSavings": 25000.0,
            "CoapplicantIncome": 0.0,
            "LoanAmount": 150.0,
            "Loan_Amount_Term": 12.0,
            "Credit_History": 1.0,
            "Property_Area": "Urban"
        },
        "expected_status": "Rejected",
        "expected_risk": "High Risk"
    },
    {
        "name": "4. Bad Credit Default",
        "data": {
            "ApplicantIncome": 45000.0,
            "MemberSavings": 30000.0,
            "CoapplicantIncome": 0.0,
            "LoanAmount": 50.0,
            "Loan_Amount_Term": 12.0,
            "Credit_History": 0.0,
            "Property_Area": "Rural"
        },
        "expected_status": "Rejected",
        "expected_risk": "High Risk"
    }
]

print("=== Running Chama Engine Automated Scenario Validations ===")
passed = 0
for sc in scenarios:
    res = evaluate_chama_loan(sc["data"])
    status_ok = res["loan_status"] == sc["expected_status"]
    if status_ok:
        passed += 1
        print(f" PASS: {sc['name']} -> {res['loan_status']} ({res['risk_tier']}, prob={res['approval_probability']:.1%})")
        print(f"       Message: {res['message']}")
    else:
        print(f" FAIL: {sc['name']} -> Got {res['loan_status']}, Expected {sc['expected_status']}")

print(f"\nResult: {passed}/{len(scenarios)} scenarios passed!")
