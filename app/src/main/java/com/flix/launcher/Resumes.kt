package com.flix.launcher

import android.content.Context

/**
 * Notes when the user opens each app from the launcher (home tiles, All apps). Kept per
 * package so a future feature can rank apps by recency; the usage-log permission (granted
 * once with adb) stays in the manifest if system-wide recency is ever needed again. The
 * app resume cards this once fed are gone — Continue watching is Nuvio titles only, since
 * other apps' per-title progress can't be read and bare app icons looked out of place.
 */
object Resumes {
    private fun prefs(context: Context) = context.getSharedPreferences("flix_resumes", Context.MODE_PRIVATE)

    /** Records that the user just opened [pkg] from the launcher. */
    fun note(context: Context, pkg: String?) {
        if (pkg.isNullOrBlank() || pkg == context.packageName) return
        prefs(context).edit().putLong(pkg, System.currentTimeMillis()).apply()
    }

    /** Every package with a launch note — the ghost purge checks these against what's installed. */
    fun packages(context: Context): Set<String> = prefs(context).all.keys

    /** Drops an uninstalled app's note. */
    fun forget(context: Context, pkg: String): Boolean {
        if (!prefs(context).contains(pkg)) return false
        prefs(context).edit().remove(pkg).apply()
        return true
    }
}
