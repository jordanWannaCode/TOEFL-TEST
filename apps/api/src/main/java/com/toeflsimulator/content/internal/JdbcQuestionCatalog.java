package com.toeflsimulator.content.internal;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import com.toeflsimulator.content.QuestionCatalog;
import com.toeflsimulator.common.*;
@Service
public class JdbcQuestionCatalog implements QuestionCatalog {
    private final JdbcTemplate jdbc; private final Json json;
    public JdbcQuestionCatalog(JdbcTemplate jdbc, Json json) { this.jdbc=jdbc; this.json=json; }
    public JsonNode approvedContent(UUID id) {
        return jdbc.query("SELECT content FROM simulator.question_versions WHERE id=? AND status='APPROVED' AND section='READING'",(rs,n)->json.read(rs.getString(1)),id).stream().findFirst().orElseThrow(()->new ApiException(422,"UNAVAILABLE_CONTENT","Approved Reading content is unavailable."));
    }
}
