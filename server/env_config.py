"""Load literal NAME=value settings without dependencies or shell evaluation."""
import os
import re
from pathlib import Path


def load_env(path=None):
    path = Path(path) if path is not None else Path(__file__).resolve().parent.parent / ".env"
    if not path.is_file():
        return False

    settings = {}
    for number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        match = re.fullmatch(r"(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)", line)
        if match is None:
            raise ValueError(f"Invalid .env syntax at line {number}. Use NAME=value.")
        name, value = match.groups()
        if value.startswith(("'", '"')):
            quote = value[0]
            end = value.find(quote, 1)
            if end < 0 or (value[end + 1:].strip() and not value[end + 1:].strip().startswith("#")):
                raise ValueError(f"Invalid .env quoting at line {number}.")
            value = value[1:end]
        else:
            value = re.split(r"\s+#", value, maxsplit=1)[0].strip()
        settings[name] = value

    # Validate the entire file before changing the process environment.
    for name, value in settings.items():
        os.environ.setdefault(name, value)
    return True
