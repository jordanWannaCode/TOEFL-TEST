package com.toeflsimulator.scoring.internal;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.toeflsimulator.common.Json;
import com.toeflsimulator.scoring.SessionScorer;
@Service
public class JdbcSessionScorer implements SessionScorer {
    private final JdbcTemplate jdbc; private final Json json;
    public JdbcSessionScorer(JdbcTemplate jdbc, Json json) { this.jdbc=jdbc; this.json=json; }
    public JsonNode evaluate(UUID sessionId, Instant now) {
        var items=jdbc.query("SELECT i.id,i.content_snapshot,k.answers,k.explanation,coalesce(r.response,'{}'::jsonb) response FROM simulator.session_items i JOIN simulator.answer_keys k ON k.question_version_id=i.question_version_id LEFT JOIN simulator.responses r ON r.item_id=i.id WHERE i.session_id=? ORDER BY i.position",(rs,n)->{
            var key=json.read(rs.getString("answers")); var response=json.read(rs.getString("response")); var points=ObjectiveScore.calculate(key,response);
            return Map.<String,Object>of("itemId",rs.getObject("id",UUID.class),"title",json.read(rs.getString("content_snapshot")).path("title").asText(),"correct",points.correct(),"total",points.total(),"response",response,"correctAnswers",key,"explanation",rs.getString("explanation"));
        },sessionId);
        int correct=items.stream().mapToInt(x->(int)x.get("correct")).sum(); int total=items.stream().mapToInt(x->(int)x.get("total")).sum();
        var result=json.tree(Map.of("sessionId",sessionId,"objectiveScore",Map.of("correct",correct,"total",total),"estimated",false,"rubricVersion","reading-exact-match-v1","items",items));
        jdbc.update("INSERT INTO simulator.evaluations(session_id,kind,result,evaluated_at) VALUES (?,'OBJECTIVE',?::jsonb,?) ON CONFLICT(session_id) DO NOTHING",sessionId,json.write(result),Timestamp.from(now));
        return result;
    }
}
