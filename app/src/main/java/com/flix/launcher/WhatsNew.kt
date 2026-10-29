package com.flix.launcher

import android.content.Context

/**
 * A small "What's new" card on the first home open after an update, also reachable from
 * Settings → About. The top line always describes the current release; keep it in step
 * with CHANGELOG.md when shipping.
 */
object WhatsNew {
    private val notes = listOf(
        "1.1.4" to "Flix reclaims the home default by itself after Google TV updates steal it",
        "1.1.3" to "Kids mode setup: the empty kids home shows All apps so apps can be tagged",
        "1.1.2" to "Continue watching: app cards now lead the row with their real icons",
        "1.1.0" to "Parental PIN with app locks and Kids mode, photo-frame idle and ambient tint",
        "1.0.8" to "Wallpaper drift, night dimming, surprise picks on the billboard and this card",
        "1.0.7" to "Smoother animations on 3 GB TVs (lite GPU path)",
        "1.0.6" to "Continue watching gained resume cards for all your apps",
        "1.0.4" to "Updates install properly now, with one clear confirmation",
        "1.0.3" to "Sign-in works against your own Nuvio backend, QR included",
        "1.0.2" to "Volume keys repeat again; your NuvioTV build is the one the launcher opens",
    )

    fun show(context: Context) {
        OptionsMenu.show(context, "What's new", notes.map { (v, text) -> "v$v — $text" to {} })
    }

    /** Shows the card once per installed version. */
    fun maybeShow(context: Context) {
        val prefs = context.getSharedPreferences("flix", Context.MODE_PRIVATE)
        val code = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode }.getOrDefault(0L)
        if (prefs.getLong("whats_new_code", 0L) >= code) return
        prefs.edit().putLong("whats_new_code", code).apply()
        show(context)
    }
}
