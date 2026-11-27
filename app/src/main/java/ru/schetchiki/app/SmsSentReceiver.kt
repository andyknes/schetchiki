package ru.schetchiki.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val status = if (resultCode == android.app.Activity.RESULT_OK) "sent" else "failed"
        context.sendBroadcast(Intent(MainActivity.ACTION_SMS_RESULT).setPackage(context.packageName).putExtra("status", status))
    }
}
