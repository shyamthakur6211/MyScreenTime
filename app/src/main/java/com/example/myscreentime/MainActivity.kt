package com.example.myscreentime

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import android.app.Activity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var dateLabel: TextView
    private lateinit var totalLabel: TextView
    private lateinit var list: LinearLayout
    private val day = Calendar.getInstance()
    private val fmt = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) refresh()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 20)
            setBackgroundColor(0xFF11111B.toInt())
        }
        val title = TextView(this).apply {
            text = "My Screen Time"
            textSize = 27f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title)
        val subtitle = TextView(this).apply {
            text = "Daily phone usage tracker"
            textSize = 14f
            setTextColor(0xFFB9B7C9.toInt())
            setPadding(0, 4, 0, 18)
        }
        root.addView(subtitle)

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val prev = Button(this).apply { text = "‹ Previous"; setOnClickListener { day.add(Calendar.DAY_OF_YEAR, -1); refresh() } }
        val next = Button(this).apply { text = "Next ›"; setOnClickListener { day.add(Calendar.DAY_OF_YEAR, 1); if (day.after(Calendar.getInstance())) day.timeInMillis = Calendar.getInstance().timeInMillis; refresh() } }
        nav.addView(prev, LinearLayout.LayoutParams(0, -2, 1f))
        nav.addView(next, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(nav)

        dateLabel = TextView(this).apply {
            textSize = 18f; gravity = Gravity.CENTER; setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 12, 0, 12)
        }
        root.addView(dateLabel)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(0xFF252538.toInt())
        }
        val cap = TextView(this).apply { text = "TOTAL SCREEN TIME"; textSize = 12f; setTextColor(0xFFB9B7C9.toInt()) }
        totalLabel = TextView(this).apply {
            text = "--"; textSize = 34f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 6, 0, 0)
        }
        card.addView(cap); card.addView(totalLabel)
        root.addView(card)

        val appTitle = TextView(this).apply {
            text = "App-wise usage"; textSize = 19f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt()); setPadding(0, 20, 0, 10)
        }
        root.addView(appTitle)

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val permission = Button(this).apply {
            text = "Enable Usage Access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }
        root.addView(permission)
        val note = TextView(this).apply {
            text = "Note: Android only provides usage records retained by the phone. This app cannot recover usage from before installation if Android has already removed that history."
            textSize = 11f; setTextColor(0xFFB9B7C9.toInt()); setPadding(0, 10, 0, 0)
        }
        root.addView(note)
        setContentView(root)
    }

    private fun hasUsagePermission(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun refresh() {
        dateLabel.text = fmt.format(day.time)
        list.removeAllViews()
        if (!hasUsagePermission()) {
            totalLabel.text = "Permission needed"
            addRow("Allow Usage Access", "Tap the button below, find My Screen Time, and enable usage access.")
            return
        }

        val start = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = (day.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats: List<UsageStats> = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }

        val total = stats.sumOf { it.totalTimeInForeground }
        totalLabel.text = formatDuration(total)

        if (stats.isEmpty()) {
            addRow("No usage data", "No app usage was reported for this date.")
            return
        }

        val pm = packageManager
        for (s in stats) {
            val appName = try {
                val appInfo = pm.getApplicationInfo(s.packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) { s.packageName }
            addRow(appName, formatDuration(s.totalTimeInForeground))
        }
    }

    private fun addRow(name: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 13, 14, 13)
            setBackgroundColor(0xFF1D1D2B.toInt())
        }
        val n = TextView(this).apply {
            text = name; textSize = 15f; setTextColor(0xFFFFFFFF.toInt())
        }
        val v = TextView(this).apply {
            text = value; textSize = 14f; setTextColor(0xFFC8B8FF.toInt())
            gravity = Gravity.END
        }
        row.addView(n, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(v)
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = 7
        list.addView(row, lp)
    }

    private fun formatDuration(ms: Long): String {
        val mins = TimeUnit.MILLISECONDS.toMinutes(ms)
        val hours = mins / 60
        val rem = mins % 60
        return if (hours > 0) "${hours}h ${rem}m" else "${rem}m"
    }
}
