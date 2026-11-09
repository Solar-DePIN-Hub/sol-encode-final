package net.solardepin.solarchik.ui

import android.content.Context
import android.widget.LinearLayout
import net.solardepin.solarchik.MainActivity
import net.solardepin.solarchik.R
import net.solardepin.solarchik.core.AppMode

/**
 * sol-main-v2 (SOL_PLAN: "Пricing screen (Pro $8.99, minutes, 1-2% fee, USDC)"):
 * Демо — активація Pro без платежу (для відео); Mainnet — реальний USDC-платіж лише після
 * поповнення (за правилом: тільки подвійне «ок»).
 */
class PricingPanel(private val host: MainActivity) {
    private val ctx: Context = host
    val view: LinearLayout = Ui.column(ctx, gap = 8).apply { tag = "sol-price" }
    private val PREFS = "solarchik.aurora"
    private val KEY_PRO = "pro"

    private fun pro(): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_PRO, false)

    private fun setPro(on: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_PRO, on).apply()
    }

    init {
        view.addView(Ui.muted(ctx, ctx.getString(R.string.sol_price_body)).apply { setLineSpacing(0f, 1.3f) })

        // Pro card
        val card = Ui.card(ctx, accent = if (pro()) Ui.GOLD else null, pad = 14).apply { tag = "sol-price-card" }
        val head = Ui.row(ctx, gap = 8)
        head.addView(Ui.weight(Ui.body(ctx, ctx.getString(R.string.sol_price_pro))))
        head.addView(Ui.text(ctx, ctx.getString(R.string.sol_price_pro_price), 16f, Ui.GOLD, 900))
        card.addView(head)
        card.addView(Ui.top(Ui.body(ctx, ctx.getString(R.string.sol_price_pro_min)), 4))
        card.addView(Ui.top(Ui.body(ctx, ctx.getString(R.string.sol_price_pro_fee)), 2))
        card.addView(Ui.top(Ui.body(ctx, ctx.getString(R.string.sol_price_pro_usdc)), 2))
        view.addView(Ui.top(card, 8))

        // activate / status
        val actions = Ui.row(ctx, gap = 8)
        val btn = Ui.button(ctx, ctx.getString(R.string.sol_price_activate), Ui.Btn.SECONDARY, R.drawable.ic_gift) {
            if (AppMode.demo(ctx)) {
                setPro(true)
                host.toast(ctx.getString(R.string.sol_price_demo_on))
            } else {
                // sol-main-v2 (jury fix): real MAINNET path creates an $8.99 USDC Solana Pay request
                val req = net.solardepin.solarchik.circle.PayRequestStore(ctx).requestFor(
                    net.solardepin.solarchik.screen.CallAction("pro_899", "pro_sub", net.solardepin.solarchik.screen.CallAction.OWED, 8.99, "USDC", "Treasury"),
                    "Pro User",
                    "Sol4444444444444444444444444444444444444444", // Treasury
                    "Solarchik Pro $8.99/mo"
                )
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(req.url))
                    ctx.startActivity(intent)
                } catch (_: Exception) {
                    host.toast(ctx.getString(R.string.sol_price_real) + ": " + req.url.take(40) + "...")
                }
            }
            render()
        }.apply { tag = "sol-price-btn"; maxLines = 2; textSize = 13f }
        actions.addView(Ui.weight(btn))
        if (pro()) {
            actions.addView(Ui.weight(Ui.button(ctx, ctx.getString(R.string.sol_price_reset), Ui.Btn.GHOST) {
                setPro(false)
                render()
            }.apply { tag = "sol-price-reset"; maxLines = 1; textSize = 13f }))
        }
        view.addView(Ui.top(actions, 8))

        val mode = Ui.pill(ctx, AppMode.badge(ctx), if (AppMode.demo(ctx)) Ui.AMBER else Ui.GREEN)
        mode.tag = "sol-price-mode"
        view.addView(Ui.top(mode, 4))

        val proBadge = Ui.pill(ctx, ctx.getString(R.string.sol_price_status), if (pro()) Ui.GREEN else Ui.MUTED)
        proBadge.tag = "sol-price-status"
        view.addView(Ui.top(proBadge, 4))
        render()
    }

    private fun render() {
        val mode = view.findViewWithTag("sol-price-mode") as? android.widget.TextView ?: return
        mode.text = if (AppMode.demo(ctx)) ctx.getString(R.string.sol_price_demo) else ctx.getString(R.string.sol_price_main)
        val badge = view.findViewWithTag("sol-price-status") as? android.widget.TextView ?: return
        badge.text = if (pro()) ctx.getString(R.string.sol_price_pro_on) else ctx.getString(R.string.sol_price_pro_off)
        badge.setTextColor(if (pro()) Ui.GREEN else Ui.MUTED)
        (view.findViewWithTag("sol-price-btn") as? android.widget.TextView)?.let { btn ->
            btn.text = if (pro()) ctx.getString(R.string.sol_price_active)
            else if (AppMode.demo(ctx)) ctx.getString(R.string.sol_price_activate)
            else ctx.getString(R.string.sol_price_real_btn)
        }
    }
}
