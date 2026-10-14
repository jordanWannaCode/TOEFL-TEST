import pytest
from app.providers.espeak import EspeakSpeechProvider
@pytest.mark.parametrize("text,voice,speed", [("", "en-us", 155), ("Hello", "unsupported", 155), ("Hello", "en-us", 0)])
def test_invalid_speech_input_never_starts_a_provider(text, voice, speed):
    with pytest.raises(ValueError):
        EspeakSpeechProvider("missing-executable").synthesize(text, voice, speed)
