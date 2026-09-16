package com.libreriapro.ultra.data

import android.content.Context
import com.libreriapro.ultra.domain.StoreSettings

/**
 * Key/value store for the shop preferences. Only user preferences live here,
 * never credentials or secrets.
 */
class SettingsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): StoreSettings = StoreSettings(
        businessName = prefs.getString(KEY_BUSINESS_NAME, null) ?: DEFAULTS.businessName,
        cashierName = prefs.getString(KEY_CASHIER, null) ?: DEFAULTS.cashierName,
        currencySymbol = prefs.getString(KEY_CURRENCY, null) ?: DEFAULTS.currencySymbol,
        defaultMinStock = prefs.getInt(KEY_MIN_STOCK, DEFAULTS.defaultMinStock),
    )

    fun save(settings: StoreSettings) {
        prefs.edit()
            .putString(KEY_BUSINESS_NAME, settings.businessName)
            .putString(KEY_CASHIER, settings.cashierName)
            .putString(KEY_CURRENCY, settings.currencySymbol)
            .putInt(KEY_MIN_STOCK, settings.defaultMinStock)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "libreria_settings"
        const val KEY_BUSINESS_NAME = "business_name"
        const val KEY_CASHIER = "cashier_name"
        const val KEY_CURRENCY = "currency_symbol"
        const val KEY_MIN_STOCK = "default_min_stock"
        val DEFAULTS = StoreSettings()
    }
}