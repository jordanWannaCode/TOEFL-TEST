package com.toeflsimulator.evaluation;
import java.util.UUID;
import java.time.Instant;
import com.fasterxml.jackson.databind.JsonNode;
public interface WritingEvaluation {
    JsonNode close(UUID session, Instant now);
    JsonNode report(UUID session);
    void retry(UUID session);
    JsonNode progress(UUID owner);
}
