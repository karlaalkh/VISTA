import base64
import json
import io
import ssl
import array
import math
import wave
import unittest
from unittest.mock import patch, call
from urllib.error import HTTPError, URLError

import app

Q1 = "Q1_wo_ist_dein_vater"
Q2 = "Q2_sag_mir_wo_er_ist"
Q3 = "Q3_wann_hattest_du_zuletzt_kontakt"
Q4 = "Q4_was_hat_er_gesagt"


def resolve(node, key, suspicion=20, **slots):
    return app.resolve(app.DIALOGUES[node], {"key": key, "confidence": 1,
                                           "reply": "I see.", **slots}, suspicion, "Test answer")


class DialogueTests(unittest.TestCase):
    def setUp(self):
        provider = patch.dict(app.os.environ, {"VOYA_AI_PROVIDER": "openai"})
        provider.start()
        self.addCleanup(provider.stop)
        narrator = patch.object(app, "speak_narrator", return_value=b"test-narrator-mp3")
        narrator.start()
        self.addCleanup(narrator.stop)
        drone = patch.object(app, "speak_drone", return_value=b"test-drone-wav")
        drone.start()
        self.addCleanup(drone.stop)

    def test_provider_errors_identify_cause_without_exposing_response(self):
        cases = [
            (401, {"error":{"message":"SECRET-KEY"}}, "authentication failed"),
            (403, {}, "access denied"),
            (404, {"error":{"code":"model_not_found"}}, "not found"),
            (429, {"error":{"code":"insufficient_quota"}}, "credits or quota exhausted"),
            (429, {"detail":{"status":"quota_exceeded"}}, "credits or quota exhausted"),
            (429, {"error":{"code":"project_spend_limit_exceeded"}}, "usage limit reached"),
            (429, {}, "request limit reached"),
            (422, {}, "request rejected"),
            (500, {}, "HTTP 500"),
        ]
        for status, body, expected in cases:
            error = HTTPError("https://api.openai.com/v1/responses", status, "private", {},
                              io.BytesIO(json.dumps(body).encode()))
            with patch.object(app, "urlopen", side_effect=error):
                with self.assertRaises(app.ProviderError) as raised:
                    app.request_bytes("https://api.openai.com/v1/responses", b"", {})
                self.assertIn(expected, str(raised.exception))
                self.assertNotIn("SECRET-KEY", str(raised.exception))

    def test_network_failures_distinguish_timeout_and_certificate(self):
        cases = [(TimeoutError(), "timed out"),
                 (URLError(ssl.SSLCertVerificationError()), "certificate verification failed"),
                 (URLError("private network details"), "could not connect")]
        for error, expected in cases:
            with patch.object(app, "urlopen", side_effect=error):
                with self.assertRaises(app.ProviderError) as raised:
                    app.request_bytes("https://api.elevenlabs.io/v1/text-to-speech/test", b"", {})
                self.assertIn("ElevenLabs", str(raised.exception))
                self.assertIn(expected, str(raised.exception))
                self.assertNotIn("private network details", str(raised.exception))

    def test_health_configuration_reveals_names_only(self):
        with patch.dict(app.os.environ, {"OPENAI_API_KEY":"secret", "ELEVENLABS_API_KEY":"", "ELEVENLABS_VOICE_ID":" "}, clear=True):
            self.assertEqual(["ELEVENLABS_API_KEY", "ELEVENLABS_VOICE_ID", "ELEVENLABS_NARRATOR_VOICE_ID"], app.configuration_issues())

    def test_stage_error_labels_failing_component_and_hides_unexpected_details(self):
        with patch("builtins.print") as log:
            with self.assertRaisesRegex(app.ProviderError, "Mercer voice failed: API authentication failed"):
                app.run_stage("Mercer voice", lambda: (_ for _ in ()).throw(app.ProviderError("API authentication failed")))
            with self.assertRaisesRegex(app.ProviderError, "Transcription returned an invalid response"):
                app.run_stage("Transcription", lambda: (_ for _ in ()).throw(ValueError("private transcript")))
            self.assertNotIn("private transcript", str(log.call_args_list))

    def test_all_authored_edges_and_fallbacks_are_valid(self):
        self.assertEqual(11, len(app.DIALOGUES))
        for node in app.DIALOGUES.values():
            for option in node["Options"]:
                slots = {s["Key"]: "the restaurant" for s in option["ExpectedSlots"]}
                result = resolve(node["Name"], option["Key"], **slots)
                self.assertEqual(option["Key"], result["optionKey"])
                if result["nextAction"] == "StartDialogue":
                    self.assertIn(result["nextDialogue"], app.DIALOGUES)
                    self.assertTrue(result["reply"].endswith(app.DIALOGUES[result["nextDialogue"]]["Dialog"]))
                else:
                    self.assertEqual("", result["nextDialogue"])

    def test_q2_uses_its_own_suspicion_and_next_question(self):
        self.assertEqual(22, resolve(Q1, "INSULT_OR_AGGRESSION")["suspicion"])
        self.assertEqual(23, resolve(Q2, "INSULT_OR_AGGRESSION")["suspicion"])
        self.assertEqual(Q2, resolve(Q1, "COUNTERQUESTION")["nextDialogue"])
        self.assertEqual(Q3, resolve(Q2, "COUNTERQUESTION")["nextDialogue"])

    def test_workshop_branch_returns_to_contact_question(self):
        first = resolve(Q1, "LOCATION_WORKSHOP_OUTSIDE")
        self.assertEqual("Q5_wo_befindet_sich_diese_werkstatt", first["nextDialogue"])
        second = resolve(first["nextDialogue"], "WORKSHOP_LOCATION_ANY", first["suspicion"])
        self.assertEqual(Q3, second["nextDialogue"])
        self.assertEqual(20, second["suspicion"])

    def test_game_over_ignores_next_dialogue(self):
        result = resolve(Q2, "SELF_REVEAL")
        self.assertEqual("GameOver", result["nextAction"])
        self.assertEqual("", result["nextDialogue"])
        self.assertEqual(100, result["suspicion"])

    def test_contact_truth_and_deception_have_different_consequences(self):
        self.assertEqual(10, resolve(Q3, "TRUTH_RECENT")["suspicion"])
        self.assertEqual(32, resolve(Q3, "OTHER_TIME")["suspicion"])

    def test_control_description_and_self_reveal_have_different_endings(self):
        self.assertEqual("TakeToStation", resolve(Q4, "OVERRIDE_CONTROL")["nextAction"])
        self.assertEqual("GameOver", resolve(Q4, "SELF_REVEAL")["nextAction"])
        self.assertEqual("GameOver", resolve(Q4, "PAIRING_KEYCHAIN_LINK")["nextAction"])

    def test_confidence_threshold_and_unknown_keys_use_authored_fallback(self):
        for value in [0.1, -1, 2, float("nan"), "high", None]:
            result = app.resolve(app.DIALOGUES[Q1], {"key":"SELF_REVEAL", "confidence":value}, 0, "x")
            self.assertEqual("NO_INPUT", result["optionKey"])
            self.assertEqual(15, result["suspicion"])
        self.assertEqual("NO_INPUT", resolve(Q1, "INVENTED_KEY")["optionKey"])

    def test_place_slot_report_applies_followup_delta(self):
        result = resolve(Q1, "LOCATION_OUTSIDE_PLAUSIBLE", place="the restaurant")
        self.assertIn("Sir, we checked the restaurant. He is not there.", result["reply"])
        self.assertEqual(20, result["suspicion"])
        self.assertEqual(0, result["deltaSuspicion"])
        self.assertIn("Lead disproved", result["suspicionReason"])

    def test_missing_required_slot_falls_back(self):
        for value in ["", "[]{}", "x" * 121]:
            self.assertEqual("NO_INPUT", resolve(Q1, "LOCATION_OUTSIDE_PLAUSIBLE", place=value)["optionKey"])

    def test_guard_bag_state_and_second_attempt_failure(self):
        result = resolve("HSGuard_no_bag", "NEED_OBJECT", object="your medicine")
        self.assertEqual("@GuardFetchBag", result["nextSequence"])
        self.assertEqual("OBJ_HS_GET_BAG", result["completionStateActions"]["ObjectiveStatusActions"][0]["ObjectiveId"])
        self.assertNotIn("FOUND", result["reply"])
        self.assertEqual("[calm] I hope you FOUND your medicine.", result["deferredSlotLines"][0]["text"])
        for key in ["NOTHING", "GET_BAG_DIRECT", "BAD_EXCUSE_FINAL"]:
            failed = resolve("HSGuard_no_bag_attempt_2", key)
            self.assertEqual("Failed", failed["completionStateActions"]["ObjectiveStatusActions"][0]["Status"])

    def test_silence_skips_transcription_and_classification(self):
        with patch.object(app, "transcribe") as transcription, patch.object(app, "classify") as classify, \
                patch.object(app, "speak", return_value=b"test-mp3"):
            result = app.turn({"dialogueId": Q1, "suspicion": 0, "silent": True})
            transcription.assert_not_called()
            classify.assert_not_called()
            self.assertEqual("NO_INPUT", result["optionKey"])
            dialogue = [s for s in result["segments"] if s["kind"] == "dialogue"]
            self.assertEqual(b"test-mp3", base64.b64decode(dialogue[0]["audioBase64"]))
            self.assertEqual(["Mercer", "Drone", "Mercer"], [s["speaker"] for s in dialogue])

    def test_recording_contract_transcribes_and_voices_selected_branch(self):
        with patch.object(app, "transcribe", return_value="This morning.") as transcription, \
                patch.object(app, "classify", return_value={"key":"TRUTH_RECENT", "confidence":0.95, "reply":"All right."}), \
                patch.object(app, "speak", return_value=b"test-mp3") as speech:
            result = app.turn({"dialogueId": Q3, "suspicion":20,
                               "audioBase64":base64.b64encode(b"m4a").decode()})
            transcription.assert_called_once_with(b"m4a")
            self.assertEqual(2, speech.call_count)
            speech.assert_has_calls([call("[annoyed] All right. Drone, check her smartphone."),
                                     call("[annoyed] Five minutes ago. At least that part of your story holds up. What did he tell you?")], any_order=True)
            self.assertEqual(Q4, result["nextDialogue"])
            self.assertEqual(10, result["suspicion"])
            self.assertEqual(" ".join(s["text"] for s in result["segments"] if s["kind"] != "sound_effect"), result["reply"])
            self.assertIn("Incoming call: Dad — five minutes ago.", result["reply"])

    def test_invalid_requests_rejected_before_provider_calls(self):
        cases = [{"dialogueId":"missing"}, {"dialogueId":Q1,"suspicion":-1},
                 {"dialogueId":Q1,"audioBase64":"%%"}, {"dialogueId":Q1},
                 {"dialogueId":Q1,"history":"bad"}]
        with patch.object(app, "transcribe") as transcription:
            for payload in cases:
                with self.assertRaises(ValueError):
                    app.turn(payload)
            transcription.assert_not_called()

    def test_provider_refusal_does_not_advance_story(self):
        with patch.object(app, "openai_request", return_value={"status":"completed", "output":[]}):
            with self.assertRaises(app.ProviderError):
                app.classify(app.DIALOGUES[Q1], "hello", [])

    def test_schema_restricts_model_to_current_nodes_options(self):
        reply = {"key":"TRUTH_RECENT","confidence":1,"reply":"All right.","place":"","object":""}
        with patch.object(app, "openai_request", return_value={"status":"completed",
                "output":[{"content":[{"type":"output_text","text":json.dumps(reply)}]}]}) as api:
            self.assertEqual(reply, app.classify(app.DIALOGUES[Q3], "today", []))
            request = api.call_args.args[1]
            self.assertFalse(request["store"])
            self.assertEqual([o["Key"] for o in app.DIALOGUES[Q3]["Options"]],
                             request["text"]["format"]["schema"]["properties"]["key"]["enum"])


