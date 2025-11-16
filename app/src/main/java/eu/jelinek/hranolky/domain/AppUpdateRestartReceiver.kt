package eu.jelinek.hranolky.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives ACTION_MY_PACKAGE_REPLACED after the app has been updated.
 * This runs in the new process (updated version) and can safely relaunch the app
 * to provide a seamless update experience on the Zebra terminal.
 */
class AppUpdateRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED == intent.action) {
            Log.i("AppUpdate", "ACTION_MY_PACKAGE_REPLACED received - relaunching app")
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(launchIntent)
                    Log.i("AppUpdate", "App relaunched successfully after update")
                } else {
                    Log.w("AppUpdate", "Launch intent is null; cannot relaunch app after update")
                }
            } catch (e: Exception) {
                Log.e("AppUpdate", "Failed to relaunch app after update: ${e.message}", e)
            }
        }
    }
}

