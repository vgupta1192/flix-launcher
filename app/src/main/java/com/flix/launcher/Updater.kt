package com.flix.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Updates Flix from GitHub releases (github.com/vgupta1192/flix-launcher). The build workflow
 * publishes every push to main as a release whose APK asset is named flix-launcher-<code>.apk.
 *
 * A check runs when the home screen opens (at most every CHECK_MS) while "Update automatically"
 * is on, and from Settings → About → Check for updates any time. A newer release is downloaded
 * and handed to the system installer: Android always shows its own one-press confirmation.
 *
 * Everything here runs on background threads; toasts arrive on the main one.
 */
object Updater {
    const val REPO = "vgupta1192/flix-launcher"
    private const val CHECK_MS = 12 * 60 * 60 * 1000L

    private fun prefs(context: Context) = context.getSharedPreferences("updater", Context.MODE_PRIVATE)

    data class Release(val version: String, val url: String, val notes: String)

    private fun installedVersion(context: Context) =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "0"

    /** "1.2.10" vs "1.2.9" — compares each dot-separated number. */
    private fun isNewer(current: String, candidate: String): Boolean {
        fun nums(s: String) = s.removePrefix("v").split('.').map { it.filter(Char::isDigit).ifEmpty { "0" }.toInt() }
        val a = nums(current); val b = nums(candidate)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return y > x
        }
        return false
    }

    fun lastResult(context: Context) = prefs(context).getString("result", null)

    /** The newest published release, or null when GitHub is unreachable or has no releases. */
    fun latest(context: Context): Release? = runCatching {
        val conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 15000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        val json = conn.inputStream.use { String(SafeIo.readBytes(it, SafeIo.MAX_JSON)) }
        conn.disconnect()
        val o = JSONObject(json)
        val asset = o.optJSONArray("assets")?.let { a ->
            (0 until a.length()).map { a.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk") }
        } ?: return@runCatching null
        Release(o.optString("tag_name").removePrefix("v"), asset.optString("browser_download_url"),
            o.optString("body").split('\n').firstOrNull()?.take(120) ?: "")
    }.getOrNull()

    /** Called when home opens. Checks at most every 12 h while automatic updates are on. */
    fun maybeAuto(context: Context) {
        if (!FlixSettings.isOn(context, FlixSettings.AUTO_UPDATE)) return
        val p = prefs(context)
        if (System.currentTimeMillis() - p.getLong("checked_at", 0) < CHECK_MS) return
        p.edit().putLong("checked_at", System.currentTimeMillis()).apply()
        Thread {
            val r = runCatching { latest(context) }.getOrNull() ?: return@Thread
            if (!isNewer(installedVersion(context), r.version)) return@Thread
            prepare(context, r)
        }.start()
    }

    /** Manual "Check for updates": always checks, reports with toasts, prompts on a newer release. */
    fun checkNow(context: Context) {
        Toast.makeText(context, "Checking for updates…", Toast.LENGTH_SHORT).show()
        Thread {
            val r = runCatching { latest(context) }
            val release = r.getOrNull()
            val message = when {
                release == null -> "Couldn't reach GitHub — try again later"
                !isNewer(installedVersion(context), release.version) -> "You're up to date (v${installedVersion(context)})"
                else -> "Updating to v${release.version}…"
            }
            runOnUiThread(context) { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
            if (release != null && isNewer(installedVersion(context), release.version)) prepare(context, release)
        }.start()
    }

    /** Downloads the release and hands it to the system installer (which asks one Yes). */
    private fun prepare(context: Context, r: Release) {
        val apk = File(context.cacheDir, "update.apk")
        val ok = runCatching {
            val conn = URL(r.url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            conn.inputStream.use { input -> apk.outputStream().use { out ->
                input.copyTo(out, 256 * 1024); out.flush(); out.fd.sync()
            } }
            conn.disconnect()
            apk.length() > 0
        }.getOrDefault(false)
        prefs(context).edit()
            .putString("result", if (ok) "v${r.version} ready to install" else "Download failed (v${r.version})")
            .apply()
        if (!ok) return
        val pm = context.packageManager
        if (!pm.canRequestPackageInstalls()) {
            // One-time: let the user allow installs from Flix, then the prompt works on its own.
            prefs(context).edit().putLong("checked_at", 0).apply() // try again next home open
            runOnUiThread(context) {
                Toast.makeText(context, "Allow \"Install unknown apps\" for Flix to finish updates", Toast.LENGTH_LONG).show()
                runCatching {
                    context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            return
        }
        install(context, apk)
    }

    private fun install(context: Context, apk: File) {
        runCatching {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val id = installer.createSession(params)
            val session = installer.openSession(id)
            apk.inputStream().use { input -> session.openWrite("flix.apk", 0, apk.length()).use { out ->
                input.copyTo(out)
                session.fsync(out)
            } }
            val intent = Intent("com.flix.launcher.INSTALL_RESULT").setPackage(context.packageName)
            val flag = if (Build.VERSION.SDK_INT >= 31)
                android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            else android.app.PendingIntent.FLAG_UPDATE_CURRENT
            val sender = android.app.PendingIntent.getBroadcast(context, id, intent, flag).intentSender
            session.commit(sender)
            session.close()
        }.onFailure {
            prefs(context).edit().putString("result", "Install couldn't start: ${it.message}").apply()
            runOnUiThread(context) { Toast.makeText(context, "Update failed: ${it.message}", Toast.LENGTH_LONG).show() }
        }
    }

    /** Toasts from any thread; the home screen runs on the main one. */
    private fun runOnUiThread(context: Context, block: () -> Unit) {
        android.os.Handler(context.mainLooper).post(block)
    }
}