class GeminiTests(unittest.TestCase):
    def setUp(self):
        env = patch.dict(app.os.environ, {"GEMINI_API_KEY": "test-gemini-key",
                         "VOYA_AI_PROVIDER": "gemini"}, clear=True)
        env.start()
        self.addCleanup(env.stop)
        narrator = patch.object(app, "speak_narrator", return_value=b"test-narrator-mp3")
        narrator.start()
        self.addCleanup(narrator.stop)
        drone = patch.object(app, "speak_drone", return_value=b"test-drone-wav")
        drone.start()
        self.addCleanup(drone.stop)

    def candidate(self, **overrides):
        result = {"transcript": "This morning.", "key": "TRUTH_RECENT", "confidence": 0.95,
                  "place": "", "object": "", "reply": "All right.", **overrides}
        return json.dumps({"candidates": [{"finishReason": "STOP", "content": {
            "parts": [{"text": json.dumps(result)}]}}]}).encode()

    def test_provider_auto_selection_and_explicit_override(self):
        with patch.dict(app.os.environ, {"VOYA_AI_PROVIDER": "", "OPENAI_API_KEY": "old-key"}):
            self.assertEqual("gemini", app.ai_provider())
        with patch.dict(app.os.environ, {"VOYA_AI_PROVIDER": "openai"}):
            self.assertEqual("openai", app.ai_provider())
        with patch.dict(app.os.environ, {"VOYA_AI_PROVIDER": "unsupported"}):
            with self.assertRaises(app.ProviderError):
                app.ai_provider()

    def test_gemini_requires_its_key_instead_of_openai(self):
        self.assertEqual(["ELEVENLABS_API_KEY", "ELEVENLABS_VOICE_ID", "ELEVENLABS_NARRATOR_VOICE_ID"], app.configuration_issues())
        with patch.dict(app.os.environ, {"GEMINI_API_KEY": "", "OPENAI_API_KEY": "old-key"}):
            self.assertIn("GEMINI_API_KEY", app.configuration_issues())
            with self.assertRaises(app.ProviderError):
                app.gemini_answer(app.DIALOGUES[Q3], b"m4a", [])

    def test_gemini_sends_audio_schema_and_key_header(self):
        with patch.object(app, "request_bytes", return_value=self.candidate()) as api:
            transcript, classification = app.gemini_answer(app.DIALOGUES[Q3], b"m4a-audio", [])
            self.assertEqual("This morning.", transcript)
            self.assertEqual("TRUTH_RECENT", classification["key"])
            url, body, headers = api.call_args.args
            self.assertEqual("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent", url)
            self.assertNotIn("test-gemini-key", url)
            self.assertEqual("test-gemini-key", headers["x-goog-api-key"])
            payload = json.loads(body)
            audio = payload["contents"][0]["parts"][1]["inlineData"]
            self.assertEqual("audio/m4a", audio["mimeType"])
            self.assertEqual(b"m4a-audio", base64.b64decode(audio["data"]))
            schema = payload["generationConfig"]["responseJsonSchema"]
            self.assertIn("transcript", schema["required"])
            self.assertEqual([o["Key"] for o in app.DIALOGUES[Q3]["Options"]], schema["properties"]["key"]["enum"])

    def test_gemini_turn_never_calls_openai(self):
        with patch.object(app, "request_bytes", return_value=self.candidate()) as api, \
                patch.object(app, "transcribe") as transcribe, patch.object(app, "classify") as classify, \
                patch.object(app, "speak", return_value=b"mp3"):
            result = app.turn({"dialogueId": Q3, "suspicion":20,
                               "audioBase64":base64.b64encode(b"m4a").decode()})
            self.assertEqual(10, result["suspicion"])
            self.assertEqual(Q4, result["nextDialogue"])
            self.assertEqual("This morning.", result["transcript"])
            transcribe.assert_not_called()
            classify.assert_not_called()
            api.assert_called_once()

    def test_gemini_silence_uses_authored_fallback_even_if_model_returns_self_reveal(self):
        with patch.object(app, "request_bytes", return_value=self.candidate(transcript="", key="SELF_REVEAL")), \
                patch.object(app, "speak", return_value=b"mp3"):
            result = app.turn({"dialogueId": Q1, "audioBase64":base64.b64encode(b"m4a").decode()})
            self.assertEqual("NO_INPUT", result["optionKey"])
            self.assertEqual("StartDialogue", result["nextAction"])

    def test_explicit_silence_skips_gemini(self):
        with patch.object(app, "gemini_answer") as gemini, patch.object(app, "speak", return_value=b"mp3"):
            self.assertEqual("NO_INPUT", app.turn({"dialogueId":Q1, "silent":True})["optionKey"])
            gemini.assert_not_called()

    def test_gemini_blocked_or_incomplete_answer_is_retriable(self):
        for response in [b'{}', b'{"candidates":[{"finishReason":"SAFETY"}]}',
                         b'{"candidates":[{"finishReason":"MAX_TOKENS"}]}',
                         self.candidate(transcript=None)]:
            with patch.object(app, "request_bytes", return_value=response):
                with self.assertRaises(app.ProviderError):
                    app.gemini_answer(app.DIALOGUES[Q3], b"m4a", [])

    def test_gemini_failure_does_not_fall_back_to_paid_openai_or_call_speech(self):
        with patch.object(app, "gemini_answer", side_effect=app.ProviderError("Gemini quota exhausted")), \
                patch.object(app, "transcribe") as transcribe, patch.object(app, "speak") as speech:
            with self.assertRaisesRegex(app.ProviderError, "Gemini audio and response failed"):
                app.turn({"dialogueId":Q1, "audioBase64":base64.b64encode(b"m4a").decode()})
            transcribe.assert_not_called()
            speech.assert_not_called()

    def test_root_and_health_report_provider_without_keys(self):
        for path in ["/", "/health"]:
            handler = app.Handler.__new__(app.Handler)
            handler.path = path
            with patch.object(handler, "send_json") as send:
                handler.do_GET()
                status, payload = send.call_args.args
                self.assertEqual(200, status)
                self.assertEqual("gemini", payload["provider"])
                self.assertGreaterEqual(payload["protocolVersion"], 2)
                self.assertIn("drone_checks", payload["features"])
                self.assertIn("multi_speaker", payload["features"])
                self.assertIn("reactive_interrogation", payload["features"])
                self.assertIn("station_ending", payload["features"])
                self.assertIn("narrated_scenes", payload["features"])
                self.assertIn("drone_sound_effects", payload["features"])
                self.assertNotIn("test-gemini-key", json.dumps(payload))

    def test_gemini_http_error_is_labelled_correctly(self):
        error = HTTPError("https://generativelanguage.googleapis.com/", 429, "private", {}, io.BytesIO(b'{}'))
        with patch.object(app, "urlopen", side_effect=error):
            with self.assertRaisesRegex(app.ProviderError, "Gemini: request limit reached"):
                app.request_bytes("https://generativelanguage.googleapis.com/v1beta/models/test:generateContent", b"", {})

    def test_gemini_payment_error_requires_billing_action_instead_of_retry(self):
        error = HTTPError("https://generativelanguage.googleapis.com/", 402, "private", {},
                          io.BytesIO(b'{"error":{"code":402,"status":"RESOURCE_EXHAUSTED","message":"private"}}'))
        message = app.provider_failure(error, "Gemini")
        self.assertIn("payment required (402)", message)
        self.assertIn("eligible Free Tier project", message)
        self.assertNotIn("Please retry later", message)
        self.assertNotIn("private", message)


