package com.toeflsimulator.scoring.internal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class ObjectiveScoreTest {
    final ObjectMapper json=new ObjectMapper();
    @Test void missingLettersAreNormalizedAndEachGapIsScored() throws Exception {
        var score=ObjectiveScore.calculate(json.readTree("{\"g1\":\"row\",\"g2\":\"turn\"}"),json.readTree("{\"answers\":{\"g1\":\" ROW \",\"g2\":\"wrong\"}}"));
        assertThat(score).isEqualTo(new ObjectiveScore.Points(1,2));
    }
    @Test void unansweredChoicesScoreZeroWithoutChangingTheTotal() throws Exception {
        assertThat(ObjectiveScore.calculate(json.readTree("{\"choice\":\"b\"}"),json.readTree("{}"))).isEqualTo(new ObjectiveScore.Points(0,1));
    }
    @Test void objectiveChoiceUsesThePublishedKey() throws Exception {
        assertThat(ObjectiveScore.calculate(json.readTree("{\"choice\":\"b\"}"),json.readTree("{\"answers\":{\"choice\":\"b\"}}"))).isEqualTo(new ObjectiveScore.Points(1,1));
    }
}
