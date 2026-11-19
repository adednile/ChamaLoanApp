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

        // Populate personalized user name from SharedPreferences
        val prefs = getSharedPreferences("chama_user_prefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Eli Ndeda") ?: "Eli Ndeda"
        tvMainGreeting.text = "Welcome, $userName"

        // 1. Direct Nav Bar click listeners
        navTabHome.setOnClickListener { showDashboard() }
        navTabLoan.setOnClickListener { switchTab(TAB_LOAN) }
        navTabLedger.setOnClickListener { switchTab(TAB_LEDGER) }
        navTabTreasurer.setOnClickListener { switchTab(TAB_TREASURER) }
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
            switchTab(TAB_TREASURER)
        }

        // 3. Quick Overflow Menu
        findViewById<Button>(R.id.btnQuickMenu).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 100, 0, "🏠  Home Dashboard")
            popup.menu.add(0, TAB_LOAN + 1, 1, "💳  Apply for Loan")
            popup.menu.add(0, TAB_HISTORY + 1, 2, "📋  Loan Records & History")
            popup.menu.add(0, TAB_LEDGER + 1, 3, "📊  My Ledger & Statement")
            popup.menu.add(0, TAB_TREASURER + 1, 4, "💼  Treasurer Console")
            popup.menu.add(0, TAB_BYLAWS + 1, 5, "📜  Chama Bylaws")
            popup.menu.add(0, TAB_PROFILE + 1, 6, "👤  User Profile & Settings")
            popup.menu.add(1, 10, 7, "⚡  Prime Member (Demo)")
            popup.menu.add(1, 11, 8, "⚡  Guarantor Rescue (Demo)")
            popup.menu.add(1, 12, 9, "⚡  3× Cap Exceeded (Demo)")
            popup.menu.add(1, 13, 10, "⚡  Bad Credit Default (Demo)")
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
                }
                true
            }
            popup.show()
        }

        // Default: Start on Home Dashboard
        showDashboard()
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
