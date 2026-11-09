<img src="docs/icon.png" alt="Sol the robot" width="96" align="right">

# Sol — AI call secretary + money on Solana

**Sol answers the calls you miss, and every morning one swipe clears what came out of them: call back, remind, or pay on Solana.**

I'm Vadym Bilobrovets, a solo founder. I miss calls in meetings, and some of those calls are people asking to be paid back. So I built Sol: an Android app with an AI secretary that picks up on a real phone line, writes a short note, and turns the call into cards I clear in the morning. When a card is a payment, it's one tap and one approval in my own wallet, on Solana.

This is the **final submission build for Encode**. It is a **fixed** version (build `v1.2.10`, release `Sol-main-v2`) that closes the two things Encode asked for, plus the broader "Sol Plan v2" feature set described below.

> **Honesty, up front:** this is a hackathon build by one person. It has **not** been security-audited. Where something is a demo simulation rather than a live mainnet flow, it is labeled as such. No exaggerated mainnet / cross-chain claims — see the [Status matrix](#status--what-is-verified-vs-demo-vs-unverified).

---

## What Encode asked, and how it was closed

Encode's feedback asked for two specific things. Both are implemented, exercised end-to-end, and evidenced with a real devnet transaction and on-device screenshots.

| Encode ask | What we built | Status | Proof |
| --- | --- | --- | --- |
| **1. Demo one Solana feature end-to-end on DEVNET** (call → payment, or an on-chain action — a real devnet tx + Solscan link, not a simulation) | Circle **pay from the app with no wallet app installed** → real devnet transfer, settled on-chain, recipient credited, "Paid ✓" with a Solscan link | **Verified end-to-end** | real devnet tx `4ggv…8C6KsyJ` below + screenshots `docs/evidence-v2/04-fresh.png` → `05-settled.png` → `06-solscan.png` |
| **2. Simple onboarding for a user with NO wallet** | A **built-in on-device devnet wallet** (`LocalKey`, AES-GCM in AndroidKeyStore) that the player can choose when no wallet app is present — so onboarding and a first payment work without Phantom/Solflare | **Verified** | `wallet/LocalKey.kt`, `wallet/SolanaWallet.kt` (`isLocal`, `useLocal()`); screenshots `04`–`06` |

The devnet end-to-end run (Circle → Pay → settle on devnet → "Paid ✓" → Solscan) was executed on a device **without a wallet app installed**, which simultaneously proves both asks in one flow.

### Real DEVNET transaction (proof for Encode ask #1)

| Field | Value |
| --- | --- |
| What | Circle payment, 0.09 SOL, paid from the app with the built-in local wallet |
| Cluster | **Solana devnet** |
| Transaction | `4ggvdWMbHiTy5jRTk5nDRCwzVN11dwMZiARkmuFNbsrKCmsemYGNGbmMa7oY4RcDVQovvYvRxZjYi1XhL8C6KsyJ` |
| Solscan | https://solscan.io/tx/4ggvdWMbHiTy5jRTk5nDRCwzVN11dwMZiARkmuFNbsrKCmsemYGNGbmMa7oY4RcDVQovvYvRxZjYi1XhL8C6KsyJ?cluster=devnet |

Screenshots of that run (02 → 06) are in `docs/evidence-v2/`:
`02-circle.png` (debt) → `03-amount.png` (amount) → `04-fresh.png` (no wallet / local wallet offered) → `05-settled.png` (Paid ✓) → `06-solscan.png` (Solscan devnet link).

> **Funding note (honest):** the free faucet/airdrop was unreliable, so this devnet transfer was funded with a local `encode-payer` key for the demo. That is a **funding** detail, not a claim about production settlement.

---

## What's new in this build (Sol Plan v2)

This release adds the v2 feature set on top of the v1 product (calls, morning stack, Circle, habits, Seeker). Each row is grounded in a file that is in this repo.

| Feature | What it does | Where (in this repo) | Mode |
| --- | --- | --- | --- |
| **DEMO / MAINNET modes** | One switch that tags every flow as simulated (safe, no spend) or live. DEMO is the default; MAINNET is only toggled deliberately. | `core/AppMode.kt`, `ui/ModePanel.kt` | both |
| **Outbound calls (Sol dials out)** | Sol can *initiate* a call to a known contact. DEMO: local TTS + simulated call. MAINNET: real voice via Zadarma v3 `/outgoingcall`. | `core/OutboundCall.kt` (`ENDPOINT`, `simulate()`), `ui/OutboundPanel.kt` | DEMO here |
| **5 shared numbers, auto-select by region** | One number per region (🇨🇦 CA, 🇺🇸 US, 🇬🇧 GB, 🇩🇰 DK, 🇺🇦 UA); the app auto-picks the right one by SIM/network, then locale, overridable in settings. No number purchase. | `core/ZadNumberList.kt` (`NUMBERS`, `pick()`, `detectCountry()`) | both |
| **Circle — debt & pay** | People I save with a wallet address; **Send** / **Request**; a caller mention adds a debt to Circle linked to the call; on-chain "Paid ✓" detection. | `circle/Circle.kt`, `circle/CircleRules.kt`, `circle/SolanaPay.kt`, `ui/CircleScreen.kt` | both |
| **Aurora — pay from any network** (Encode bounty surface) | A pay-page link + reference tracking + a best-effort on-chain `verify?ref=` poll. DEMO simulates the "paid" state; settlement happens at the worker pay-page, not as an in-app on-chain cross-chain move. | `core/AuroraPay.kt` (`link()`, `verify()`, `demoMarkPaid()`), `ui/AuroraPanel.kt`, `ui/PricingPanel.kt` | DEMO here |
| **Anti-scam** | Warns when a caller dictates a wallet address; never pays an address I didn't save myself. | `screen/CallActions.kt` | both |
| **No-wallet onboarding** | Built-in on-device devnet wallet (AndroidKeyStore AES-GCM), chosen when no wallet app is present; Phantom/Solflare remain optional over Mobile Wallet Adapter 2.0. | `wallet/SolanaWallet.kt`, `wallet/LocalKey.kt`, `wallet/WalletDiag.kt` | devnet |
| **Shared-line config** | `solscanTx(sig)` builds the correct cluster-correct Solscan URL; shared-line + mode config. | `core/SolarchikConfig.kt` | both |

### Fixed in this build (regressions found in QA and corrected)

- **LISTENING overlay no longer blocks the "Me" tab.** A dismissed VoiceSheet left an invisible scrim covering the navigation, so tapping "Me" did nothing. Fix: `MainActivity.select()` now calls `VoiceSheet.dismissIfOpen(this)` (line 718) before switching tabs. Verified on-device — the "Me" tab responds while the mic overlay is up, and the scrim releases on tap. Evidence: `docs/evidence-v2/07-listening-over-me.png` … `12-me-tap-works.png`.

---

## What Sol does (the v1 product, unchanged)

- **Calls.** My carrier forwards missed calls to a Zadarma number; the call goes over SIP to an OpenAI Realtime voice agent that talks to the caller like a receptionist. A Cloudflare Worker saves the transcript, writes a short note (who / why / urgency) and extracts action cards: call back, reminder, or payment. The Calls tab keeps history.
- **Today — the morning stack.** All of the day's cards in one stack, each labeled with where it came from ("Call Ira back at 15:00, asked at 13:32"). Swipe right when done; swipe left for Later/Tomorrow/Dismiss; tap to act. When everything is handled, clock in.
- **Circle.** People I save with a wallet address, each with **Send** and **Request**. Request makes a Solana Pay link + QR and opens an SMS. On-chain "Paid ✓" detection is read-only.
- **Me.** Wallet with SOL / USDC / SKR, **Send** / **Receive** (QR), daily habits, settings, wallet diagnostics.
- **Sol (the mic).** Hold and ask "who do I owe?" or "send Ira what I owe." Sol answers from my own data and opens the right card. **He never sends anything by himself.**
- **Season agent + Verified Seeker.** Daily Seeker Season tasks from official sources; sign in with Solana + a Seeker Genesis Token check for a badge. Everything else works without a Seeker.
- **Tested on a real Seeker (v1):** Seed Vault connect, mainnet balances, Season task, Clock in.

A **v1 demo video** (with a real Seeker, mainnet) exists: https://youtu.be/-7SXHOq-W2g — note this is the v1/mainnet demo; the v2 devnet end-to-end above is documented with screenshots, not a new video.

## How it works

```mermaid
flowchart LR
    caller([Caller]) -->|GSM conditional forwarding| zad[One of 5 Zadarma numbers, auto-selected by region]
    zad -->|SIP| rt[OpenAI Realtime voice agent]
    rt -->|transcript| wk[Cloudflare Worker]
    wk -->|note + action cards| app[Sol Android app, Kotlin]
    app -->|Mobile Wallet Adapter 2.0| wallet[Phantom / Solflare / Seed Vault ... or]
    app -->|built-in on-device devnet wallet (LocalKey)| wallet2[LocalKey keypair]
    wallet -->|signed tx| sol[(Solana: mainnet or devnet)]
    wallet2 -->|signed tx| sol
    app -->|Aurora: pay-page link + ref + verify poll| aur[Aurora worker pay-page]
    app -->|Sol chat / voice / briefing| wk
```

- The Android app is native Kotlin, no WebView. It holds no API keys; every model call goes through the worker.
- Worker source: [`worker/`](worker/) — `solarchik-screen.js` (calls, SIP, notes, Sol), `assistant-extras.js` (call actions, briefing), `season-drops.js` / `season-rules.js` (Season), `seeker-verify.js` (Genesis Token), `aurora-payer-worker.js` (Aurora pay-page / ref).
- AI phone minutes are capped per call (3 min), per account and per day, so a stuck call can't run up a bill.

## Build & run from source

You need Java 17 + the Android SDK (API 35). The Gradle wrapper is in the repo.

```bash
cd artifacts/solarchik-handoff/android
echo "sdk.dir=$ANDROID_HOME" > local.properties        # only if ANDROID_HOME isn't set
./gradlew :app:testDebugUnitTest                       # unit + Robolectric tests
./gradlew :app:assembleDebug
```

- Debug output: `app/build/outputs/apk/debug/app-debug.apk` (build root can be redirected with `-PbuildRoot=<dir>`).
- Web side (the worker / dApp shell): `npm install`, then `npm run dev` (dev server on `0.0.0.0:8080`), `npm run build`, `npm run test`.
- **No environment variables are required for a debug build.** For a signed release build you provide `app/solarchik-release.jks` + its passwords (`keystore.properties` or `SOLARCHIK_STORE_PASSWORD` / `SOLARCHIK_KEY_PASSWORD`) — none of that is committed.

### APK (the artifact to try)

- File: `Sol-main-v2.apk` (attached as the single release asset on this repo's latest release, `v1.2.10`).
- **SHA-256:** `73648a1b694c66730d0f9b96934842f1aa4f86590b9d20f2e2c3eb0869cfb545`
- Package: `net.solardepin.solarchik.assistant` · signed with a release key (not a store key).
- Install: on Android 8.0+, allow "install unknown apps," then run it.

## Run the demo (matches the evidence)

1. Install `Sol-main-v2.apk`; allow the install.
2. Open it → **Today** (morning stack). Default mode is **DEMO** — safe, nothing spends.
3. **Circle** tab → add/choose a debt → pick an amount.
4. **Pay.** With **no wallet app installed**, the app offers the built-in local (devnet) wallet → tap Pay → sign locally → settles on devnet → **Paid ✓** + Solscan link. (Evidence `02`–`06`.)
5. While the **LISTENING…** mic overlay is up, tap **Me** — it dismisses the overlay and the tab opens (the fixed regression). (Evidence `07`–`12`.)
6. **Calls** tab: an outbound-call card (DEMO simulation) and the auto-selected region number for your SIM/locale.

## Status — what is verified vs demo vs unverified

This is the single most important table in the repo. It separates what was actually run on a device and evidenced from what is implemented-but-demo and what is deliberately not claimed.

| Claim | Status | Evidence / where |
| --- | --- | --- |
| Circle **pay on DEVNET with no wallet app** → real on-chain tx, recipient credited, "Paid ✓" + Solscan | **Verified end-to-end** | real devnet tx `4ggv…8C6KsyJ` (link above); screenshots `02`–`06` |
| **Onboarding with a NO-WALLET user** (built-in on-device devnet wallet, no Phantom/Solflare needed) | **Verified** | `wallet/LocalKey.kt`, `wallet/SolanaWallet.useLocal()`; screenshots `04`–`06` |
| **LISTENING overlay does not block the "Me" tab** (regression fix) | **Verified on-device** | `MainActivity.select()` L718 `VoiceSheet.dismissIfOpen`; screenshots `07`–`12` |
| **5 numbers + auto-select** by SIM→locale→fallback (overridable) | **Verified (unit)** | `core/ZadNumberList.kt` + `SolPlanV2Test` (part of the 9/9 green unit run) |
| **DEMO / MAINNET mode** switch and labeling | **Verified (unit + UI)** | `core/AppMode.kt` + `SolPlanV2Test`; Calls screen `09` |
| **Anti-scam** rules (no pay to an unsaved / caller-dictated address) | **Verified (unit)** | `screen/CallActions.kt` + unit tests |
| **Outbound calls** (Sol initiates a call) | **DEMO simulation here** | `OutboundCall.simulate()` (local TTS + simulated dial). Real = MAINNET + Zadarma v3; not exercised live for this submission. |
| **Aurora — pay from any network** | **Implemented: pay-page link + ref tracking + best-effort `verify?ref=` poll; DEMO simulates the paid state. Settlement happens at the worker pay-page, not as an in-app on-chain cross-chain move.** | `core/AuroraPay.kt` (`link`, `verify`, `demoMarkPaid`). Not claimed as proven live cross-chain settlement in-app. |
| **Mainnet real money / real calls** | **Available but not run for this submission** | requires owner double-OK; no spend/call was made. v1 mainnet txs below are historical (pre-v2). |
| **Full monkey / judge suite after the v2 fixes** | **Not re-run as a full suite after FIX2** | the base build's `monkey 621 pass / 0 fail` and judge `40/50` are from the pre-fixture base; the v2 unit suite (9/9) is green, but a full post-FIX2 monkey/judge pass was not re-run. |
| **Free airdrop / faucet funding** | **Unreliable** | devnet demo was funded via a local `encode-payer` key (see funding note). |

## Aurora, precisely (Encode bounty — what is real vs simulated)

To avoid over-claiming, here is exactly what the app does and does not do:

- **Implemented (in the APK):** opens an Aurora worker **pay-page link** (`core/AuroraPay.link(ref, demo)`), creates and persists a **payment reference** (`newRef` / `activeRef`), and runs a **best-effort** on-chain check `verify?ref=` (`AuroraPay.verify`) that reads `status:"paid"` / `pending` / unknown and never guesses "paid."
- **DEMO mode:** `demoMarkPaid()` / `demoPaid()` **simulate** the "Paid ✓" state locally so the flow is fully operable with zero real movement. The shared demo pay-page is `https://sol-anychain.davidbell1603.workers.dev/r/wh6yhcdkke`.
- **Not claimed:** that the in-app flow performs a live cross-chain settlement. Settlement is a property of the **worker pay-page**, not of the APK's on-chain code path. We do **not** present the app as executing cross-chain settlement on-chain in-app.
- **MAINNET path (available, not exercised here):** same worker endpoint in production; real funds move only after the owner's double-OK.

## Outbound calls, precisely

- **DEMO (default):** `OutboundCall.simulate()` plays local TTS and simulates the outbound dial, with a local log. No network charge, no real call.
- **MAINNET (available, not exercised here):** `OutboundCall` posts to Zadarma v3 `/outgoingcall` (`ENDPOINT`), with API key/secret stored in a private `SharedPreferences` (`solarchik.outbound`) and displayed **masked** in the UI only. Real minutes cost money, so this is gated behind MAINNET and the owner's double-OK.

## On-chain proof (mainnet, v1 — historical, before v2)

Two real **mainnet** payments made from the app on 10 Oct 2026, 0.01 SOL each, via Circle → Send → Phantom, ~3–4 s to settle, fee 0.00008 SOL. These are the **v1** (pre-v2) flows — included as prior evidence, not the v2 Encode proof.

| What | Slot | Transaction |
| --- | --- | --- |
| Circle payment #1 (mainnet) | 455365483 | [4z8PUAVD…ByLUnW6z](https://solscan.io/tx/4z8PUAVD99bFWRM2KHpAGFqarESNDxQBsKNVzsh3e2MCT65N79tgSKayRvfRSVZg8AMhJVnkA7ftUd22ByLUnW6z) |
| Circle payment #2 (mainnet) | 455400077 | [3BtXnkEf…F8bvdFFQbVTr](https://solscan.io/tx/3BtXnkEfWTbZbGSzEKFJVsrMTRYqBvGY39xMSd1gzdAEvWCBwFBLE13cYmqRug5xzgeA8Fv2XsDyF8bvdFFQbVTr) |

Wallets: [sender](https://solscan.io/account/8J3hxf1XSYV1HKVUJtwtQtVwSvSeaAyW5RmL8EqC67ic) · [recipient](https://solscan.io/account/5sH2TB1x5gjYz9ReBfx9DjTjVX1kieYY41zCKjU47peL).

## Security

- **Nothing moves without my approval.** Every transaction is signed in the wallet app over Mobile Wallet Adapter; the app never sees a private key, and the server never holds one. The built-in devnet wallet keeps its keypair in **AndroidKeyStore (AES-GCM)** — it is a devnet test key, never mainnet.
- **Simulated first.** Before a wallet opens, the app simulates the transaction; plain-language errors instead of a failed signature.
- **No addresses from calls.** A payment only goes to an address I saved myself in Circle; a caller-dictated address triggers a scam warning and is ignored.
- **App identity.** The wallet sees the app as `app.solardepin.net`, which serves a Digital Asset Links file for this package and signing certificate.
- **No secrets in the repo.** Keystore + passwords live outside git; worker keys are Cloudflare secrets; Zadarma API key/secret are set at runtime by the owner and masked in the UI.
- It hasn't been audited. Start with tiny amounts.

## Business

- **Free:** Sol, the wallet, Circle, habits, secretary minutes on the shared demo line.
- **Pro (proposed):** own secretary number, more minutes, cards from every call; Pro is offered in-app via the Aurora pay-page (DEMO in this submission) or a link, not a hard paywall.
- **Business (planned):** for small service businesses that miss calls while they work — own number, booking from the call, deposit requests.

Traction, honestly: live on mainnet (v1), tested by a small circle of family and friends, nothing billed yet.

## Roadmap

- Personal secretary numbers + Pro billing (USDC / SKR).
- Publish to the Solana dApp Store with a store key.
- Business-plan pilot: booking + deposits from the call card.
- An external security review.

## Repo map (where to look)

- `PITCH.md` — the pitch, in my words.
- `PRIVACY.md` — what data is used and where it goes.
- `docs/COLOSSEUM_SUBMISSION.md` — the full prior submission write-up.
- `docs/evidence-v2/` — **12 on-device screenshots of the v2 devnet end-to-end + the LISTENING fix.**
- `artifacts/solarchik-handoff/android/` — the native Android app (Kotlin) that is the artifact.
- `worker/` + `src/` + `migrations/` — the Cloudflare Worker + dApp shell + DB.

## Links

- Pitch: [PITCH.md](PITCH.md) · Privacy: [PRIVACY.md](PRIVACY.md)
- This submission: `Solar-DePIN-Hub/sol-encode-final` (public) — APK on its latest release.
- Prior builds: `Solar-DePIN-Hub/solarchik-assistant`, `Solar-DePIN-Hub/Solarchik`.
- X: [@SolarDePin](https://x.com/SolarDePin) · team.solardepinhub@gmail.com

## License

MIT, see [LICENSE](LICENSE). Built by Vadym Bilobrovets.
