package com.toeflsimulator.content;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.toeflsimulator.common.Json;
import com.toeflsimulator.media.ObjectStorage;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
class ListeningImportTest {
    @Test void rejectsInvalidAnswerAndTranscriptAssociationsBeforeWriting() throws Exception {
        var mapper=new ObjectMapper(); var manifest=mapper.readTree(new ClassPathResource("listening/manifest.json").getInputStream());
        var exercise=(ObjectNode)manifest.path("exercises").get(0).deepCopy(); byte[] audio=new ClassPathResource("listening/"+exercise.path("audioFile").asText()).getInputStream().readAllBytes();
        var jdbc=mock(JdbcTemplate.class); var storage=mock(ObjectStorage.class); var importer=new ListeningImporter(jdbc,new Json(mapper),storage);
        ((ObjectNode)exercise.path("answers")).put("q1","not-an-option");
        assertThatThrownBy(()->importer.importExercise(exercise,audio)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc,storage);
        ((ObjectNode)exercise.path("answers")).put("q1","a"); ((ObjectNode)exercise.path("turns").get(0)).put("speakerId","unknown");
        assertThatThrownBy(()->importer.importExercise(exercise,audio)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc,storage);
    }
    @Test void rejectsCorruptedAudioChecksum() throws Exception {
        var mapper=new ObjectMapper(); var exercise=mapper.readTree(new ClassPathResource("listening/manifest.json").getInputStream()).path("exercises").get(0);
        var jdbc=mock(JdbcTemplate.class); var storage=mock(ObjectStorage.class);
        assertThatThrownBy(()->new ListeningImporter(jdbc,new Json(mapper),storage).importExercise(exercise,new byte[]{1,2,3})).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc,storage);
    }
}
