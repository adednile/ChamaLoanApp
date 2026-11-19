package com.example.chamaloanapp

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAB_LOAN = 0
        const val TAB_LEDGER = 1
        const val TAB_TREASURER = 2
        const val TAB_BYLAWS = 3
        const val TAB_PROFILE = 4
        const val TAB_HISTORY = 5
    }

    private var currentTab = TAB_LOAN

    // ─── Bottom Nav Views ───────────────────────────────────────────────────
    private lateinit var navTabLoan: LinearLayout
    private lateinit var navTabLedger: LinearLayout
    private lateinit var navTabTreasurer: LinearLayout
    private lateinit var navTabBylaws: LinearLayout
    private lateinit var navTabProfile: LinearLayout

    private lateinit var tvNavTextLoan: TextView
    private lateinit var tvNavTextLedger: TextView
    private lateinit var tvNavTextTreasurer: TextView
    private lateinit var tvNavTextBylaws: TextView
    private lateinit var tvNavTextProfile: TextView

    private lateinit var tvToolbarSubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind nav bar
        navTabLoan = findViewById(R.id.navTabLoan)
        navTabLedger = findViewById(R.id.navTabLedger)
        navTabTreasurer = findViewById(R.id.navTabTreasurer)
        navTabBylaws = findViewById(R.id.navTabBylaws)
        navTabProfile = findViewById(R.id.navTabProfile)

        tvNavTextLoan = findViewById(R.id.tvNavTextLoan)
        tvNavTextLedger = findViewById(R.id.tvNavTextLedger)
        tvNavTextTreasurer = findViewById(R.id.tvNavTextTreasurer)
        tvNavTextBylaws = findViewById(R.id.tvNavTextBylaws)
        tvNavTextProfile = findViewById(R.id.tvNavTextProfile)

        tvToolbarSubtitle = findViewById(R.id.tvToolbarSubtitle)

        // Nav click listeners
        navTabLoan.setOnClickListener { switchTab(TAB_LOAN) }
        navTabLedger.setOnClickListener { switchTab(TAB_LEDGER) }
        navTabTreasurer.setOnClickListener { switchTab(TAB_TREASURER) }
        navTabBylaws.setOnClickListener { switchTab(TAB_BYLAWS) }
        navTabProfile.setOnClickListener { switchTab(TAB_PROFILE) }

        // Quick overflow menu
        findViewById<Button>(R.id.btnQuickMenu).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, TAB_LOAN + 1, 0, "💳  Loan Application")
            popup.menu.add(0, TAB_HISTORY + 1, 1, "📋  Loan Records & History")
            popup.menu.add(0, TAB_LEDGER + 1, 2, "📊  My Ledger & Statement")
            popup.menu.add(0, TAB_TREASURER + 1, 3, "💼  Treasurer Console")
            popup.menu.add(0, TAB_BYLAWS + 1, 4, "📜  Chama Bylaws")
            popup.menu.add(0, TAB_PROFILE + 1, 5, "👤  User Profile & Settings")
            popup.menu.add(1, 10, 6, "⚡  Prime Member (Demo)")
            popup.menu.add(1, 11, 7, "⚡  Guarantor Rescue (Demo)")
            popup.menu.add(1, 12, 8, "⚡  3× Cap Exceeded (Demo)")
            popup.menu.add(1, 13, 9, "⚡  Bad Credit Default (Demo)")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
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

        // Load the initial screen (restore on rotation)
        if (savedInstanceState == null) {
            commitFragment(LoanFragment.newInstance(), animate = false)
        }
        updateNavAppearance(TAB_LOAN)
    }

    // ── Public API ──────────────────────────────────────────────────────────

    fun switchTab(tab: Int) {
        if (tab == currentTab) return
        currentTab = tab
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
        commitFragment(LoanFragment.newInstance(preset), animate = true)
        updateNavAppearance(TAB_LOAN)
    }

    // ── Private helpers ─────────────────────────────────────────────────────

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

        val tabs = listOf(navTabLoan, navTabLedger, navTabTreasurer, navTabBylaws, navTabProfile)
        val labels = listOf(tvNavTextLoan, tvNavTextLedger, tvNavTextTreasurer, tvNavTextBylaws, tvNavTextProfile)
        val subtitles = mapOf(
            TAB_LOAN to "Loan Application",
            TAB_LEDGER to "My Ledger",
            TAB_TREASURER to "Treasurer Console",
            TAB_BYLAWS to "Chama Bylaws",
            TAB_PROFILE to "User Profile",
            TAB_HISTORY to "Loan Records & History"
        )

        tabs.forEachIndexed { index, tab ->
            val isActive = index == activeTab
            tab.setBackgroundResource(if (isActive) R.drawable.bg_nav_item_active else android.R.color.transparent)
            labels[index].setTextColor(if (isActive) colorActive else colorInactive)
            labels[index].setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
        }

        tvToolbarSubtitle.text = subtitles[activeTab] ?: "Chama Trust"
    }
}
