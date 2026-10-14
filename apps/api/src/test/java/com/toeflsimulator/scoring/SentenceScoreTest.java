package com.toeflsimulator.scoring;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
class SentenceScoreTest {
    private final ObjectMapper json=new ObjectMapper();
    @Test void reviewedAdjunctVariantsAcceptedButIncompleteOrDuplicatedResponsesRejected() throws Exception {
        var key=json.readTree("{\"variants\":[[\"a\",\"b\",\"c\"],[\"a\",\"c\",\"b\"]]}");
        assertThat(SentenceScore.calculate(key,json.readTree("{\"elementIds\":[\"a\",\"b\",\"c\"]}"))).isEqualTo(1);
        assertThat(SentenceScore.calculate(key,json.readTree("{\"elementIds\":[\"a\",\"c\",\"b\"]}"))).isEqualTo(1);
        for(String response:new String[]{"{}","{\"elementIds\":[\"a\",\"b\"]}","{\"elementIds\":[\"a\",\"a\",\"c\"]}","{\"elementIds\":[\"b\",\"a\",\"c\"]}"}) assertThat(SentenceScore.calculate(key,json.readTree(response))).isZero();
    }
}
