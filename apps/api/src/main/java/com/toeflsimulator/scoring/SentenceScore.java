package com.toeflsimulator.scoring;
import com.fasterxml.jackson.databind.JsonNode;
/** Exact token variants from the private, reviewed answer key. */
public final class SentenceScore {
    private SentenceScore() {}
    public static int calculate(JsonNode key,JsonNode response) {
        if(!response.path("elementIds").isArray()) return 0;
        for(var variant:key.path("variants")) if(variant.equals(response.path("elementIds"))) return 1;
        return 0;
    }
}
