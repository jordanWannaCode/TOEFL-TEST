"""Interfaces de synthèse ; aucune notation objective n'utilise ce service."""
from typing import Protocol
class SpeechProvider(Protocol):
    def synthesize(self, text: str, voice: str, speed: int = 155) -> bytes: ...
