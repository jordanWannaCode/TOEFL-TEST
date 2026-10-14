"""Génère les WAV synthétiques originaux ; ne publie aucun contenu automatiquement."""
import io, json, os, sys, wave, hashlib
from pathlib import Path
root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root / "apps/ai-service"))
from app.providers.espeak import EspeakSpeechProvider
provider = EspeakSpeechProvider(os.getenv("ESPEAK_EXECUTABLE", "espeak-ng"), os.getenv("ESPEAK_DATA_PATH"))
manifest = root / "content/listening/manifest.json"
data = json.loads(manifest.read_text())
output = root / "apps/api/src/main/resources/listening"
output.mkdir(parents=True, exist_ok=True)
for exercise in data["exercises"]:
    frames = bytearray(); params = None
    voices = {speaker["id"]: speaker["voice"] for speaker in exercise["speakers"]}
    for turn in exercise["turns"]:
        with wave.open(io.BytesIO(provider.synthesize(turn["text"], voices[turn["speakerId"]])), "rb") as audio:
            current = audio.getparams()[:3]
            if params and params != current: raise ValueError("Incompatible voice audio formats")
            params = current; frames.extend(audio.readframes(audio.getnframes()))
            frames.extend(bytes(int(audio.getframerate() * 0.25) * audio.getsampwidth() * audio.getnchannels()))
    path = output / exercise["audioFile"]
    with wave.open(str(path), "wb") as audio:
        audio.setnchannels(params[0]); audio.setsampwidth(params[1]); audio.setframerate(params[2]); audio.writeframes(frames)
    exercise["sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
    with wave.open(str(path)) as audio: exercise["durationMs"] = round(audio.getnframes() * 1000 / audio.getframerate())
    # Regeneration always requires a new review before publication.
    exercise["reviewed"] = False
    print(exercise["id"], exercise["durationMs"], "ms — DRAFT, review required")
manifest.write_text(json.dumps(data, indent=2) + "\n")
(output / "manifest.json").write_text(manifest.read_text())
