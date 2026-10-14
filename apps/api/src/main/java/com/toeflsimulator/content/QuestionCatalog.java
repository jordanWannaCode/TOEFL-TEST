package com.toeflsimulator.content;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
public interface QuestionCatalog {
    JsonNode approvedContent(UUID questionVersionId);
}
