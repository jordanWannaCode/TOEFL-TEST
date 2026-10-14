package com.toeflsimulator.scoring;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
public interface SessionScorer {
    JsonNode evaluate(UUID sessionId, Instant evaluatedAt);
}
