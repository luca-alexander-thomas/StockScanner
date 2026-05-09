package com.lucathomas.stockscanner

import android.content.Context
import android.content.Intent
import android.os.Bundle

object DataWedgeHelper {

    private const val PROFILE_NAME = "StockScanner"
    private const val ACTION_DATAWEDGE = "com.symbol.datawedge.api.ACTION"
    private const val EXTRA_SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"

    fun createProfile(context: Context) {
        // Master Bundle
        val mainBundle = Bundle().apply {
            putString("PROFILE_NAME", PROFILE_NAME)
            putString("PROFILE_ENABLED", "true")
            putString("CONFIG_MODE", "CREATE_IF_NOT_EXIST")
        }

        // Associate with App
        val appConfig = Bundle().apply {
            putString("PACKAGE_NAME", context.packageName)
            putStringArray("ACTIVITY_LIST", arrayOf("*"))
        }
        mainBundle.putParcelableArray("APP_LIST", arrayOf(appConfig))

        // Barcode Input
        val barcodeBundle = Bundle().apply {
            putString("PLUGIN_NAME", "BARCODE")
            putBundle("PARAM_LIST", Bundle().apply {
                putString("scanner_selection", "auto")
                putString("decoder_ean8", "true")
                putString("decoder_ean13", "true")
                putString("decoder_code128", "true")
                putString("decoder_qrcode", "true")
            })
        }

        // Intent Output
        val intentBundle = Bundle().apply {
            putString("PLUGIN_NAME", "INTENT")
            putBundle("PARAM_LIST", Bundle().apply {
                putString("intent_output_enabled", "true")
                putString("intent_action", "com.lucathomas.stockscanner.SCAN_RESULT")
                // 0 = Start Activity, 1 = Start Service, 2 = Broadcast
                putString("intent_delivery", "2") 
            })
        }

        mainBundle.putParcelableArrayList("PLUGIN_CONFIG", arrayListOf(barcodeBundle, intentBundle))

        context.sendBroadcast(Intent(ACTION_DATAWEDGE).apply {
            putExtra(EXTRA_SET_CONFIG, mainBundle)
            // Request feedback for debugging
            putExtra("SEND_RESULT", "true")
        })
    }
}