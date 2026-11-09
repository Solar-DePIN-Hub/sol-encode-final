package net.solardepin.solarchik.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.LinearLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.solardepin.solarchik.MainActivity
import net.solardepin.solarchik.R
import net.solardepin.solarchik.core.AppMode
import net.solardepin.solarchik.core.AuroraPay

/**
 * sol-main-v2 (SOL_PLAN: "Aurora pay-from-any-chain (bounty)"):
 * DEMO — локальний демо-посилання та «я оплатив» без жодного чека;
 * MAINNET — реальний посилання через мост Aurora (платіж з будь-якого блокчейну → Solana).
 */
class AuroraPanel(private val host: MainActivity) {
    private val ctx: Context = host
    val view: LinearLayout = Ui.column(ctx, gap = 8).apply { tag = "sol-aurora" }
    // sol-main-v2 (jury fix Solana#1): the ref is persisted — it survives app restarts
    private var ref: String = AuroraPay.activeRef(ctx)

    init {
        view.addView(Ui.muted(ctx, ctx.getString(R.string.sol_aur_body)).apply { setLineSpacing(0f, 1.3f) })
        val amt = Ui.text(ctx, "5 USDC", 14f, Ui.TEXT, 800).apply {
            tag = "sol-aur-amount"; setOnClickListener { copy(text.toString()) }
        }
        val row = Ui.row(ctx, gap = 8)
        row.addView(Ui.weight(Ui.label(ctx, ctx.getString(R.string.sol_aur_amount))))
        row.addView(amt)
        view.addView(Ui.top(row, 8))

        val linkBox = run {
            Ui.column(ctx, gap = 6).apply {
                addView(Ui.muted(ctx, ctx.getString(R.string.sol_aur_link_hint), 12f))
                val link = Ui.text(ctx, "—", 12f, Ui.CYAN, 600).apply {
                    tag = "sol-aur-link"; setTextIsSelectable(true); setLineSpacing(0f, 1.3f)
                    setOnClickListener { copy(text.toString()) }
                }
                addView(Ui.top(link, 4))
            }
        }
        view.addView(linkBox)

        val actions = Ui.row(ctx, gap = 8)
        actions.addView(Ui.weight(Ui.button(ctx, ctx.getString(R.string.sol_aur_make), Ui.Btn.PRIMARY, R.drawable.ic_open) {
            makeLink()
        }.apply { tag = "sol-aur-make"; maxLines = 2; textSize = 13f }))
        actions.addView(Ui.weight(Ui.button(ctx, ctx.getString(R.string.sol_aur_i_paid), Ui.Btn.GHOST) {
            paid()
        }.apply { tag = "sol-aur-paid"; maxLines = 2; textSize = 13f }))
        view.addView(Ui.top(actions, 10))

        val statusBox = Ui.column(ctx, gap = 4)
        statusBox.tag = "sol-aur-status"
        view.addView(Ui.top(statusBox, 4))
        renderStatus()
    }

    private fun linkView(): android.widget.TextView? =
        view.findViewWithTag("sol-aur-link") as? android.widget.TextView

    private fun makeLink() {
        // sol-main-v2 (jury fix Solana#1): a fresh pay-ref per link, stale states cleared
        ref = AuroraPay.newRef(ctx)
        AuroraPay.resetStates(ctx, ref)
        val demo = AppMode.demo(ctx)
        val uri = AuroraPay.link(ref, demo)
        linkView()?.text = uri
        host.toast(ctx.getString(if (demo) R.string.sol_aur_demo_on else R.string.sol_aur_main_on))
        renderStatus()
    }

    private fun paid() {
        if (AppMode.demo(ctx)) {
            AuroraPay.demoMarkPaid(ctx, ref)
            host.toast(ctx.getString(R.string.sol_aur_demo_paid))
        } else {
            // sol-main-v2 (jury fix Encode#2): MAINNET «I paid» = waiting state (persisted), never a false «paid»
            AuroraPay.setWaiting(ctx, ref, true)
            host.toast(ctx.getString(R.string.sol_aur_real_wait))
        }
        renderStatus()
    }

    // sol-main-v2 (jury fix Solana#3): honest on-chain status check for a MAINNET waiting payment
    private fun checkStatus() {
        host.scope.launch {
            val v = withContext(Dispatchers.IO) { AuroraPay.verify(ctx, ref) }
            host.runOnUiThread {
                when (v) {
                    true -> {
                        AuroraPay.markPaidReal(ctx, ref)
                        host.toast(ctx.getString(R.string.sol_aur_check_ok))
                    }
                    false -> host.toast(ctx.getString(R.string.sol_aur_check_unknown))
                    null -> host.toast(ctx.getString(R.string.sol_aur_check_unknown))
                }
                renderStatus()
            }
        }
    }

    private fun renderStatus() {
        val status = view.findViewWithTag("sol-aur-status") as? android.widget.LinearLayout ?: return
        status.removeAllViews()
        if (AuroraPay.demoPaid(ctx, ref)) {
            status.addView(Ui.pill(ctx, ctx.getString(R.string.sol_aur_paid), Ui.GREEN))
        } else if (AuroraPay.paidReal(ctx, ref)) {
            status.addView(Ui.pill(ctx, ctx.getString(R.string.sol_aur_paid), Ui.GREEN))
            status.addView(Ui.muted(ctx, ctx.getString(R.string.sol_aur_check_ok), 12f))
        } else if (AuroraPay.waiting(ctx, ref)) {
            status.addView(Ui.pill(ctx, ctx.getString(R.string.sol_aur_new), Ui.AMBER))
            status.addView(Ui.muted(ctx, ctx.getString(R.string.sol_aur_waiting), 12f))
            val check = Ui.button(ctx, ctx.getString(R.string.sol_aur_check), Ui.Btn.GHOST) { checkStatus() }
                .apply { tag = "sol-aur-check"; maxLines = 1; textSize = 13f }
            status.addView(check)
        } else {
            status.addView(Ui.pill(ctx, ctx.getString(R.string.sol_aur_new), Ui.AMBER))
        }
    }

    private fun copy(t: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Solarchik", t))
        host.toast(ctx.getString(R.string.sol_copied))
    }
}
