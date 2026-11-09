package net.solardepin.solarchik.core

import android.content.Context
import java.util.Locale

/**
 * sol-main-v2 (SOL_PLAN item 7): the shared Zadarma numbers, one per country.
 *
 * Numbers come from the project handoff (docs/SHARED_NUMBER.md / ~/kb privacypolicy). They are
 * Sol's shared receiving lines used for call-forwarding — the user does NOT buy any number, the app
 * only auto-selects the right one for the user's country and lets it be overridden in Settings.
 */
data class ZadNumber(val iso: String, val flag: String, val name: String, val display: String, val e164: String)

object ZadNumberList {
    val NUMBERS = listOf(
        ZadNumber("CA", "CA", "Canada", "+1 581 689 9510", "+15816899510"),
        ZadNumber("US", "US", "USA (Houston)", "+1 832 500 9920", "+18325009920"),
        ZadNumber("GB", "GB", "United Kingdom", "+44 20 4577 1829", "+442045771829"),
        ZadNumber("DK", "DK", "Denmark", "+45 32 33 04 95", "+4532330495"),
        ZadNumber("UA", "UA", "Ukraine / CIS", "+380 91 481 0885", "+380914810885"),
    )

    private const val PREFS = "solarchik.zadarma"
    private const val KEY_OVERRIDE = "country_override"

    fun all(): List<ZadNumber> = NUMBERS

    fun byIso(iso: String): ZadNumber? = NUMBERS.firstOrNull { it.iso.equals(iso, true) }

    /** User's country ISO-3166 from the SIM/network, then device locale; null if neither. */
    fun detectCountry(ctx: Context): String? {
        try {
            val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val sim = tm?.simCountryIso
            if (!sim.isNullOrBlank() && sim.length == 2) return sim.uppercase(Locale.ROOT)
        } catch (_: Exception) {
        }
        val localeIso = Locale.getDefault().country
        if (localeIso.length == 2) return localeIso.uppercase(Locale.ROOT)
        return null
    }

    fun override(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_OVERRIDE, null)

    fun setOverride(ctx: Context, iso: String?) {
        val e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (iso == null) e.remove(KEY_OVERRIDE) else e.putString(KEY_OVERRIDE, iso.uppercase(Locale.ROOT))
        e.apply()
    }

    /**
     * The number to use for forwarding: explicit override first; then SIM/network country;
     * then device locale; fallback Ukraine line (Sol's home base).
     */
    fun pick(ctx: Context): ZadNumber {
        override(ctx)?.let { iso -> byIso(iso)?.let { return it } }
        detectCountry(ctx)?.let { iso -> byIso(iso)?.let { return it } }
        return byIso("UA") ?: NUMBERS.first()
    }

    /** Human reason for the pick, for the Settings line. */
    fun pickReason(ctx: Context): String {
        if (override(ctx) != null) return "manual"
        val sim = simIso(ctx)
        if (sim != null && sim.length == 2) return "SIM/network"
        if (Locale.getDefault().country.length == 2) return "locale"
        return "default"
    }

    private fun simIso(ctx: Context): String? {
        return try {
            (ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("zadarma.iso", null) ?: null)
                ?: (ctx.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager)?.simCountryIso
        } catch (e: Exception) {
            null
        }
    }
}
