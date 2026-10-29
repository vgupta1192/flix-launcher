package com.flix.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast

/**
 * Reports what the system installer decided about an update (see [Updater.install]).
 * On STATUS_PENDING_USER_ACTION the system attaches the "Install this update?" dialog as an
 * extra intent — launching it is our job; without it the update simply never proceeds.
 */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        android.util.Log.i("FlixUpdater", "installer status $status")
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm: Intent? = if (Build.VERSION.SDK_INT >= 33)
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                if (confirm == null) android.util.Log.i("FlixUpdater", "no confirmation intent attached to the broadcast")
                if (confirm != null) runCatching {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure {
                    android.util.Log.i("FlixUpdater", "couldn't open the confirmation: ${it.message}")
                    Toast.makeText(context, "Update needs confirmation: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
            PackageInstaller.STATUS_SUCCESS -> Toast.makeText(context, "Flix updated", Toast.LENGTH_LONG).show()
            else -> Toast.makeText(context,
                "Update didn't install: " + (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "cancelled"),
                Toast.LENGTH_LONG).show()
        }
    }
}
