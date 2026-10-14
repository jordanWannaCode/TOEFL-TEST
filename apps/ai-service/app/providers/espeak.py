import os
import subprocess
import tempfile
from pathlib import Path
from .speech import SpeechProvider
class EspeakSpeechProvider(SpeechProvider):
    def __init__(self, executable: str = "espeak-ng", data_path: str | None = None):
        self.executable, self.data_path = executable, data_path
    def synthesize(self, text: str, voice: str, speed: int = 155) -> bytes:
        if not text.strip() or voice not in {"en-us", "en-gb"} or not 100 <= speed <= 220:
            raise ValueError("Invalid speech input")
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "speech.wav"
            command = [self.executable, "-v", voice, "-s", str(speed), "-w", str(output)]
            if self.data_path: command.append("--path=" + self.data_path)
            command.append("--stdin")
            subprocess.run(command, input=text.encode(), check=True, timeout=60)
            return output.read_bytes()
