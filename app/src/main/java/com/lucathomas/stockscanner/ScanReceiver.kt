package com.lucathomas.stockscanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast


class ScanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val barcode = intent.getStringExtra("com.symbol.datawedge.data_string")
        val symbology = intent.getStringExtra("com.symbol.datawedge.label_type")
        Toast.makeText(context, "Barcode: $barcode\nTyp: $symbology", Toast.LENGTH_LONG).show()
    }
}
