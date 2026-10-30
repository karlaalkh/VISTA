import array
import base64
import io
import json
import os
import unittest
import wave
from unittest.mock import patch

import app
from sound_effects import scan_sound

Q1 = "Q1_wo_ist_dein_vater"
Q3 = "Q3_wann_hattest_du_zuletzt_kontakt"
Q4 = "Q4_was_hat_er_gesagt"


def outcome(node_id, key):
    return app.resolve(app.DIALOGUES[node_id], {"key":key, "confidence":1, "reply":"Enough."}, 20, "Test answer")


class NarrationTests(unittest.TestCase):
    def test_existing_narrator_env_spelling_is_supported_and_standard_name_wins(self):
        with patch.dict(os.environ, {"ELEVENSLABS_NARRATOR_VOICE_ID":"existing-narrator"}, clear=True):
            self.assertEqual("existing-narrator", app.narrator_voice_id())
            self.assertNotIn("ELEVENLABS_NARRATOR_VOICE_ID", app.configuration_issues())
            with patch.dict(os.environ, {"ELEVENLABS_NARRATOR_VOICE_ID":"standard-narrator"}):
                self.assertEqual("standard-narrator", app.narrator_voice_id())

    def test_narration_uses_its_own_voice_and_caches_fixed_lines(self):
        app.cached_narration.cache_clear()
        self.addCleanup(app.cached_narration.cache_clear)
        env = {"ELEVENLABS_API_KEY":"test-key", "ELEVENLABS_VOICE_ID":"mercer",
               "ELEVENSLABS_NARRATOR_VOICE_ID":"narrator", "VISTA_SPEECH_MODEL":"eleven_v3"}
        with patch.dict(os.environ, env, clear=True), patch.object(app, "request_bytes", return_value=b"narration") as api:
            self.assertEqual(b"narration", app.speak_narrator("Mercer approaches Maya."))
            app.speak_narrator("Mercer approaches Maya.")
            api.assert_called_once()
            url, body, headers = api.call_args.args
            self.assertIn("/narrator?output_format=mp3_44100_128", url)
            self.assertEqual("[calm] Mercer approaches Maya.", json.loads(body)["text"])
            self.assertEqual("test-key", headers["xi-api-key"])

    def test_opening_is_narrator_only_and_does_not_call_ai_or_mercer(self):
        with patch.object(app, "speak_narrator", return_value=b"narration") as narrator, \
                patch.object(app, "speak") as mercer, patch.object(app, "gemini_answer") as ai:
            scene = app.opening_scene()
            self.assertEqual(1, len(scene["segments"]))
            self.assertEqual("Narrator", scene["segments"][0]["speaker"])
            self.assertIn("Mercer steps", scene["segments"][0]["text"])
            narrator.assert_called_once()
            mercer.assert_not_called()
            ai.assert_not_called()

    def test_vitals_scene_narrates_movements_then_scans_then_confronts(self):
        result = outcome(Q1, "NONSENSE_OFFTOPIC")
        segments = app.directed_segments(app.DIALOGUES[Q1], result)
        self.assertEqual(["narration", "dialogue", "narration", "sound_effect", "dialogue", "narration", "dialogue"],
                         [s["kind"] for s in segments])
        self.assertIn("moves closer to Maya", segments[0]["text"])
        self.assertIn("Maya shifts", segments[2]["text"])
        self.assertEqual("vitals_scan", segments[3]["effect"])
        self.assertIn("Deception probability: high", segments[4]["text"])
        self.assertIn("eyes narrow", segments[5]["text"])
        self.assertIn("lie number one", segments[-1]["text"])

    def test_smartphone_scene_keeps_exact_report_and_mentions_family(self):
        segments = app.directed_segments(app.DIALOGUES[Q3], outcome(Q3, "OTHER_TIME"))
        self.assertIn("Maya's family", segments[0]["text"])
        self.assertEqual("phone_scan", segments[3]["effect"])
        self.assertEqual("Incoming call: Dad — five minutes ago.", segments[4]["text"])
        self.assertTrue(segments[-1]["text"].endswith("What did he tell you?"))

    def test_station_direction_precedes_final_order_and_does_not_change_ending(self):
        result = outcome(Q4, "EXPLAIN_INNOCENT")
        segments = app.directed_segments(app.DIALOGUES[Q4], result)
        self.assertEqual(["Mercer", "Narrator", "Mercer"], [s["speaker"] for s in segments])
        self.assertIn("signals to the officers", segments[1]["text"])
        self.assertEqual("Enough. We're taking her to the station.", segments[-1]["text"])
        self.assertEqual("TakeToStation", result["nextAction"])

    def test_scan_effect_is_local_and_never_sent_to_tts(self):
        with patch.object(app, "request_bytes", side_effect=AssertionError("No API call expected")):
            segment = app.voice_segment({"speaker":"Drone", "kind":"sound_effect", "effect":"vitals_scan", "text":"Vitals scan in progress"})
        self.assertEqual("audio/wav", segment["mimeType"])
        self.assertTrue(base64.b64decode(segment["audioBase64"]).startswith(b"RIFF"))

    def test_effects_have_bounded_level_and_expected_duration(self):
        for effect, duration in [("vitals_scan", 1.8), ("phone_scan", 1.5)]:
            audio = scan_sound(effect)
            with wave.open(io.BytesIO(audio), "rb") as wav:
                self.assertEqual(24000, wav.getframerate())
                self.assertEqual(1, wav.getnchannels())
                self.assertAlmostEqual(duration, wav.getnframes() / wav.getframerate(), places=3)
                samples = array.array("h", wav.readframes(wav.getnframes()))
            peak = max(abs(s) for s in samples)
            self.assertGreater(peak, 1000)
            self.assertLess(peak, 32767)
        self.assertNotEqual(scan_sound("vitals_scan"), scan_sound("phone_scan"))
        with self.assertRaises(ValueError):
            scan_sound("unknown")


if __name__ == "__main__":
    unittest.main()