class DroneTests(unittest.TestCase):
    def test_first_question_places_vitals_check_before_next_question(self):
        result = resolve(Q1, "LOCATION_COMPANY_OR_WORK")
        segments = app.scene_segments(app.DIALOGUES[Q1], result)
        self.assertEqual(["Mercer", "Drone", "Mercer"], [s["speaker"] for s in segments])
        self.assertTrue(segments[0]["text"].endswith("Drone, check her vitals."))
        self.assertEqual("Biometrics stable. Consistency high. Deception probability: low.", segments[1]["text"])
        self.assertTrue(segments[2]["text"].endswith(app.DIALOGUES[Q3]["Dialog"]))

    def test_contact_answer_places_exact_phone_report_before_next_question(self):
        for key in ["TRUTH_RECENT", "OTHER_TIME", "NONCOOP_OTHER"]:
            segments = app.scene_segments(app.DIALOGUES[Q3], resolve(Q3, key))
            self.assertTrue(segments[0]["text"].endswith("Drone, check her smartphone."))
            self.assertEqual("Incoming call: Dad — five minutes ago.", segments[1]["text"])
            self.assertTrue(segments[2]["text"].endswith("What did he tell you?"))

    def test_drone_check_preserves_workshop_and_counterquestion_branches(self):
        for key, next_id in [("LOCATION_WORKSHOP_OUTSIDE", "Q5_wo_befindet_sich_diese_werkstatt"),
                             ("COUNTERQUESTION", Q2)]:
            result = resolve(Q1, key)
            segments = app.scene_segments(app.DIALOGUES[Q1], result)
            self.assertEqual(next_id, result["nextDialogue"])
            self.assertTrue(segments[-1]["text"].endswith(app.DIALOGUES[next_id]["Dialog"]))

    def test_terminal_branches_never_add_drone_checks_or_next_questions(self):
        for node in [Q1, Q3]:
            result = resolve(node, "SELF_REVEAL")
            segments = app.scene_segments(app.DIALOGUES[node], result)
            self.assertEqual(1, len(segments))
            self.assertEqual(result["reply"], segments[0]["text"])
        self.assertEqual(1, len(app.scene_segments(app.DIALOGUES[Q2], resolve(Q2, "DONT_KNOW"))))

    def test_robot_effect_produces_valid_distinct_wav_without_changing_duration(self):
        samples = array.array("h", [int(14000 * math.sin(2 * math.pi * 220 * i / 24000)) for i in range(2400)])
        pcm = samples.tobytes()
        result = app.robot_voice(pcm)
        with wave.open(io.BytesIO(result), "rb") as wav:
            self.assertEqual(24000, wav.getframerate())
            self.assertEqual(1, wav.getnchannels())
            self.assertEqual(2, wav.getsampwidth())
            self.assertEqual(2400, wav.getnframes())
            output = wav.readframes(wav.getnframes())
        self.assertNotEqual(pcm, output)
        self.assertTrue(any(output))
        for invalid in [b"", b"x"]:
            with self.assertRaises(app.ProviderError):
                app.robot_voice(invalid)

    def test_drone_voice_uses_optional_id_pcm_and_caches_static_reports(self):
        app.cached_drone_voice.cache_clear()
        self.addCleanup(app.cached_drone_voice.cache_clear)
        env = {"ELEVENLABS_API_KEY":"test-key", "ELEVENLABS_VOICE_ID":"mercer-test",
               "ELEVENLABS_DRONE_VOICE_ID":"drone-test", "VOYA_SPEECH_MODEL":"eleven_v3"}
        with patch.dict(app.os.environ, env, clear=True), patch.object(app, "request_bytes", return_value=b"\x00\x01" * 100) as api:
            result = app.speak_drone("Test report.")
            self.assertEqual(result, app.speak_drone("Test report."))
            api.assert_called_once()
            url, body, _ = api.call_args.args
            self.assertIn("/drone-test?output_format=pcm_24000", url)
            self.assertEqual("Test report.", json.loads(body)["text"])
            self.assertTrue(result.startswith(b"RIFF"))
            with patch.dict(app.os.environ, {"ELEVENLABS_DRONE_VOICE_ID":""}):
                app.speak_drone("Test report.")
                self.assertIn("/mercer-test?output_format=pcm_24000", api.call_args.args[0])

    def test_voice_rendering_keeps_mime_types_and_speakers(self):
        with patch.object(app, "speak_drone", return_value=b"wav") as drone, patch.object(app, "speak", return_value=b"mp3") as mercer:
            result = app.voice_segment({"speaker":"Drone", "text":"Incoming call: Dad — five minutes ago."})
            self.assertEqual("audio/wav", result["mimeType"])
            self.assertEqual(b"wav", base64.b64decode(result["audioBase64"]))
            drone.assert_called_once()
            mercer.assert_not_called()


