package com.example.chamaloanapp

import java.util.Locale
import java.util.regex.Pattern

// ─────────────────────────────────────────────────────────────────────────────
// Data Models for M-Pesa Ledger Ingestion & Chama Management
// ─────────────────────────────────────────────────────────────────────────────

data class ChamaMember(
    val memberId: String,
    val fullName: String,
    val phoneNormalized: String, // e.g. "254712345678"
    val email: String = "",
    val groupCode: String = "CH-4482",
    val role: String = "MEMBER", // "MEMBER" or "TREASURER"
    var totalSavingsKes: Float = 0f,
    var activeLoanBalanceKes: Float = 0f
)

enum class TransactionType(val label: String) {
    SHARE_CONTRIBUTION("Monthly Shares"),
    LOAN_REPAYMENT("Loan Repayment"),
    WELFARE_FUND("Welfare Reserve"),
    PENALTY_FINE("Fine / Penalty")
}

data class ParsedMpesaTransaction(
    val transactionCode: String,
    val amountKes: Float,
    val senderName: String,
    val senderPhone: String,
    val timestampStr: String,
    val rawText: String,
    var matchedMember: ChamaMember? = null,
    var transactionType: TransactionType = TransactionType.SHARE_CONTRIBUTION,
    var isReconciled: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Regex Parser Engine for Kenyan M-Pesa SMS
// ─────────────────────────────────────────────────────────────────────────────

object MpesaParserEngine {

    private val CODE_PATTERN = Pattern.compile("^([A-Z0-9]{8,12})\\s+Confirmed", Pattern.CASE_INSENSITIVE)
    private val AMOUNT_PATTERN = Pattern.compile("Ksh\\.?\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE)
    private val PHONE_PATTERN = Pattern.compile("(?:254|0)(7\\d{8}|1\\d{8})")
    private val SENDER_FROM_PATTERN = Pattern.compile("(?:from|received from)\\s+([A-Z\\s]+?)(?:\\s+(?:07|254|01|on))", Pattern.CASE_INSENSITIVE)
    private val DATE_TIME_PATTERN = Pattern.compile("on\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*(?:AM|PM)?)", Pattern.CASE_INSENSITIVE)

    fun parseSms(rawSms: String): ParsedMpesaTransaction? {
        val cleanText = rawSms.trim().replace("\n", " ")

        // 1. Extract Code (e.g. QK99248M)
        var code = "MP" + (10000000..99999999).random()
        val codeMatcher = CODE_PATTERN.matcher(cleanText)
        if (codeMatcher.find()) {
            code = codeMatcher.group(1)?.uppercase(Locale.US) ?: code
        } else {
            val bareMatcher = Pattern.compile("^([A-Z0-9]{8,12})\\b").matcher(cleanText)
            if (bareMatcher.find()) {
                code = bareMatcher.group(1)?.uppercase(Locale.US) ?: code
            }
        }

        // 2. Extract Amount
        var amount = 0f
        val amountMatcher = AMOUNT_PATTERN.matcher(cleanText)
        if (amountMatcher.find()) {
            val amtStr = amountMatcher.group(1)?.replace(",", "") ?: ""
            amount = amtStr.toFloatOrNull() ?: 0f
        }
        if (amount <= 0f) return null

        // 3. Extract Phone Number
        var phone = ""
        val phoneMatcher = PHONE_PATTERN.matcher(cleanText)
        if (phoneMatcher.find()) {
            val matchedDigits = phoneMatcher.group(0) ?: ""
            phone = normalizePhoneNumber(matchedDigits)
        }

        // 4. Extract Sender Name
        var senderName = "Unknown Sender"
        val senderMatcher = SENDER_FROM_PATTERN.matcher(cleanText)
        if (senderMatcher.find()) {
            val candidate = senderMatcher.group(1)?.trim() ?: ""
            if (candidate.isNotEmpty() && !candidate.equals("Ksh", ignoreCase = true)) {
                senderName = candidate
            }
        }

        // 5. Extract Date & Time
        var timestamp = "Today"
        val dateMatcher = DATE_TIME_PATTERN.matcher(cleanText)
        if (dateMatcher.find()) {
            val d = dateMatcher.group(1) ?: ""
            val t = dateMatcher.group(2) ?: ""
            timestamp = "$d at $t"
        }

        return ParsedMpesaTransaction(
            transactionCode = code,
            amountKes = amount,
            senderName = senderName,
            senderPhone = phone,
            timestampStr = timestamp,
            rawText = rawSms
        )
    }

    fun normalizePhoneNumber(phone: String): String {
        val digits = phone.replace("[^0-9]".toRegex(), "")
        return when {
            digits.startsWith("0") && digits.length == 10 -> "254" + digits.substring(1)
            digits.startsWith("7") && digits.length == 9 -> "254$digits"
            digits.startsWith("1") && digits.length == 9 -> "254$digits"
            digits.startsWith("254") && digits.length == 12 -> digits
            else -> digits
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared In-Memory Chama Ledger & Member Registry
// ─────────────────────────────────────────────────────────────────────────────

object ChamaLedgerRepository {

    // Registered Chama groups: Code -> Name
    val chamaGroups = mutableMapOf(
        "CH-4482" to "Mwangaza Chama",
        "CH-1001" to "Ushirika Chama"
    )

    // Members list
    val members = mutableListOf(
        ChamaMember("MBR-0191959", "Eli Ndeda", "254712345678", email = "elindeda1@gmail.com", groupCode = "CH-4482", role = "MEMBER", totalSavingsKes = 40000f, activeLoanBalanceKes = 0f),
        ChamaMember("MBR-0191917", "Alex Mutitu", "254722123456", email = "alexmutitu@gmail.com", groupCode = "CH-4482", role = "MEMBER", totalSavingsKes = 35000f, activeLoanBalanceKes = 15000f),
        ChamaMember("MBR-0191834", "Faith Wanjiku", "254733987654", email = "faithwanjiku@gmail.com", groupCode = "CH-4482", role = "MEMBER", totalSavingsKes = 50000f, activeLoanBalanceKes = 0f),
        ChamaMember("MBR-0191755", "Kevin Otieno", "254710554433", email = "kevinotieno@gmail.com", groupCode = "CH-4482", role = "MEMBER", totalSavingsKes = 28000f, activeLoanBalanceKes = 8000f),
        ChamaMember("MBR-0191620", "Grace Chebet", "254720112233", email = "gracechebet@gmail.com", groupCode = "CH-4482", role = "MEMBER", totalSavingsKes = 62000f, activeLoanBalanceKes = 0f)
    )

    val transactionHistory = mutableListOf<ParsedMpesaTransaction>()

    var groupTotalSavingsPoolKes: Float = 285000f
    var groupActiveDisbursedKes: Float = 150000f

    fun registerChama(code: String, name: String) {
        chamaGroups[code.uppercase(Locale.US).trim()] = name.trim()
    }

    fun isValidGroupCode(code: String): Boolean {
        return chamaGroups.containsKey(code.uppercase(Locale.US).trim())
    }

    fun getChamaName(code: String): String {
        return chamaGroups[code.uppercase(Locale.US).trim()] ?: "Chama Trust ($code)"
    }

    fun addMember(member: ChamaMember) {
        // Replace or add
        members.removeAll { it.memberId == member.memberId || it.phoneNormalized == member.phoneNormalized }
        members.add(member)
    }

    fun removeMember(memberId: String): Boolean {
        return members.removeAll { it.memberId == memberId }
    }

    fun getMembersForGroup(groupCode: String): List<ChamaMember> {
        val clean = groupCode.uppercase(Locale.US).trim()
        return members.filter { it.groupCode.uppercase(Locale.US).trim() == clean }
    }

    fun getMember(memberId: String): ChamaMember? {
        return members.find { it.memberId == memberId }
    }

    fun getGroupSavingsPool(groupCode: String): Float {
        val groupMembers = getMembersForGroup(groupCode)
        val sum = groupMembers.sumOf { it.totalSavingsKes.toDouble() }.toFloat()
        return if (groupMembers.isNotEmpty()) sum else 0f
    }

    fun getGroupActiveDisbursed(groupCode: String): Float {
        val groupMembers = getMembersForGroup(groupCode)
        val sum = groupMembers.sumOf { it.activeLoanBalanceKes.toDouble() }.toFloat()
        return if (groupMembers.isNotEmpty()) sum else 0f
    }

    fun matchMember(phone: String, senderName: String): ChamaMember? {
        val normPhone = MpesaParserEngine.normalizePhoneNumber(phone)
        val phoneMatch = members.find { it.phoneNormalized == normPhone }
        if (phoneMatch != null) return phoneMatch

        val cleanSender = senderName.lowercase(Locale.US).trim()
        if (cleanSender.length >= 3 && cleanSender != "unknown sender") {
            return members.find { member ->
                val memName = member.fullName.lowercase(Locale.US)
                val parts = memName.split(" ")
                parts.any { part -> part.length >= 3 && cleanSender.contains(part) }
            }
        }
        return null
    }

    fun applyTransaction(tx: ParsedMpesaTransaction, member: ChamaMember, type: TransactionType) {
        tx.matchedMember = member
        tx.transactionType = type
        tx.isReconciled = true

        when (type) {
            TransactionType.SHARE_CONTRIBUTION -> {
                member.totalSavingsKes += tx.amountKes
                groupTotalSavingsPoolKes += tx.amountKes
            }
            TransactionType.LOAN_REPAYMENT -> {
                val reduction = minOf(member.activeLoanBalanceKes, tx.amountKes)
                member.activeLoanBalanceKes -= reduction
                groupActiveDisbursedKes -= reduction
            }
            TransactionType.WELFARE_FUND, TransactionType.PENALTY_FINE -> {
                groupTotalSavingsPoolKes += tx.amountKes
            }
        }

        transactionHistory.add(0, tx)
    }
}
