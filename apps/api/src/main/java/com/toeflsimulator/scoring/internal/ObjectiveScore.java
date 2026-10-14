package com.toeflsimulator.scoring.internal;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
public final class ObjectiveScore {
    public record Points(int correct, int total) {}
    private ObjectiveScore() {}
    public static Points calculate(JsonNode key, JsonNode response) {
        int correct=0;
        var fields=key.fields();
        while(fields.hasNext()) {
            var field=fields.next();
            String actual=response.path("answers").path(field.getKey()).asText("").strip().toLowerCase(Locale.ROOT);
            String expected=field.getValue().asText().strip().toLowerCase(Locale.ROOT);
            if (actual.equals(expected)) correct++;
        }
        return new Points(correct,key.size());
    }
}
