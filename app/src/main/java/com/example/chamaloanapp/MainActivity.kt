package com.example.chamaloanapp

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAB_HOME = -1
        const val TAB_LOAN = 0
        const val TAB_LEDGER = 1
        const val TAB_TREASURER = 2
        const val TAB_BYLAWS = 3
        const val TAB_PROFILE = 4
        const val TAB_HISTORY = 5
    }

    private var currentTab = TAB_HOME

    // Containers
    private lateinit var dashboardScrollView: ScrollView
    private lateinit var fragmentContainer: FrameLayout

    // Nav Bar Views
    private lateinit var navTabHome: LinearLayout
    private lateinit var navTabLoan: LinearLayout
    private lateinit var navTabLedger: LinearLayout
    private lateinit var navTabTreasurer: LinearLayout
    private lateinit var navTabBylaws: LinearLayout
    private lateinit var navTabProfile: LinearLayout

    private lateinit var tvNavTextHome: TextView
    private lateinit var tvNavTextLoan: TextView
    private lateinit var tvNavTextLedger: TextView
    private lateinit var tvNavTextTreasurer: TextView
    private lateinit var tvNavTextBylaws: TextView
    private lateinit var tvNavTextProfile: TextView

    private lateinit var tvToolbarSubtitle: TextView
    private lateinit var tvMainGreeting: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dashboardScrollView = findViewById(R.id.dashboardScrollView)
        fragmentContainer = findViewById(R.id.fragmentContainer)

        navTabHome = findViewById(R.id.navTabHome)
        navTabLoan = findViewById(R.id.navTabLoan)
        navTabLedger = findViewById(R.id.navTabLedger)
        navTabTreasurer = findViewById(R.id.navTabTreasurer)
        navTabBylaws = findViewById(R.id.navTabBylaws)
        navTabProfile = findViewById(R.id.navTabProfile)

        tvNavTextHome = findViewById(R.id.tvNavTextHome)
        tvNavTextLoan = findViewById(R.id.tvNavTextLoan)
        tvNavTextLedger = findViewById(R.id.tvNavTextLedger)
        tvNavTextTreasurer = findViewById(R.id.tvNavTextTreasurer)
        tvNavTextBylaws = findViewById(R.id.tvNavTextBylaws)
        tvNavTextProfile = findViewById(R.id.tvNavTextProfile)

        tvToolbarSubtitle = findViewById(R.id.tvToolbarSubtitle)
        tvMainGreeting = findViewById(R.id.tvMainGreeting)

        // Populate personalized user name and role from SharedPreferences
        val prefs = getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "User") ?: "User"
        val userRole = prefs.getString("user_role", "MEMBER") ?: "MEMBER"
        val isTreasurer = (userRole == "TREASURER")
        val chamaName = prefs.getString("chama_name", "Mwangaza Chama") ?: "Mwangaza Chama"
        val groupCode = prefs.getString("group_code", "CH-4482") ?: "CH-4482"
        val memberId = prefs.getString("member_id", if (isTreasurer) "TRS-0191901" else "MBR-0191959") ?: (if (isTreasurer) "TRS-0191901" else "MBR-0191959")

        tvMainGreeting.text = "Welcome, $userName"
        findViewById<TextView>(R.id.tvMainChamaHeader)?.text = "${chamaName.uppercase(java.util.Locale.US)} • $groupCode"
        findViewById<TextView>(R.id.tvMainMemberSubtitle)?.text = "${if (isTreasurer) "Treasurer ID" else "Member ID"}: $memberId • ${if (isTreasurer) "Admin Console" else "Active Tier 1"}"
        val roleBadge = findViewById<TextView>(R.id.tvMainRoleBadge)
        if (isTreasurer) {
            roleBadge?.text = "TREASURER"
            roleBadge?.setBackgroundResource(R.drawable.bg_badge_pending)
            roleBadge?.setTextColor(ContextCompat.getColor(this, R.color.status_pending))
        } else {
            roleBadge?.text = "MEMBER"
            roleBadge?.setBackgroundResource(R.drawable.bg_badge_approved)
            roleBadge?.setTextColor(ContextCompat.getColor(this, R.color.status_approved))
        }

        // Role-based visibility: Members cannot view or access the Treasury Console
        if (!isTreasurer) {
            navTabTreasurer.visibility = View.GONE
            findViewById<LinearLayout>(R.id.cardActionTreasury)?.visibility = View.GONE
        } else {
            navTabTreasurer.visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.cardActionTreasury)?.visibility = View.VISIBLE
        }

        // Auto-configure Backend URL based on device (Emulator vs Physical Phone) or saved preference
        val savedBaseUrl = prefs.getString("backend_base_url", null)
        if (savedBaseUrl != null) {
            RetrofitClient.setBaseUrl(savedBaseUrl)
        } else {
            RetrofitClient.setBaseUrl(RetrofitClient.getDefaultBaseUrl())
        }

        // 1. Direct Nav Bar click listeners
        navTabHome.setOnClickListener { showDashboard() }
        navTabLoan.setOnClickListener { switchTab(TAB_LOAN) }
        navTabLedger.setOnClickListener { switchTab(TAB_LEDGER) }
        navTabTreasurer.setOnClickListener {
            if (isTreasurer) {
                switchTab(TAB_TREASURER)
            } else {
                Toast.makeText(this, "Access Denied: Restricted to Chama Treasurers.", Toast.LENGTH_SHORT).show()
            }
        }
        navTabBylaws.setOnClickListener { switchTab(TAB_BYLAWS) }
        navTabProfile.setOnClickListener { switchTab(TAB_PROFILE) }

        // 2. Direct One-Tap Action Cards on Dashboard
        findViewById<LinearLayout>(R.id.cardActionApply).setOnClickListener {
            switchTab(TAB_LOAN)
        }
        findViewById<LinearLayout>(R.id.cardActionHistory).setOnClickListener {
            switchTab(TAB_HISTORY)
        }
        findViewById<LinearLayout>(R.id.cardActionLedger).setOnClickListener {
            switchTab(TAB_LEDGER)
        }
        findViewById<LinearLayout>(R.id.cardActionTreasury).setOnClickListener {
            if (isTreasurer) {
                switchTab(TAB_TREASURER)
            } else {
                Toast.makeText(this, "Access Denied: Restricted to Chama Treasurers.", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Quick Overflow Menu
        findViewById<Button>(R.id.btnQuickMenu).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 100, 0, "🏠  Home Dashboard")
            popup.menu.add(0, TAB_LOAN + 1, 1, "💳  Apply for Loan")
            popup.menu.add(0, TAB_HISTORY + 1, 2, "📋  Loan Records & History")
            popup.menu.add(0, TAB_LEDGER + 1, 3, "📊  My Ledger & Statement")
            if (isTreasurer) {
                popup.menu.add(0, TAB_TREASURER + 1, 4, "💼  Treasurer Console")
            }
            popup.menu.add(0, TAB_BYLAWS + 1, 5, "📜  Chama Bylaws")
            popup.menu.add(0, TAB_PROFILE + 1, 6, "👤  User Profile & Settings")
            if (isTreasurer) {
                popup.menu.add(1, 10, 7, "⚡  Prime Member (Demo)")
                popup.menu.add(1, 11, 8, "⚡  Guarantor Rescue (Demo)")
                popup.menu.add(1, 12, 9, "⚡  3× Cap Exceeded (Demo)")
                popup.menu.add(1, 13, 10, "⚡  Bad Credit Default (Demo)")
            }
            popup.menu.add(2, 999, 11, "🔌  Ping Backend")
            popup.menu.add(2, 998, 12, "⚙️  Configure Backend Host")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    100 -> showDashboard()
                    TAB_LOAN + 1 -> switchTab(TAB_LOAN)
                    TAB_HISTORY + 1 -> switchTab(TAB_HISTORY)
                    TAB_LEDGER + 1 -> switchTab(TAB_LEDGER)
                    TAB_TREASURER + 1 -> switchTab(TAB_TREASURER)
                    TAB_BYLAWS + 1 -> switchTab(TAB_BYLAWS)
                    TAB_PROFILE + 1 -> switchTab(TAB_PROFILE)
                    10 -> navigateToLoanWithPreset(LoanPresets.PRIME)
                    11 -> navigateToLoanWithPreset(LoanPresets.GUARANTOR)
                    12 -> navigateToLoanWithPreset(LoanPresets.CAP_EXCEEDED)
                    13 -> navigateToLoanWithPreset(LoanPresets.BAD_CREDIT)
                    999 -> testBackendConnection()
                    998 -> showConfigureHostDialog()
                }
                true
            }
            popup.show()
        }

        // Default: Start on Home Dashboard
        showDashboard()
    }

    /**
     * Dialog allowing one-tap switching between Emulator, USB cable (adb reverse), or local Wi-Fi.
     */
    fun showConfigureHostDialog() {
        val options = arrayOf(
            "📱 Physical Phone via USB Cable (http://127.0.0.1:8000/)",
            "🌐 Physical Phone via Wi-Fi (http://10.0.11.142:8000/)",
            "💻 Android Studio Emulator (http://10.0.2.2:8000/)",
            "✏️ Enter Custom IP Address..."
        )
        AlertDialog.Builder(this)
            .setTitle("Select Backend Host (${if (RetrofitClient.isEmulator()) "Emulator" else "Physical Phone"})")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> saveAndApplyBackendUrl("http://127.0.0.1:8000/")
                    1 -> saveAndApplyBackendUrl("http://10.0.11.142:8000/")
                    2 -> saveAndApplyBackendUrl("http://10.0.2.2:8000/")
                    3 -> showCustomIpInputDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomIpInputDialog() {
        val input = EditText(this)
        input.hint = "e.g. http://192.168.1.50:8000/"
        input.setText(RetrofitClient.getBaseUrl())
        AlertDialog.Builder(this)
            .setTitle("Enter Custom Backend URL")
            .setView(input)
            .setPositiveButton("Save & Test") { _, _ ->
                val entered = input.text.toString().trim()
                if (entered.isNotEmpty()) {
                    val formatted = if (entered.startsWith("http://") || entered.startsWith("https://")) entered else "http://$entered"
                    saveAndApplyBackendUrl(formatted)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAndApplyBackendUrl(url: String) {
        val prefs = getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("backend_base_url", url).apply()
        RetrofitClient.setBaseUrl(url)
        Toast.makeText(this, "Backend host set to: $url", Toast.LENGTH_SHORT).show()
        testBackendConnection()
    }

    /**
     * Pings the FastAPI backend service to verify connectivity.
     * Useful for examiner demonstration and offline diagnostics.
     */
    fun testBackendConnection() {
        val baseUrl = RetrofitClient.getBaseUrl()
        Toast.makeText(this, "Pinging FastAPI backend at $baseUrl...", Toast.LENGTH_SHORT).show()
        RetrofitClient.apiService.getHealthCheck().enqueue(object : retrofit2.Callback<Map<String, Any>> {
            override fun onResponse(call: retrofit2.Call<Map<String, Any>>, response: retrofit2.Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Connected! Backend is ONLINE (HTTP 200) at $baseUrl", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Backend reached: HTTP ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                val isEmu = RetrofitClient.isEmulator()
                val advice = if (!isEmu && baseUrl.contains("10.0.2.2")) {
                    "\n\nTip: You are on a physical phone! 10.0.2.2 only works on emulators. Go to 'Configure Backend Host' -> USB (127.0.0.1) or Wi-Fi (10.0.11.142)."
                } else if (!isEmu && baseUrl.contains("127.0.0.1")) {
                    "\n\nTip: For USB, ensure 'adb reverse tcp:8000 tcp:8000' is active, or select Wi-Fi host."
                } else {
                    "\n\nEnsure FastAPI is running on port 8000."
                }
                Toast.makeText(this@MainActivity, "Backend OFFLINE: ${t.localizedMessage ?: "Failed to connect"}$advice", Toast.LENGTH_LONG).show()
            }
        })
    }

    /** Shows the main activity dashboard directly */
    fun showDashboard() {
        currentTab = TAB_HOME
        dashboardScrollView.visibility = View.VISIBLE
        fragmentContainer.visibility = View.GONE
        dashboardScrollView.smoothScrollTo(0, 0)
        updateNavAppearance(TAB_HOME)
    }

    /** Switches to any of the sub-screens inside the Fragment container */
    fun switchTab(tab: Int) {
        val prefs = getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)
        val userRole = prefs.getString("user_role", "MEMBER") ?: "MEMBER"
        if (tab == TAB_TREASURER && userRole != "TREASURER") {
            Toast.makeText(this, "Access Denied: The Treasury Console is restricted to Chama Treasurers.", Toast.LENGTH_LONG).show()
            return
        }

        currentTab = tab
        dashboardScrollView.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE

        val fragment: Fragment = when (tab) {
            TAB_LEDGER -> LedgerFragment()
            TAB_TREASURER -> TreasurerFragment()
            TAB_BYLAWS -> BylawsFragment()
            TAB_PROFILE -> ProfileFragment()
            TAB_HISTORY -> LoanHistoryFragment()
            else -> LoanFragment.newInstance()
        }
        commitFragment(fragment, animate = true)
        updateNavAppearance(tab)
    }

    /** Navigate to Loan screen with a preset scenario pre-filled */
    fun navigateToLoanWithPreset(preset: LoanPreset) {
        currentTab = TAB_LOAN
        dashboardScrollView.visibility = View.GONE
        fragmentContainer.visibility = View.VISIBLE
        commitFragment(LoanFragment.newInstance(preset), animate = true)
        updateNavAppearance(TAB_LOAN)
    }

    private fun commitFragment(fragment: Fragment, animate: Boolean) {
        val tx: FragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate) {
            tx.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        }
        tx.replace(R.id.fragmentContainer, fragment)
        tx.commit()
    }

    private fun updateNavAppearance(activeTab: Int) {
        val colorActive = ContextCompat.getColor(this, R.color.brand_primary)
        val colorInactive = ContextCompat.getColor(this, R.color.text_secondary)

        val tabs = listOf(navTabHome, navTabLoan, navTabLedger, navTabTreasurer, navTabBylaws, navTabProfile)
        val labels = listOf(tvNavTextHome, tvNavTextLoan, tvNavTextLedger, tvNavTextTreasurer, tvNavTextBylaws, tvNavTextProfile)
        val tabIds = listOf(TAB_HOME, TAB_LOAN, TAB_LEDGER, TAB_TREASURER, TAB_BYLAWS, TAB_PROFILE)

        val subtitles = mapOf(
            TAB_HOME to "Home Dashboard",
            TAB_LOAN to "Loan Application",
            TAB_LEDGER to "My Ledger",
            TAB_TREASURER to "Treasurer Console",
            TAB_BYLAWS to "Chama Bylaws",
            TAB_PROFILE to "User Profile",
            TAB_HISTORY to "Loan Records & History"
        )

        tabIds.forEachIndexed { index, id ->
            val isActive = (id == activeTab)
            tabs[index].setBackgroundResource(if (isActive) R.drawable.bg_nav_item_active else android.R.color.transparent)
            labels[index].setTextColor(if (isActive) colorActive else colorInactive)
            labels[index].setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
        }

        tvToolbarSubtitle.text = subtitles[activeTab] ?: "Chama Trust"
    }

    override fun onBackPressed() {
        if (currentTab != TAB_HOME) {
            showDashboard()
        } else {
            super.onBackPressed()
        }
    }
}
