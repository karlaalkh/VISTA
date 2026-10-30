"""Short, locally synthesized scanner effects; no API calls or extra dependencies."""
import array
import io
import math
import random
import sys
import wave
from functools import lru_cache


@lru_cache(maxsize=2)
def scan_sound(effect):
    if effect not in {"vitals_scan", "phone_scan"}:
        raise ValueError("Unknown drone sound effect.")
    rate = 24000
    duration = 1.8 if effect == "vitals_scan" else 1.5
    hum_frequency = 95 if effect == "vitals_scan" else 145
    noise = random.Random(7)
    samples = array.array("h")
    phase = 0.0
    filtered_noise = 0.0
    for index in range(round(rate * duration)):
        time = index / rate
        progress = time / duration
        envelope = min(1.0, time / 0.035, (duration - time) / 0.07)
        sweep = 650 + 1600 * (0.5 - 0.5 * math.cos(2 * math.pi * progress))
        phase += 2 * math.pi * sweep / rate
        pulse = 0.5 + 0.5 * math.sin(2 * math.pi * 5 * time)
        filtered_noise = 0.9 * filtered_noise + 0.1 * noise.uniform(-1, 1)
        value = (0.16 * math.sin(2 * math.pi * hum_frequency * time)
                 + 0.12 * pulse * math.sin(phase) + 0.08 * filtered_noise)
        # A brief acquisition chirp and a two-tone confirmation finish.
        if time < 0.12:
            value += 0.12 * math.sin(2 * math.pi * 1400 * time) * math.sin(math.pi * time / 0.12)
        if time > duration - 0.24:
            end_time = time - (duration - 0.24)
            tone = 1100 if end_time < 0.12 else 1650
            value += 0.15 * math.sin(2 * math.pi * tone * end_time)
        samples.append(round(max(-0.8, min(0.8, value * envelope)) * 32767))
    if sys.byteorder != "little":
        samples.byteswap()
    result = io.BytesIO()
    with wave.open(result, "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(rate)
        output.writeframes(samples.tobytes())
    return result.getvalue()
