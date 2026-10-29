package com.flix.launcher

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * The Continue watching row's app cards: the video apps the TV has been opening lately,
 * most recent first, which open the app itself on OK. Two recency sources are merged —
 * the launcher notes every app it opens (see [note]), and, with Usage access granted
 * once with adb, Android's own per-app log, which also covers voice and in-app launches.
 *
 * Android shares no per-title progress from other apps (the system's Watch Next store is
 * signature-only), so a card resumes by opening the app rather than a specific film.
 */
object Resumes {
    private const val WINDOW_MS = 7 * 24 * 3600_000L

    private fun prefs(context: Context) = context.getSharedPreferences("flix_resumes", Context.MODE_PRIVATE)

    /** Records that the user just opened [pkg] from the launcher. */
    fun note(context: Context, pkg: String?) {
        if (pkg.isNullOrBlank() || pkg == context.packageName) return
        prefs(context).edit().putLong(pkg, System.currentTimeMillis()).apply()
    }

    /** Last-open time per package: the launcher's own notes, plus Android's usage log when granted. */
    private fun lastUsed(context: Context): Map<String, Long> {
        val map = HashMap<String, Long>()
        prefs(context).all.forEach { (k, v) -> (v as? Long)?.let { map[k] = it } }
        runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - WINDOW_MS, now)
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
                    e.packageName?.let { if ((map[it] ?: 0L) < e.timeStamp) map[it] = e.timeStamp }
            }
        }
        return map
    }

    /** Apps whose package name or Play category says video. */
    private val HINTS = listOf("youtube", "netflix", "amazonvideo", "primevideo", "hotstar", "sonyliv",
        "stremio", "jellyfin", "wholphin", "mbox", "videolan", "mxtech", "kodi", "plex", "zee5", "voot",
        "jiotv", "smarttube", "pluto", "tubi", "crackle", "disney", "paramount", "peacock")

    private fun isVideo(pkg: String, category: Int) =
        category == ApplicationInfo.CATEGORY_VIDEO || HINTS.any(pkg::contains)

    /** Resume cards for video apps opened in the last week, most recent first, each with the app's own icon. */
    fun cards(context: Context, limit: Int = 4): List<Catalog.Title> {
        val last = lastUsed(context)
        val cutoff = System.currentTimeMillis() - WINDOW_MS
        val skip = setOf("com.nuvio.tv", "com.nuvio.tv.jellyfintv")
        // loadApps already excludes hidden apps, the launcher itself and unlaunchable packages,
        // and hands back the compacted icon and accent each tile uses.
        return AppRepository(context).loadApps()
            .filter { app -> (last[app.packageName] ?: 0L) > cutoff && app.packageName !in skip &&
                isVideo(app.packageName, app.category) }
            .sortedByDescending { last[it.packageName] ?: 0L }
            .take(limit)
            .map { app ->
                Catalog.Title(
                    id = "app:${app.packageName}",
                    type = "movie",
                    name = app.label,
                    poster = null,
                    backdrop = null,
                    logo = null,
                    year = null,
                    rating = null,
                    runtime = null,
                    genres = emptyList(),
                    description = "Pick up where you left off in ${app.label}",
                    icon = app.icon,
                    accent = app.accent,
                )
            }
    }
}
