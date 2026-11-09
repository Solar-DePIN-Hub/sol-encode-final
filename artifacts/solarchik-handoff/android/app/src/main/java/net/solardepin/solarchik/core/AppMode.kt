package net.solardepin.solarchik.core

import android.content.Context

/**
 * sol-main-v2 (SOL_PLAN item 3): DEMO / MAINNET switch for every money and call function.
 * DEMO (default): simulated calls, fake balances, no real network charge — fully clickable, ready for video.
 * MAINNET: real Solana mainnet, real Aurora, real Zadarma calls.
 * The MAINNET code path is real (Zadarma v3 /outgoingcall, Aurora worker); real spending only after
 * Vadym's explicit double "ок".
 */
object AppMode {
    const val DEMO = "demo"
    const val MAINNET = "mainnet"
    private const val PREF = "solarchik.appmode"

    fun get(ctx: Context): String =
        runCatching { ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("mode", DEMO) }.getOrNull() ?: DEMO

    fun set(ctx: Context, mode: String) {
        val m = if (mode == MAINNET) MAINNET else DEMO
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("mode", m).apply()
    }

    fun demo(ctx: Context): Boolean = get(ctx) == DEMO

    /** Short badge text for panels: "DEMO" or "MAINNET". */
    fun badge(ctx: Context): String = if (demo(ctx)) "DEMO" else "MAINNET"
}
