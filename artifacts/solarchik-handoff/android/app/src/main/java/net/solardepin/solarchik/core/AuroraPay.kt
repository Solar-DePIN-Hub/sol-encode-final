package net.solardepin.solarchik.core

import android.content.Context

/**
 * sol-main-v2 (SOL_PLAN): Aurora pay-from-any-chain for the debt (bounty).
 *
 * DEMO (default): the verified shared pay-page link (Solana/Aurora worker page from the project
 * handoff) with a simulated "paid ✓" state — fully clickable, no money moves.
 * MAINNET: the same worker endpoint used for real; the app opens the link and records the
 * reference. Real transfers only after Vadym's double "ок".
 */
object AuroraPay {
    const val DEMO_PAGE = "https://sol-anychain.davidbell1603.workers.dev/r/wh6yhcdkke"
    private const val PREFS = "solarchik.aurora"

    fun demoMarkPaid(ctx: Context, ref: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("last_paid_demo_$ref", java.time.Instant.now().toString()).apply()
    }

    fun demoPaid(ctx: Context, ref: String): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("last_paid_demo_$ref")

    /** Payment link for [ref] (e.g. contact name or debt id). */
    fun link(ref: String, demo: Boolean): String = if (demo) DEMO_PAGE else "$DEMO_PAGE?ref=${android.net.Uri.encode(ref)}"

    // ---- sol-main-v2 (jury fix Solana#1/#3): the ref and every state survive app restarts ----

    fun newRef(ctx: Context): String =
        "s" + System.currentTimeMillis() + "-" + (0..9999).random()

    /** The active pay-ref, persisted so it survives process death. */
    fun activeRef(ctx: Context): String {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var r = p.getString("active_ref", null)
        if (r == null) {
            r = newRef(ctx)
            p.edit().putString("active_ref", r).apply()
        }
        return r
    }

    /** A link was freshly generated for [ref] — clear any stale state for it. */
    fun resetStates(ctx: Context, ref: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("active_ref", ref)
            .remove("waiting_$ref")
            .remove("paid_real_$ref")
            .remove("last_paid_demo_$ref")
            .apply()
    }

    fun setWaiting(ctx: Context, ref: String, b: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("waiting_$ref", b).apply()
    }

    fun waiting(ctx: Context, ref: String): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("waiting_$ref", false)

    fun markPaidReal(ctx: Context, ref: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("paid_real_$ref", java.time.Instant.now().toString())
            .putBoolean("waiting_$ref", false)
            .apply()
    }

    fun paidReal(ctx: Context, ref: String): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("paid_real_$ref")

    /**
     * Best-effort on-chain check for [ref] against the worker: true = chain says paid,
     * false = explicitly pending, null = the bridge hasn't reported yet (or network error).
     * Honest about the unknown state — never guesses «paid».
     */
    fun verify(ctx: Context, ref: String): Boolean? = runCatching {
        val url = java.net.URL("$DEMO_PAGE/verify?ref=${android.net.Uri.encode(ref)}")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        val body = if (conn.responseCode in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else throw IllegalStateException("http " + conn.responseCode)
        when {
            body.contains("\"status\":\"paid\"") || body.contains("\"paid\":true") -> true
            body.contains("\"status\":\"pending\"") || body.contains("\"paid\":false") -> false
            else -> null
        }
    }.getOrNull()
}
