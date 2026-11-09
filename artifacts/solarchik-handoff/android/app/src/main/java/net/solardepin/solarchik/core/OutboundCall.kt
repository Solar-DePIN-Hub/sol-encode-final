package net.solardepin.solarchik.core

import android.content.Context
import android.speech.tts.TextToSpeech
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * sol-main-v2 (SOL_PLAN): "Sol calls by itself" — outbound reminder call.
 *
 * DEMO (default): the text is spoken with the local TTS voice and logged; no network, no bill,
 * fully clickable for the video. MAINNET: a real Zadarma TTS outbound call (v3 /outgoingcall).
 *
 * Keys live only in this phone's private SharedPreferences and are masked in the UI. Real calls
 * spend money, so they run only in MAINNET mode, which itself flips only on Vadym's double "ок".
 */
object OutboundCall {
    sealed interface Result
    data class Ok(val demo: Boolean, val message: String) : Result
    data class Fail(val message: String) : Result

    private const val PREFS = "solarchik.outbound"
    private const val ENDPOINT = "https://api.zadarma.com/v3/outgoingcall"

    fun normalize(raw: String): String {
        var t = raw.trim().filterNot { it == ' ' || it == '-' || it == '.' || it == '(' || it == ')' }
        if (t.startsWith("00")) t = "+" + t.drop(2)
        if (!t.startsWith("+")) return ""
        val digits = t.drop(1)
        if (digits.length !in 8..15 || !digits.all { it in '0'..'9' }) return ""
        return "+" + digits
    }

    fun hasKeys(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (p.getString("zadarma.api_key", "")?.isNotBlank() == true ||
            p.getString("zadarma.api_secret", "")?.isNotBlank() == true)
    }

    fun saveKeys(ctx: Context, apiKey: String, apiSecret: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("zadarma.api_key", apiKey.trim())
            .putString("zadarma.api_secret", apiSecret.trim())
            .apply()
    }

    fun logs(ctx: Context): List<String> =
        (ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("log", "") ?: "")
            .lines().filter { it.isNotBlank() }.takeLast(5).reversed()

    private fun pushLog(ctx: Context, line: String) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = ((p.getString("log", "") ?: "").lines().filter { it.isNotBlank() } + line).takeLast(20)
        p.edit().putString("log", next.joinToString("\n")).apply()
    }

    /** DEMO: speak the text locally, then report a simulated call. */
    fun simulate(ctx: Context, text: String, target: String, done: (Result) -> Unit) {
        val flag = java.util.concurrent.atomic.AtomicBoolean(false)
        var engine: TextToSpeech? = null
        fun report(ok: Boolean, note: String) {
            if (!flag.compareAndSet(false, true)) return
            try { engine?.stop(); engine?.shutdown() } catch (_: Exception) {}
            pushLog(ctx, if (ok) "DEMO call -> $target" else "DEMO call -> $target ($note)")
            done(if (ok)
                Ok(true, "DEMO: SimVoice прочитав текст (симуляція дзвінка на $target)")
            else
                Ok(true, "DEMO: симуляція дзвінка на $target (голос недоступний)"))
        }
        engine = TextToSpeech(ctx) { status ->
            val e = engine ?: return@TextToSpeech
            if (status != TextToSpeech.SUCCESS) { report(false, "tts unavailable"); return@TextToSpeech }
            try {
                e.setOnUtteranceCompletedListener { report(true, "") }
                e.speak(text, TextToSpeech.QUEUE_FLUSH, null, "solarchik-outbound")
            } catch (_: Exception) { report(false, "speak() failed"); return@TextToSpeech }
            android.os.Handler(ctx.mainLooper).postDelayed({ report(true, "") }, 15_000)
        }
    }

    /** MAINNET: real Zadarma v3 TTS call. Blocking — call it from Dispatchers.IO. */
    fun real(ctx: Context, text: String, targetE164: String): Result {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val keys = listOf(p.getString("zadarma.api_key", ""), p.getString("zadarma.api_secret", ""))
            .map { it.orEmpty().trim() }.filter { it.isNotBlank() }
        if (keys.isEmpty()) return Fail("Немає ключа Zadarma — введи в налаштуваннях")
        val body = JSONObject().put("phone", targetE164).put("text", text)
        var last = "HTTP ?"
        for (k in keys) {
            val (code, msg) = runCatching { post(body, k) }.getOrDefault(-1 to "?")
            last = "HTTP $code: $msg"
            if (code == 200) {
                pushLog(ctx, "MAINNET call -> $targetE164 (ok)")
                return Ok(false, "Задарма: виклик створено на $targetE164")
            }
        }
        pushLog(ctx, "MAINNET call -> $targetE164 FAILED ($last)")
        return Fail("Задарма відмовила: $last")
    }

    private fun post(body: JSONObject, apiKey: String): Pair<Int, String> {
        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Api-Key", apiKey)
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val txt = (conn.errorStream ?: conn.inputStream).bufferedReader().use { it.readText() }.take(200)
            return code to txt
        } finally {
            conn.disconnect()
        }
    }
}
