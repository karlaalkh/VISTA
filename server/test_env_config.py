import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from env_config import load_env


class EnvConfigTests(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        self.path = Path(folder.name) / ".env"
        environment = patch.dict(os.environ, {}, clear=True)
        environment.start()
        self.addCleanup(environment.stop)

    def test_loads_bom_quotes_comments_and_empty_optional_settings(self):
        self.path.write_text('\ufeff# Local settings\nVISTA_AI_PROVIDER=gemini\n'
                             'GEMINI_API_KEY="secret#with=punctuation" # comment\n'
                             "export ELEVENLABS_API_KEY='another-secret'\n"
                             'VISTA_HOST=0.0.0.0 # Wi-Fi\nELEVENLABS_DRONE_VOICE_ID=\n', encoding="utf-8")
        self.assertTrue(load_env(self.path))
        self.assertEqual("secret#with=punctuation", os.environ["GEMINI_API_KEY"])
        self.assertEqual("another-secret", os.environ["ELEVENLABS_API_KEY"])
        self.assertEqual("0.0.0.0", os.environ["VISTA_HOST"])
        self.assertEqual("", os.environ["ELEVENLABS_DRONE_VOICE_ID"])

    def test_existing_environment_wins(self):
        self.path.write_text("GEMINI_API_KEY=file-secret\n", encoding="utf-8")
        os.environ["GEMINI_API_KEY"] = "terminal-secret"
        load_env(self.path)
        self.assertEqual("terminal-secret", os.environ["GEMINI_API_KEY"])

    def test_missing_file_keeps_terminal_setup_working(self):
        os.environ["GEMINI_API_KEY"] = "terminal-secret"
        self.assertFalse(load_env(self.path))
        self.assertEqual("terminal-secret", os.environ["GEMINI_API_KEY"])

    def test_values_are_literal_without_shell_or_variable_expansion(self):
        value = "$(do-not-run)${HOME}\\literal"
        self.path.write_text("GEMINI_API_KEY='" + value + "'\n", encoding="utf-8")
        load_env(self.path)
        self.assertEqual(value, os.environ["GEMINI_API_KEY"])

    def test_invalid_file_does_not_partially_load_or_expose_secrets(self):
        for line in ["private-secret-without-equals", 'KEY="private-secret', 'KEY="private-secret" garbage']:
            self.path.write_text("FIRST=valid\n" + line + "\n", encoding="utf-8")
            with self.assertRaises(ValueError) as error:
                load_env(self.path)
            self.assertIn("line 2", str(error.exception))
            self.assertNotIn("private-secret", str(error.exception))
            self.assertNotIn("FIRST", os.environ)


if __name__ == "__main__":
    unittest.main()
