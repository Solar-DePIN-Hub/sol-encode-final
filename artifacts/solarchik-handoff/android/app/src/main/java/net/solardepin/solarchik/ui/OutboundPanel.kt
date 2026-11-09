package net.solardepin.solarchik.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.solardepin.solarchik.MainActivity
import net.solardepin.solarchik.R
import net.solardepin.solarchik.core.AppMode
import net.solardepin.solarchik.core.OutboundCall

/**
 * sol-main-v2 (SOL_PLAN: "Sol calls by himself"): Sol сам дзвонить боржнику.
 * DEMO — SimVoice читає текст на місці (нульовий чек, повністю клікабельне для відео);
 * MAINNET — реальний TTS-виклик через Zadarma v3.
 */
class OutboundPanel(private val host: MainActivity) {
    private val ctx: Context = host
    val view: LinearLayout = Ui.column(ctx, gap = 0).apply { tag = "sol-outbound" }
    private lateinit var toField: EditText
    private lateinit var msgField: EditText
    private lateinit var keyField: EditText
    private lateinit var secretField: EditText
    private lateinit var status: TextView

    init { build() }

    private fun build() {
        view.addView(Ui.top(Ui.label(ctx, ctx.getString(R.string.sol_out_title), Ui.GOLD), 0))
        view.addView(Ui.top(Ui.muted(ctx, ctx.getString(R.string.sol_out_body)).apply { setLineSpacing(0f, 1.3f) }, 6))

        toField = field(ctx, ctx.getString(R.string.sol_out_to_hint), InputType.TYPE_CLASS_PHONE)
        toField.tag = "sol-out-to"
        view.addView(Ui.top(toField, 8))

        msgField = field(ctx, ctx.getString(R.string.sol_out_msg_hint),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        msgField.minLines = 2
        msgField.maxLines = 4
        msgField.tag = "sol-out-msg"
        view.addView(Ui.top(msgField, 8))

        // sol-main-v2 (jury fix #2 / Encode #3): ONE mode-aware action button. DEMO taps
        // straight through (zero spend); MAINNET needs the double-«ok» gate before doCall.
        val callBtn = Ui.button(ctx, btnLabel(), Ui.Btn.PRIMARY) { onCall() }
            .apply { tag = "sol-out-call"; maxLines = 2; textSize = 13f }
        view.addView(Ui.top(callBtn, 8))

        // sol-main-v2 (jury fix UX#2): keys are MAINNET-only, collapsed by default — clean wall for video
        val keysWrap = Ui.column(ctx, gap = 6)
        val keys = Ui.column(ctx, gap = 6).apply { visibility = android.view.View.GONE }
        val keysToggle = Ui.button(ctx, ctx.getString(R.string.sol_out_keys_toggle), Ui.Btn.GHOST) {
            keys.visibility = if (keys.visibility == android.view.View.VISIBLE) android.view.View.GONE else android.view.View.VISIBLE
            host.toast(ctx.getString(R.string.sol_out_keys_hint))
        }.apply { tag = "sol-out-keystoggle"; maxLines = 1; textSize = 13f }
        keysWrap.addView(keysToggle)

        keyField = field(ctx, ctx.getString(R.string.sol_out_key_hint), InputType.TYPE_CLASS_TEXT, prefill = false)
        keyField.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        keyField.tag = "sol-out-key"
        secretField = field(ctx, ctx.getString(R.string.sol_out_secret_hint), InputType.TYPE_CLASS_TEXT, prefill = false)
        secretField.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        secretField.tag = "sol-out-secret"
        keys.addView(keyField)
        keys.addView(Ui.top(secretField, 6))
        keys.addView(Ui.top(Ui.button(ctx, ctx.getString(R.string.sol_out_savekey), Ui.Btn.SECONDARY) {
            OutboundCall.saveKeys(ctx, keyField.text.toString(), secretField.text.toString())
            keyField.text = null
            secretField.text = null
            host.toast(ctx.getString(R.string.sol_out_key_saved))
        }.apply { tag = "sol-out-savekey"; maxLines = 1; textSize = 13f }, 6))
        keysWrap.addView(keys)
        view.addView(Ui.top(keysWrap, 10))

        status = Ui.text(ctx, "", 12.5f, Ui.MUTED, 700)
        status.tag = "sol-out-status"
        status.setLineSpacing(0f, 1.25f)
        view.addView(Ui.top(status, 8))
    }

    private fun field(ctx: Context, hint: String, type: Int, prefill: Boolean = true): EditText = EditText(ctx).apply {
        if (prefill) setText(hint) else setHint(hint)
        setHintTextColor(Ui.MUTED)
        setTextColor(Ui.TEXT)
        textSize = 14f
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = ctx.resources.displayMetrics.density * 14
            setColor(Ui.SURFACE2)
            setStroke((ctx.resources.displayMetrics.density).toInt(), Ui.STROKE)
        }
        setPadding(
            (ctx.resources.displayMetrics.density * 12).toInt(),
            (ctx.resources.displayMetrics.density * 10).toInt(),
            (ctx.resources.displayMetrics.density * 12).toInt(),
            (ctx.resources.displayMetrics.density * 10).toInt()
        )
        inputType = type
    }

    private fun btnLabel(): String =
        if (AppMode.demo(ctx)) ctx.getString(R.string.sol_out_demo) else ctx.getString(R.string.sol_out_real)

    /** The ModePanel up the same Settings screen may switch modes — keep this label honest. */
    fun refreshMode() {
        (view.findViewWithTag("sol-out-call") as? android.widget.TextView)?.text = btnLabel()
    }

    private fun onCall() {
        if (AppMode.demo(ctx)) { doCall(); return }
        // MAINNET: the double-«ok» gate. 1) the real-charge warning, 2) final confirmation.
        android.app.AlertDialog.Builder(ctx)
            .setTitle(R.string.sol_out_gate1_title)
            .setMessage(R.string.sol_out_gate1_msg)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.sol_out_gate1_ok) { _, _ ->
                android.app.AlertDialog.Builder(ctx)
                    .setTitle(R.string.sol_out_gate2_title)
                    .setMessage(R.string.sol_out_gate2_msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.sol_out_gate1_ok) { _, _ -> doCall() }
                    .show()
            }
            .show()
    }

    private fun doCall() {
        val to = OutboundCall.normalize(toField.text.toString())
        val msg = msgField.text.toString().trim()
        when {
            to.isEmpty() -> host.toast(ctx.getString(R.string.sol_out_bad_to))
            msg.isEmpty() -> host.toast(ctx.getString(R.string.sol_out_bad_msg))
            else -> run(to, msg)
        }
    }

    private fun run(to: String, msg: String) {
        status.setTextColor(Ui.MUTED)
        status.text = ctx.getString(R.string.sol_out_working) + " " + AppMode.badge(ctx) + "…"
        if (AppMode.demo(ctx)) {
            OutboundCall.simulate(ctx, msg, to) { r ->
                host.runOnUiThread { apply(r) }
            }
        } else {
            host.scope.launch {
                val r = withContext(Dispatchers.IO) { OutboundCall.real(ctx, msg, to) }
                apply(r)
            }
        }
    }

    private fun apply(r: OutboundCall.Result) {
        when (r) {
            is OutboundCall.Ok -> { status.text = r.message; status.setTextColor(Ui.GREEN) }
            is OutboundCall.Fail -> { status.text = r.message; status.setTextColor(Ui.AMBER) }
        }
    }
}
