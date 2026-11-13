package com.example.chamaloanapp

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
    }

    private var currentTab = TAB_LOAN

    // ─── Bottom Nav Views ───────────────────────────────────────────────────
    private lateinit var navTabLoan: LinearLayout
    private lateinit var navTabLedger: LinearLayout
    private lateinit var navTabTreasurer: LinearLayout
    private lateinit var navTabBylaws: LinearLayout

    private lateinit var tvNavTextLoan: TextView
    private lateinit var tvNavTextLedger: TextView
    private lateinit var tvNavTextTreasurer: TextView
    private lateinit var tvNavTextBylaws: TextView

    private lateinit var tvToolbarSubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind nav bar
        navTabLoan = findViewById(R.id.navTabLoan)
        navTabLedger = findViewById(R.id.navTabLedger)
        navTabTreasurer = findViewById(R.id.navTabTreasurer)
        navTabBylaws = findViewById(R.id.navTabBylaws)
        tvNavTextLoan = findViewById(R.id.tvNavTextLoan)
        tvNavTextLedger = findViewById(R.id.tvNavTextLedger)
        tvNavTextTreasurer = findViewById(R.id.tvNavTextTreasurer)
        tvNavTextBylaws = findViewById(R.id.tvNavTextBylaws)
        tvToolbarSubtitle = findViewById(R.id.tvToolbarSubtitle)

        // Nav click listeners
        navTabLoan.setOnClickListener { switchTab(TAB_LOAN) }
        navTabLedger.setOnClickListener { switchTab(TAB_LEDGER) }
        navTabTreasurer.setOnClickListener { switchTab(TAB_TREASURER) }
        navTabBylaws.setOnClickListener { switchTab(TAB_BYLAWS) }

        // Quick overflow menu
        findViewById<Button>(R.id.btnQuickMenu).setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, TAB_LOAN + 1, 0, "💳  Loan Application")
            popup.menu.add(0, TAB_LEDGER + 1, 1, "📊  My Ledger & Statement")
            popup.menu.add(0, TAB_TREASURER + 1, 2, "💼  Treasurer Console")
            popup.menu.add(0, TAB_BYLAWS + 1, 3, "📜  Chama Bylaws")
            popup.menu.add(1, 10, 4, "⚡  Prime Member (Demo)")
            popup.menu.add(1, 11, 5, "⚡  Guarantor Rescue (Demo)")
            popup.menu.add(1, 12, 6, "⚡  3× Cap Exceeded (Demo)")
            popup.menu.add(1, 13, 7, "⚡  Bad Credit Default (Demo)")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    TAB_LOAN + 1 -> switchTab(TAB_LOAN)
                    TAB_LEDGER + 1 -> switchTab(TAB_LEDGER)
                    TAB_TREASURER + 1 -> switchTab(TAB_TREASURER)
                    TAB_BYLAWS + 1 -> switchTab(TAB_BYLAWS)
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

        val tabs = listOf(navTabLoan, navTabLedger, navTabTreasurer, navTabBylaws)
        val labels = listOf(tvNavTextLoan, tvNavTextLedger, tvNavTextTreasurer, tvNavTextBylaws)
        val subtitles = listOf("Loan Application", "My Ledger", "Treasurer Console", "Chama Bylaws")

        tabs.forEachIndexed { index, tab ->
            val isActive = index == activeTab
            tab.setBackgroundResource(if (isActive) R.drawable.bg_nav_item_active else android.R.color.transparent)
            labels[index].setTextColor(if (isActive) colorActive else colorInactive)
            labels[index].setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
        }

        tvToolbarSubtitle.text = subtitles[activeTab]
    }
}
