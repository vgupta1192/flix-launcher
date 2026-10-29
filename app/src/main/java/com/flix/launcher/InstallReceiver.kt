package com.flix.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

/** Reports what the system installer decided about an update (see [Updater.install]). */
class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val code = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = when (code) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> "Confirm the update on screen"
            PackageInstaller.STATUS_SUCCESS -> "Flix updated"
            else -> "Update didn't install: " +
                (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "cancelled")
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
