package net.solardepin.solarchik.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import net.solardepin.solarchik.MainActivity
import net.solardepin.solarchik.R
import net.solardepin.solarchik.core.AppMode
import net.solardepin.solarchik.core.ZadNumber
import net.solardepin.solarchik.core.ZadNumberList
import net.solardepin.solarchik.screen.Secretary
import net.solardepin.solarchik.ui.Ui.dp

/**
 * sol-main-v2 (SOL_PLAN item 3 + 7): the app-wide DEMO/MAINNET switch and the 5 shared Zadarma
 * lines. Auto-selects a line by the user country (SIM/network first, then locale) with a
 * country picker to override; "Use for forwarding" feeds the selected line into the carrier
 * forwarding setup (ForwardPanel).
 */
class ModePanel(private val host: MainActivity, private val onModeChanged: (() -> Unit)? = null) {
    private val ctx: Context = host
    val view: LinearLayout = Ui.column(ctx, gap = 8).apply { tag = "sol-modes" }
    private lateinit var modeBox: LinearLayout
    private lateinit var linesBox: LinearLayout

    init {
        view.addView(Ui.label(ctx, ctx.getString(R.string.sol_mode_title), Ui.GOLD))
        view.addView(Ui.top(Ui.muted(ctx, ctx.getString(R.string.sol_mode_body)).apply { setLineSpacing(0f, 1.3f) }, 6))
        modeBox = Ui.column(ctx, gap = 8)
        view.addView(Ui.top(modeBox, 8))

        view.addView(Ui.top(Ui.label(ctx, ctx.getString(R.string.sol_lines_title)).apply { tag = "sol-lines-label" }, 14))
        linesBox = Ui.column(ctx, gap = 6)
        view.addView(Ui.top(linesBox, 6))
        render()
    }

    fun render() {
        // ---- DEMO / MAINNET ----
        modeBox.removeAllViews()
        val cur = AppMode.get(ctx)
        val row = Ui.row(ctx, gap = 8)
        row.addView(Ui.weight(Ui.button(ctx, "DEMO", if (cur == AppMode.DEMO) Ui.Btn.PRIMARY else Ui.Btn.GHOST) {
            AppMode.set(ctx, AppMode.DEMO)
            host.toast(ctx.getString(R.string.sol_mode_demo_on))
            onModeChanged?.invoke()
            render()
        }.apply { tag = "sol-mode-demo"; maxLines = 2 }))
        row.addView(Ui.weight(Ui.button(ctx, "MAINNET", if (cur == AppMode.MAINNET) Ui.Btn.PRIMARY else Ui.Btn.GHOST) {
            android.app.AlertDialog.Builder(ctx)
                .setTitle(R.string.sol_mode_mainnet_title)
                .setMessage(R.string.sol_mode_mainnet_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.sol_mode_go) { _, _ ->
                    AppMode.set(ctx, AppMode.MAINNET)
                    host.toast(ctx.getString(R.string.sol_mode_mainnet_on))
                    onModeChanged?.invoke()
                    render()
                }
                .show()
        }.apply { tag = "sol-mode-mainnet"; maxLines = 2 }))
        modeBox.addView(row)
        modeBox.addView(Ui.top(Ui.muted(ctx, if (cur == AppMode.DEMO) ctx.getString(R.string.sol_mode_demo_hint) else ctx.getString(R.string.sol_mode_mainnet_hint), 12f).apply { tag = "sol-mode-hint"; setLineSpacing(0f, 1.25f) }, 4))

        // ---- the 5 shared lines ----
        linesBox.removeAllViews()
        val picked = ZadNumberList.pick(ctx)
        val reason = ZadNumberList.pickReason(ctx)
        linesBox.addView(Ui.top(Ui.muted(ctx, ctx.getString(R.string.sol_lines_auto, reason, display(picked)), 12f).apply { tag = "sol-lines-pick" }, 4))
        ZadNumberList.all().forEach { line ->
            val isSel = line.iso == picked.iso
            val card = Ui.card(ctx, accent = if (isSel) Ui.GOLD else null, pad = 10).apply {
                tag = "sol-line-" + line.iso.toLowerCase()
                val r1 = Ui.row(ctx, gap = 8).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
                r1.addView(Ui.weight(Ui.body(ctx, line.name)))
                if (isSel) r1.addView(Ui.pill(ctx, ctx.getString(R.string.sol_lines_sel), Ui.GREEN))
                addView(r1)
                addView(Ui.top(Ui.text(ctx, line.display, 13f, Ui.CYAN, 700).apply { setTextIsSelectable(true) }, 2))
                val actions = Ui.row(ctx, gap = 8)
                actions.addView(Ui.weight(Ui.button(ctx, ctx.getString(R.string.sol_line_use), if (isSel) Ui.Btn.SECONDARY else Ui.Btn.GHOST) {
                    ZadNumberList.setOverride(ctx, line.iso)
                    host.toast(ctx.getString(R.string.sol_line_used, line.display))
                    render()
                }.apply { tag = "sol-line-use-" + line.iso.toLowerCase(); maxLines = 2; textSize = 13f }))
                actions.addView(Ui.weight(Ui.button(ctx, ctx.getString(R.string.sol_line_fw), Ui.Btn.GHOST) {
                    // set this exact shared line as the carrier forwarding target
                    val ok = net.solardepin.solarchik.screen.Secretary.setForwardNumber(ctx, line.e164)
                    host.toast(if (ok == null) ctx.getString(R.string.sol_line_bad) else ctx.getString(R.string.sol_line_fw_ok, line.display))
                    render()
                }.apply { tag = "sol-line-fw-" + line.iso.toLowerCase(); maxLines = 2; textSize = 13f }))
                addView(Ui.top(actions, 8))
            }
            linesBox.addView(Ui.top(card, 4))
        }
        // reset to auto
        linesBox.addView(Ui.top(Ui.button(ctx, ctx.getString(R.string.sol_lines_reset), Ui.Btn.GHOST) {
            ZadNumberList.setOverride(ctx, null)
            render()
        }.apply { tag = "sol-lines-reset"; textSize = 13f }, 2))
    }

    private fun display(l: ZadNumber): String = l.display
}