class ConfrontationTests(unittest.TestCase):
    def test_disproved_location_triggers_high_vitals_and_first_lie(self):
        result = resolve(Q1, "LOCATION_OUTSIDE_PLAUSIBLE", place="the restaurant")
        result["detectedLieCount"] = 0
        segments = app.scene_segments(app.DIALOGUES[Q1], result)
        self.assertIn("Deception probability: high.", segments[1]["text"])
        self.assertIn("lie number one", segments[2]["text"])
        self.assertEqual("angry", segments[2]["delivery"])
        self.assertEqual(1, result["detectedLieCount"])
        self.assertEqual("vitals_deception_high", result["evidenceOutcome"])
        self.assertNotIn("lie number", segments[0]["text"])

    def test_uncertain_answers_do_not_become_proven_lies(self):
        for key in ["DONT_KNOW", "REFUSE", "NO_INPUT", "COUNTERQUESTION"]:
            result = resolve(Q1, key)
            result["detectedLieCount"] = 0
            segments = app.scene_segments(app.DIALOGUES[Q1], result)
            self.assertIn("Insufficient information", segments[1]["text"])
            self.assertEqual(0, result["detectedLieCount"])
            self.assertNotIn("lie number", segments[2]["text"])

    def test_phone_contradiction_escalates_persistent_lie_count(self):
        first = resolve(Q1, "NONSENSE_OFFTOPIC")
        app.scene_segments(app.DIALOGUES[Q1], first)
        second = resolve(Q3, "OTHER_TIME")
        second["detectedLieCount"] = first["detectedLieCount"]
        segments = app.scene_segments(app.DIALOGUES[Q3], second)
        self.assertEqual("Incoming call: Dad — five minutes ago.", segments[1]["text"])
        self.assertEqual(2, second["detectedLieCount"])
        self.assertIn("lie number two", segments[2]["text"])
        self.assertIn("consequences", segments[2]["text"])
        self.assertIn("doesn't match the call log", segments[2]["text"])
        self.assertEqual("angry", segments[2]["delivery"])
        self.assertTrue(segments[2]["text"].endswith("What did he tell you?"))

    def test_first_phone_contradiction_is_not_called_second_lie(self):
        result = resolve(Q3, "OTHER_TIME")
        segments = app.scene_segments(app.DIALOGUES[Q3], result)
        self.assertEqual(1, result["detectedLieCount"])
        self.assertIn("lie number one", segments[2]["text"])
        self.assertNotIn("lie number two", segments[2]["text"])

    def test_matching_contact_does_not_trigger_accusation(self):
        result = resolve(Q3, "TRUTH_RECENT")
        result["detectedLieCount"] = 1
        segments = app.scene_segments(app.DIALOGUES[Q3], result)
        self.assertEqual(1, result["detectedLieCount"])
        self.assertEqual("phone_consistent", result["evidenceOutcome"])
        self.assertNotIn("lie number", segments[2]["text"])

    def test_explicit_missed_call_is_not_proof_of_contact(self):
        result = resolve(Q3, "OTHER_TIME", callWasUnanswered=True)
        result["detectedLieCount"] = 0
        segments = app.scene_segments(app.DIALOGUES[Q3], result)
        self.assertEqual("phone_unanswered", result["evidenceOutcome"])
        self.assertEqual(0, result["detectedLieCount"])
        self.assertEqual(0, result["deltaSuspicion"])
        self.assertEqual(20, result["suspicion"])
        self.assertNotIn("lie number", segments[2]["text"])

    def test_angry_delivery_reaches_elevenlabs_without_changing_captions(self):
        with patch.object(app, "speak", return_value=b"audio") as speech:
            segment = {"speaker":"Mercer", "text":"That was lie number two. Last chance.", "delivery":"angry"}
            result = app.voice_segment(segment)
            speech.assert_called_once_with("[angry] That was lie number two. Last chance.")
            self.assertEqual(segment["text"], result["text"])

    def test_invalid_lie_counts_fail_before_ai_requests(self):
        for count in [-1, 21, True, "two"]:
            with self.assertRaises(ValueError):
                app.turn({"dialogueId":Q1, "detectedLieCount":count, "silent":True})

    def test_retry_does_not_double_count_a_lie(self):
        payload = {"dialogueId":Q3, "detectedLieCount":1, "audioBase64":base64.b64encode(b"m4a").decode()}
        with patch.dict(app.os.environ, {"VOYA_AI_PROVIDER":"gemini"}), \
                patch.object(app, "gemini_answer", return_value=("Yesterday.", {"key":"OTHER_TIME", "confidence":1, "reply":"Enough."})), \
                patch.object(app, "speak_drone", return_value=b"drone"), \
                patch.object(app, "speak_narrator", return_value=b"narrator"), \
                patch.object(app, "speak", side_effect=app.ProviderError("Temporary failure")):
            with self.assertRaises(app.ProviderError):
                app.turn(payload)
            self.assertEqual(1, payload["detectedLieCount"])
            with patch.object(app, "speak", return_value=b"mercer"):
                result = app.turn(payload)
            self.assertEqual(2, result["detectedLieCount"])
            self.assertEqual(1, payload["detectedLieCount"])


