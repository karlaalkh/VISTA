# VOYA English AI dialogue

The Android player speaks **as Maya**. Gemini or OpenAI interprets the recording and selects an allowed response category. The server applies the authored branch and suspicion changes, writes a short English reaction, and asks ElevenLabs to voice Mercer. Credentials stay on this server.

## Run locally

You can store configuration in a **`.env` file in the VOYA project root**, next to `README.md`. The server loads that exact file at startup, regardless of the terminal's current directory. No extra Python packages are needed.

```powershell
Copy-Item .env.example .env
```

Fill in `GEMINI_API_KEY`, `ELEVENLABS_API_KEY`, Mercer's `ELEVENLABS_VOICE_ID` and `ELEVENLABS_NARRATOR_VOICE_ID`. The existing spelling `ELEVENSLABS_NARRATOR_VOICE_ID` is also accepted; you do not need to rename it. Optionally fill in `ELEVENLABS_DRONE_VOICE_ID`. Leave `VOYA_AI_PROVIDER=gemini` for Gemini. Then run:

```powershell
python server/app.py
```

The file uses `NAME=value`, not PowerShell `$env:NAME = ...` commands. Blank lines, comments and optional single/double quotes are supported. Values are literal: no shell execution, variable expansion or multiline values. `.env` and `.env.*` are excluded from Git; `.env.example` contains no credentials and may be committed. Keys stay on the Python server and are not bundled into the APK.

**Existing terminal environment variables override `.env`.** After moving keys to the file, stop Python and use a fresh terminal to avoid old `$env:` values taking precedence. Restart Python after editing `.env`. The example listens on `0.0.0.0` so the existing phone connection over Wi-Fi continues to work. It does not change the Android app's configured server URL.

You can also keep using terminal variables instead:

Python 3.11 or newer is sufficient; there are no packages to install. In PowerShell, set these environment variables in the terminal where you start the service (replace the placeholders locally; do not commit keys). For Gemini:

```powershell
$env:VOYA_AI_PROVIDER = 'gemini'
$env:GEMINI_API_KEY = '<your Google AI Studio API key>'
$env:ELEVENLABS_API_KEY = '<your ElevenLabs API key>'
$env:ELEVENLABS_VOICE_ID = '<voice ID selected for Mercer>'
python server/app.py
```

