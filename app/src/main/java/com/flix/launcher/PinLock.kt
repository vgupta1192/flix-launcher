package com.flix.launcher

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The parental PIN: one code, hashed with a random salt, that gates two things —
 * individually locked apps (long-press an app → "Require PIN") and Kids mode, a home
 * that shows only kid-tagged apps and hides the library tabs until the PIN is entered.
 * An unlock lasts for this visit to the launcher: going into any app clears it, so a
 * kid can't unlock once and keep going. With no PIN set nothing is gated.
 */
object PinLock {
    private fun prefs(context: Context) = context.getSharedPreferences("pin", Context.MODE_PRIVATE)

    fun hasPin(context: Context) = prefs(context).getString("hash", null) != null

    fun kidsMode(context: Context) = hasPin(context) && prefs(context).getBoolean("kids", false)
    fun kidApps(context: Context) = prefs(context).getStringSet("kid_apps", emptySet()).orEmpty()
    fun lockedApps(context: Context) = prefs(context).getStringSet("locked_apps", emptySet()).orEmpty()

    fun setKids(context: Context, on: Boolean) = prefs(context).edit().putBoolean("kids", on).apply()

    /** Returns the new state. */
    fun toggleKidApp(context: Context, pkg: String): Boolean {
        val set = kidApps(context).toMutableSet()
        val on = pkg !in set
        if (on) set += pkg else set -= pkg
        prefs(context).edit().putStringSet("kid_apps", set).apply()
        return on
    }

    /** Returns the new state. */
    fun toggleLockedApp(context: Context, pkg: String): Boolean {
        val set = lockedApps(context).toMutableSet()
        val on = pkg !in set
        if (on) set += pkg else set -= pkg
        prefs(context).edit().putStringSet("locked_apps", set).apply()
        return on
    }

    /** True while this visit to the launcher is unlocked; [clearSession] on stop re-arms the gate. */
    @Volatile
    var sessionUnlocked = false
    fun clearSession() { sessionUnlocked = false }

    /**
     * Runs [onPass] once the gate is satisfied: straight through when no PIN is set or
     * [pkg] is neither locked nor (in Kids mode) outside the kid set, else the PIN dialog.
     */
    fun gate(activity: Activity, pkg: String?, onPass: () -> Unit) {
        if (!hasPin(activity) || sessionUnlocked) return onPass()
        val blocked = pkg != null && (pkg in lockedApps(activity) ||
            kidsMode(activity) && kidApps(activity).isNotEmpty() && pkg !in kidApps(activity))
        if (!blocked) return onPass()
        enter(activity, "Parental PIN") { ok -> if (ok) { sessionUnlocked = true; onPass() } }
    }

    // ---- dialogs ----

    /** Asks for the PIN; [onDone] is false on cancel or a wrong code. */
    fun enter(activity: Activity, title: String, onDone: (Boolean) -> Unit) {
        keypad(activity, title) { code ->
            when {
                code == null -> onDone(false)
                matches(activity, code) -> onDone(true)
                else -> {
                    Toast.makeText(activity, "Wrong PIN", Toast.LENGTH_SHORT).show()
                    enter(activity, title, onDone)
                }
            }
        }
    }

    /** Sets a new PIN (typed twice); [onSet] runs only after it's stored. */
    fun setNew(activity: Activity, onSet: () -> Unit) {
        keypad(activity, "Choose a PIN (3–8 digits)") { first ->
            when {
                first == null -> {}
                else -> keypad(activity, "Confirm the PIN") { second ->
                    when {
                        second == null -> {}
                        second == first -> {
                            store(activity, first)
                            sessionUnlocked = true
                            Toast.makeText(activity, "PIN set", Toast.LENGTH_SHORT).show()
                            onSet()
                        }
                        else -> {
                            Toast.makeText(activity, "They didn't match — try again", Toast.LENGTH_SHORT).show()
                            setNew(activity, onSet)
                        }
                    }
                }
            }
        }
    }

    /** The D-pad keypad: digits, backspace and OK on a grid; [onDone] gets null on cancel. */
    private fun keypad(activity: Activity, title: String, onDone: (String?) -> Unit) {
        val dp = activity.resources.displayMetrics.density
        val dialog = Dialog(activity)
        var code = ""
        val dots = TextView(activity).apply {
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            letterSpacing = 0.3f
            setPadding(0, (10 * dp).toInt(), 0, (6 * dp).toInt())
        }
        fun repaint() { dots.text = code.map { '●' }.joinToString("").ifEmpty { " " } }
        fun key(label: String): TextView = TextView(activity).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 19f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            isFocusable = true
            setBackgroundResource(R.drawable.menu_item)
            setTextColor(activity.getColorStateList(R.color.menu_text))
            layoutParams = LinearLayout.LayoutParams((62 * dp).toInt(), (44 * dp).toInt())
                .apply { setMargins((3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt()) }
            setOnClickListener {
                when (label) {
                    "⌫" -> { code = code.dropLast(1); repaint() }
                    "✓" -> if (code.length >= 3) { dialog.dismiss(); onDone(code) }
                        else Toast.makeText(activity, "Enter at least 3 digits", Toast.LENGTH_SHORT).show()
                    else -> if (code.length < 8) { code += label; repaint() }
                }
            }
        }
        val grid = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "✓")))
                addView(LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; row.forEach { addView(key(it)) } })
        }
        val panel = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.menu_panel)
            setPadding((14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
            layoutParams = ViewGroup.LayoutParams((226 * dp).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            addView(TextView(activity).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(dots)
            addView(grid)
        }
        dialog.setContentView(panel)
        dialog.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.55f)
            setWindowAnimations(0)
            setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        (grid.getChildAt(0) as LinearLayout).getChildAt(1).post { (grid.getChildAt(0) as LinearLayout).getChildAt(1).requestFocus() }
        dialog.setOnCancelListener { onDone(null) }
        dialog.show()
        panel.alpha = 0f
        panel.scaleX = 0.9f; panel.scaleY = 0.9f
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220)
            .setInterpolator(OvershootInterpolator(1.1f)).withLayer().start()
    }

    // ---- storage: salted SHA-256, never the PIN itself ----

    private fun salted(context: Context, pin: String, salt: String) =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }

    private fun store(context: Context, pin: String) {
        val salt = ByteArray(8).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        prefs(context).edit().putString("salt", salt).putString("hash", salted(context, pin, salt)).apply()
    }

    private fun matches(context: Context, pin: String): Boolean {
        val salt = prefs(context).getString("salt", null) ?: return false
        return prefs(context).getString("hash", null) == salted(context, pin, salt)
    }
}