class StationEndingTests(unittest.TestCase):
    def test_normal_police_endings_transfer_maya_instead_of_releasing_her(self):
        for node_id in [Q4, "Q6_wo_befindet_sich_diese_werkstatt"]:
            node = app.DIALOGUES[node_id]
            for option in node["Options"]:
                result = resolve(node_id, option["Key"])
                if result["nextAction"] in {"GameOver", "StartDialogue"}:
                    continue
                self.assertEqual("TakeToStation", result["nextAction"])
                self.assertEqual("", result["nextDialogue"])
                self.assertNotIn("LET_GO", result["nextSequence"])
                segments = app.scene_segments(node, result)
                self.assertEqual(2, len(segments))
                self.assertEqual(["Mercer", "Mercer"], [s["speaker"] for s in segments])
                self.assertEqual("Enough. We're taking her to the station.", segments[-1]["text"])
                self.assertNotIn("You can go", result["reply"])

    def test_aggression_gets_manners_threat_before_station_order(self):
        result = resolve(Q4, "INSULT_OR_AGGRESSION")
        segments = app.scene_segments(app.DIALOGUES[Q4], result)
        self.assertEqual("I'll teach you some manners.", segments[0]["text"])
        self.assertEqual("angry", segments[0]["delivery"])
        self.assertEqual("Enough. We're taking her to the station.", segments[1]["text"])

    def test_generated_release_text_cannot_override_authored_ending(self):
        result = app.resolve(app.DIALOGUES[Q4], {"key":"EXPLAIN_INNOCENT", "confidence":1,
                            "reply":"You can go for now. You're free to leave."}, 20, "He is innocent.")
        self.assertNotIn("You can go", result["reply"])
        self.assertNotIn("free to leave", result["reply"])
        self.assertIn("If you don't cooperate, I'll take you with me", result["reply"])
        self.assertTrue(result["reply"].endswith("Enough. We're taking her to the station."))

    def test_guard_free_roam_and_explicit_game_over_remain_separate(self):
        self.assertEqual("FreeRoam", resolve("HSGuard_entry_attempt_1", "NOTHING")["nextAction"])
        self.assertEqual("GameOver", resolve(Q4, "SELF_REVEAL")["nextAction"])


if __name__ == "__main__":
    unittest.main()