Create the Gemini key in [Google AI Studio](https://aistudio.google.com/apikey). Gemini handles transcription and dialogue classification in one audio request; OpenAI is not called in this mode. It defaults to `gemini-3.5-flash-lite`, with optional `VOYA_GEMINI_MODEL` override. Selected Gemini models have free-tier quotas, depending on project eligibility and limits; ElevenLabs still has its own billing/quota. See [Gemini pricing](https://ai.google.dev/gemini-api/docs/pricing) and [audio support](https://ai.google.dev/gemini-api/docs/audio). Data handling depends on the account and provider terms.

To use OpenAI instead:

```powershell
$env:VOYA_AI_PROVIDER = 'openai'
$env:OPENAI_API_KEY = '<your OpenAI API key>'
$env:ELEVENLABS_API_KEY = '<your ElevenLabs API key>'
$env:ELEVENLABS_VOICE_ID = '<voice ID selected for Mercer>'
python server/app.py
```

If `VOYA_AI_PROVIDER` is unset, a nonempty `GEMINI_API_KEY` selects Gemini; otherwise the server selects OpenAI. Explicit selection wins when both keys are present. Startup and `/health` show the active provider. Provider errors never silently switch to another provider. After changing variables or updating Python code, stop the server with Ctrl+C and restart it in the same terminal. No APK reinstall is needed to switch AI providers. Opening `/` shows server status; gameplay takes place in the Android app.

Choose an English voice in your ElevenLabs account. This implementation generates new speech using that voice; it does not clone the performer in the existing recordings. The three opening clips still use the bundled recordings.

## Narrator and scene sound

The narrator uses `ELEVENLABS_NARRATOR_VOICE_ID`, with `ELEVENSLABS_NARRATOR_VOICE_ID` accepted as an alias. If both are set, the standard spelling takes precedence. The narrator has a separate voice and a calm delivery; Mercer retains his own annoyed/angry performance. The server reports a missing narrator setting without printing any voice IDs or API keys.

After **Start Dialogue**, the app requests `/opening` and plays a short narrated scene before the three bundled Mercer opening lines. During the interrogation, the narrator describes Mercer approaching Maya, looking at the drone or her family, Maya shifting in her seat, and the drone beginning its checks. Contradiction outcomes get a more tense action beat before Mercer's confrontation. The authored directions live in `scene_directions.json`; they describe physical scene actions without inventing extra player speech or changing the selected story branch. The station ending includes Mercer signalling to the officers before his final order.

The vitals and smartphone checks include locally synthesized mechanical hums, electronic sweeps and confirmation chirps. These are new science-fiction scan effects inspired by the reference, not audio extracted from the video. They need no sound-generation API key or additional service calls. `/turn` segments distinguish `dialogue`, `narration` and `sound_effect`; the app displays each appropriately and keeps the microphone closed until the entire sequence finishes. Effects are standalone cues between the scan description and the drone report, so they do not mask speech.

Narration and drone reports are cached in server memory by text, voice ID and model. The first run generates new narration through ElevenLabs; later repetitions reuse cached audio until Python restarts. This adds narrated speech time and uses ElevenLabs credits on first generation. Live voice quality should be checked with the selected narrator voice.

## Mercer and the drone

After Maya answers `Q1_wo_ist_dein_vater`, Mercer says **“Drone, check her vitals.”** The authored outcome determines whether the drone reports low deception, high deception, or an inconclusive assessment. Disproved local leads and fantastical location claims trigger the high-deception report; silence, refusal and memory gaps are inconclusive. Mercer confronts Maya after the drone's report, then asks the next question selected by the existing branch rules. The key lists and reports are editable in `AfterAnswerCheck` in `dialogue_en.json`.

After Maya answers the last-contact question (`Q3_wann_hattest_du_zuletzt_kontakt`), Mercer says **“Drone, check her smartphone.”** The drone reports **“Incoming call: Dad — five minutes ago.”** Mercer then asks **“What did he tell you?”** These checks also play after silence, but explicit GameOver outcomes end the conversation without an extra scan. They are authored story events, not real measurements or access to the player's phone.

Mercer is written as rude, impatient and intimidating, with annoyed delivery by default and angry delivery for contradictions. His confrontation follows the evidence rather than preceding it. The app carries `detectedLieCount` between turns; a high-deception vitals outcome followed by a conflicting old-contact claim becomes “lie number two,” with a last-chance warning. A recent contact answer matches the phone evidence and is not accused of being a lie. An explicitly missed/unanswered call is separately identified by `callWasUnanswered`; it does not prove a conversation, increment the lie count, or incur the old-contact deception penalty. Failed requests do not advance the client count, so a retry cannot count the same lie twice. Restarting the game resets the count. Existing story transitions and explicit GameOver branches are preserved.

The `/turn` reply contains ordered `segments`, each with `speaker`, `kind`, `text`, `mimeType` and `audioBase64`. A scan scene plays narrator → Mercer command → narrator → scan effect → drone report → narrator → Mercer confrontation/question before enabling another answer. The drone has its own speaker label and robot illustration. Reinstall the updated APK for this playback format, then restart Python.

The ordinary interrogation endings now use `TakeToStation`, rather than releasing Maya. Mercer says “If you don't cooperate, I'll take you with me and we'll continue this conversation somewhere else.” An aggressive final answer instead gets “I'll teach you some manners.” Both then play the separate order “Enough. We're taking her to the station.” The Android completion screen identifies the station transfer. The source Unreal LET_GO sequence is removed from these resolved outcomes. Explicit GameOver branches and guard FreeRoam actions remain separate; a playable station scene is not yet implemented.

The app checks `/health` for protocol version 5 and the `drone_checks` feature before loading scenes or uploading a recording. An older Python process is rejected with instructions to restart it instead of silently playing a reply without the update. Startup prints the enabled drone checks, reactive Mercer, station-transfer ending, narrator and scan sounds. If those lines are missing, stop the existing Python process and run the updated file again.

The drone works with the existing ElevenLabs key and voice ID: it synthesizes 24 kHz PCM, applies a metallic ring-modulation effect and returns a mono WAV. Its two fixed reports are cached in server memory, so repeats do not request new drone speech until the server restarts. Independent speech segments are generated concurrently and played in the authored order. The first run may take longer and uses additional speech-generation credits. Actual voice quality should be checked on the device.

Optionally assign a separate ElevenLabs voice as the robot's base voice:

```powershell
$env:ELEVENLABS_DRONE_VOICE_ID = '<voice ID selected for the drone>'
python server/app.py
```

Stop the existing Python process with Ctrl+C first and use the same terminal so its Gemini/OpenAI, ElevenLabs and `VOYA_HOST` settings remain present. `ELEVENLABS_VOICE_ID` still belongs to Mercer. If no drone voice ID is configured, the robotic effect uses the Mercer voice as its source. This creates a distinct processed voice at playback; it does not create a new voice in your ElevenLabs account. You can choose or design an English drone voice there and paste its ID into the optional variable. Suggested voice-design description: “A calm, synthetic security drone with precise English pronunciation, clipped diction, a flat analytical delivery and a restrained mechanical tone.”

Audio API reference: [ElevenLabs supported formats](https://elevenlabs.io/docs/help-center/troubleshooting/what-audio-formats-do-you-support).

## Device connection

Without a local override, the Android emulator connects to `http://10.0.2.2:8787`. For a physical device connected by USB or wireless debugging, run `adb reverse tcp:8787 tcp:8787` and add `voya.dialogueUrl=http://127.0.0.1:8787` to the root `local.properties` file. This persists the device address for Android Studio Run and future builds on your computer; `local.properties` is excluded from Git. Repeat the reverse command after reconnecting the device. A command-line `'-PVOYA_DIALOGUE_URL=http://127.0.0.1:8787'` overrides the local setting for that build only. Rebuild after changing the URL. Cleartext HTTP is enabled only in debug builds. The service binds to localhost and has no authentication; keep it local. A shared deployment needs HTTPS, authentication and usage limits before exposing the paid endpoints.

Optional model overrides:

For a direct connection on the same trusted Wi-Fi network, bypass ADB reverse: set `voya.dialogueUrl=http://<computer-Wi-Fi-IP>:8787` in `local.properties`, rebuild the app, and restart Python in the existing credential terminal with `$env:VOYA_HOST = '<computer-Wi-Fi-IP>'`. Use the computer's address (from `ipconfig`), not the phone's address. If Windows prompts for Python firewall access, allow it on the private network used for testing. Verify `http://<computer-Wi-Fi-IP>:8787/health` from the phone before submitting an answer. The IP may change when the computer changes networks. To return to ADB reverse, reset the local URL to `http://127.0.0.1:8787`, set `VOYA_HOST` back to `127.0.0.1`, and rebuild/restart.

| Variable | Default |
| --- | --- |
| `VOYA_AI_PROVIDER` | Gemini when its key is set; otherwise OpenAI |
| `VOYA_GEMINI_MODEL` | `gemini-3.5-flash-lite` |
| `VOYA_DIALOGUE_MODEL` | `gpt-4o-mini` |
| `VOYA_TRANSCRIBE_MODEL` | `gpt-transcribe` |
| `VOYA_SPEECH_MODEL` | `eleven_v3` |
| `VOYA_PORT` | `8787` |

Availability depends on your provider accounts. API usage incurs charges. The app explains that recordings are sent for AI processing and labels generated voice playback. The service keeps recordings/transcripts in request memory and does not log them; provider retention is governed by your provider account settings. Failed answers can be retried or recorded again without advancing the story. Retries can make new provider calls.

## Dialogue data

`dialogue_en.json` contains the six interrogation nodes from the pasted German source. Stable German node IDs and sequence IDs remain unchanged so branches can be traced back to the original. Player-facing dialogue, classifier hints and suspicion reasons are English. `OptionsSource` plus `OptionOverrides` shares Q1/Q2 rules without duplicating them. The resolved options have the original `Index`, `Key`, `NextAction`, `ExpectedSlots`, `DeltaSuspicion` and sequence fields.

Low-confidence or invalid classifications select each node's authored fallback. Silence skips transcription and selects the fallback directly. Only the JSON determines transitions and suspicion; the AI cannot invent a destination, delta or world-state action. Suspicion starts at zero and is clamped to 0–100. The source specifies explicit `GameOver` branches but no numerical loss threshold, so reaching 100 alone does not invent an extra game-over rule.

For a plausible outside location, the source's scripted report disproving the lead is included in the same voiced turn and its +10 follow-up delta is applied with the initial -10. This is an authored fictional event, not a real external location check.

`guard_dialogue_en.json` translates the five supplied guard nodes, including bag/no-bag states, two attempts, object slots, world flags and objective actions. The service supports evaluating those nodes and returns their completion actions, sequence aliases and deferred object-slot lines. The `[calm]` tag stays at the start and `FOUND` stays uppercase. The object remark is deferred until study exit, rather than played while entering. **The Android demo currently plays the interrogation only**: it has no house exploration, inventory, study-exit event or Unreal cinematic runtime. Guard world actions are returned for a future scene controller to apply. `TakeToStation` ends the Android interrogation with the station-transfer completion screen.

The pasted source was fragmented and contained terminal escape noise. The English configuration reconstructs the complete supplied interrogation and guard entries. Unreal sequence identifiers are retained as references; the cinematic assets themselves are not imported into Android. All insult-sequence switches remain false as supplied.

## Check

If an answer fails, restart the server after updating the code (`Ctrl+C`, then `python server/app.py` in the same terminal). Startup reports the selected provider and missing credential variable names without printing their values. `/health` also returns `missingConfiguration`. A running server can be healthy while provider credentials or billing still need attention.

For each answer the terminal reports receipt and elapsed time for Gemini audio and response (or OpenAI Transcription and Response generation) and Mercer voice. Failures identify the stage and distinguish authentication, access, quota, missing models/voices, connection problems and timeouts. Recordings, transcripts, keys and raw provider error bodies are not logged. Provider requests time out after 45 seconds per network operation. No APK reinstall is needed for these server diagnostics.

```powershell
python -m unittest discover -s server -v
```

These tests exercise authored transitions, suspicion, confidence fallbacks, slot handling, guard outcomes and mocked AI/audio requests. They do not call providers or verify actual voice quality. Live voice and AI classification require the credentials above and a device/emulator smoke test.

API references: [OpenAI transcription](https://developers.openai.com/api/docs/guides/speech-to-text), [OpenAI structured outputs](https://developers.openai.com/api/docs/guides/structured-outputs), [ElevenLabs speech](https://elevenlabs.io/docs/api-reference/text-to-speech/convert).
