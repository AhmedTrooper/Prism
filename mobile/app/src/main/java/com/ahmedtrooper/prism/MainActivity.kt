package com.ahmedtrooper.prism

import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private enum class Tab { LOCAL, STREAMS, ME }

    private var currentTab = Tab.LOCAL

    private val localFragment by lazy {
        supportFragmentManager.findFragmentByTag(TAG_LOCAL) ?: MainScreenFragment()
    }
    private val streamsFragment by lazy {
        supportFragmentManager.findFragmentByTag(TAG_STREAMS) ?: OnlineStreamsFragment()
    }
    private val meFragment by lazy {
        supportFragmentManager.findFragmentByTag(TAG_ME) ?: MeFragment()
    }

    private var splashOverlay: View? = null
    private var splashLogo: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from window splash theme to the app's Material 3 library theme
        // so dialogs, popups, and chips all pick up the global styling.
        setTheme(R.style.Theme_Prism_Library)
        super.onCreate(savedInstanceState)

        supportActionBar?.setTitle(R.string.app_name)

        splashOverlay = findViewById(R.id.splash_overlay)
        splashLogo = findViewById(R.id.splash_logo)

        wireBottomNav()

        if (savedInstanceState == null) {
            // Cold launch: show Local tab
            showTab(Tab.LOCAL, animate = false)
            animateSplash()
        } else {
            // Configuration change / rotation: skip splash immediately
            splashOverlay?.visibility = View.GONE
            // Restore current tab from saved state so rotation keeps the user's tab
            currentTab = Tab.valueOf(
                savedInstanceState.getString(STATE_TAB) ?: Tab.LOCAL.name
            )
            // Re-bind the cached fragments since the FragmentManager holds them already
            bindFragmentsToFields()
            refreshTabHighlights()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TAB, currentTab.name)
    }

    /**
     * Rescan media library. Called by MeFragment's "Media Manager" quick action.
     * Returns the user to the Local tab and triggers a fresh scan there.
     */
    fun rescanLibrary() {
        showTab(Tab.LOCAL, animate = true)
        // Forward the rescan to the currently attached Local fragment, if any
        (localFragment as? MainScreenFragment)?.let { fragment ->
            fragment.userRequestedRescan()
        }
    }

    private fun wireBottomNav() {
        findViewById<View>(R.id.tabLocal).setOnClickListener {
            if (currentTab != Tab.LOCAL) showTab(Tab.LOCAL, animate = true)
        }
        findViewById<View>(R.id.tabStreams).setOnClickListener {
            if (currentTab != Tab.STREAMS) showTab(Tab.STREAMS, animate = true)
        }
        findViewById<View>(R.id.tabMe).setOnClickListener {
            if (currentTab != Tab.ME) showTab(Tab.ME, animate = true)
        }
    }

    private fun bindFragmentsToFields() {
        // After rotation, supportFragmentManager has already re-attached fragments.
        // We only need to refresh cached references so rescanLibrary() can find them.
        supportFragmentManager.findFragmentByTag(TAG_LOCAL)
            ?.let { /* keep lazy-resolved to already-existing instance */ }
    }

    private fun showTab(tab: Tab, animate: Boolean) {
        val (fragment, tag) = when (tab) {
            Tab.LOCAL -> localFragment to TAG_LOCAL
            Tab.STREAMS -> streamsFragment to TAG_STREAMS
            Tab.ME -> meFragment to TAG_ME
        }

        currentTab = tab
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            // setReorderingAllowed ensures save/restore happens correctly per fragment
            replace(R.id.fragment_container_view, fragment, tag)
        }

        refreshTabHighlights()
    }

    private fun refreshTabHighlights() {
        applyTabStyle(
            R.id.tabLocal, R.id.tabLocalIcon, R.id.tabLocalTxt,
            active = currentTab == Tab.LOCAL
        )
        applyTabStyle(
            R.id.tabStreams, R.id.tabStreamsIcon, R.id.tabStreamsTxt,
            active = currentTab == Tab.STREAMS
        )
        applyTabStyle(
            R.id.tabMe, R.id.tabMeIcon, R.id.tabMeTxt,
            active = currentTab == Tab.ME
        )
    }

    private fun applyTabStyle(
        tabId: Int, iconId: Int, textId: Int, active: Boolean
    ) {
        val tab = findViewById<View>(tabId) ?: return
        tab.isSelected = active

        // Resolve theme attribute for inactive state so we follow light/dark mode
        val inactiveColor = resolveInactiveTextColor()
        val activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.mx_blue)
        val iconTint = android.content.res.ColorStateList.valueOf(
            if (active) activeColor else inactiveColor
        )

        findViewById<ImageView>(iconId)?.imageTintList = iconTint
        findViewById<TextView>(textId)?.setTextColor(
            if (active) activeColor else inactiveColor
        )
        findViewById<TextView>(textId)?.setTypeface(
            null,
            if (active) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
        )
    }

    private fun resolveInactiveTextColor(): Int {
        val typedValue = android.util.TypedValue()
        return if (theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true))
            typedValue.data
        else
            androidx.core.content.ContextCompat.getColor(this, R.color.mx_text_secondary)
    }

    private fun animateSplash() {
        splashLogo?.apply {
            scaleX = 0.85f
            scaleY = 0.85f
            alpha = 0f
            animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        splashOverlay?.apply {
            postDelayed({
                if (!isFinishing && !isDestroyed) {
                    animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction { visibility = View.GONE }
                        .start()
                }
            }, 1000)

            setOnClickListener {
                animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { visibility = View.GONE }
                    .start()
            }
        }
    }

    companion object {
        private const val TAG_LOCAL = "tab_local"
        private const val TAG_STREAMS = "tab_streams"
        private const val TAG_ME = "tab_me"
        private const val STATE_TAB = "current_tab"
    }
}
