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
        var items=jdbc.query("SELECT i.id,i.content_snapshot,k.answers,k.explanation,k.feedback,coalesce(r.response,'{}'::jsonb) response FROM simulator.session_items i JOIN simulator.answer_keys k ON k.question_version_id=i.question_version_id LEFT JOIN simulator.responses r ON r.item_id=i.id WHERE i.session_id=? ORDER BY i.position",(rs,n)->{
            var key=json.read(rs.getString("answers")); var response=json.read(rs.getString("response")); var points=ObjectiveScore.calculate(key,response);
            var content=json.read(rs.getString("content_snapshot")); var feedback=json.read(rs.getString("feedback"));
            var detail=new ArrayList<Object>();
            for(var q:content.path("questions")) { String id=q.path("id").asText(); detail.add(Map.of("questionId",id,"prompt",q.path("prompt").asText(),"skill",q.path("skill").asText(),"correct",response.path("answers").path(id).asText().equals(key.path(id).asText()),"explanation",feedback.path(id).asText())); }
            var value=new LinkedHashMap<String,Object>(Map.<String,Object>of("itemId",rs.getObject("id",UUID.class),"title",json.read(rs.getString("content_snapshot")).path("title").asText(),"correct",points.correct(),"total",points.total(),"response",response,"correctAnswers",key,"explanation",rs.getString("explanation"))); value.put("questionFeedback",detail); value.put("content",content); return value;
        },sessionId);
        int correct=items.stream().mapToInt(x->(int)x.get("correct")).sum(); int total=items.stream().mapToInt(x->(int)x.get("total")).sum();
        String section=jdbc.queryForObject("SELECT section FROM simulator.exam_sessions WHERE id=?",String.class,sessionId);
        var skills=new TreeSet<String>();
        for(var item:items) for(var feedback:json.tree(item.get("questionFeedback"))) if(!feedback.path("correct").asBoolean()) skills.add(feedback.path("skill").asText());
        var result=json.tree(Map.of("section",section,"skillsToImprove",skills,"sessionId",sessionId,"objectiveScore",Map.of("correct",correct,"total",total),"estimated",false,"rubricVersion",section.equals("LISTENING")?"listening-exact-match-v1":"reading-exact-match-v1","items",items));
        jdbc.update("INSERT INTO simulator.evaluations(session_id,kind,result,evaluated_at) VALUES (?,'OBJECTIVE',?::jsonb,?) ON CONFLICT(session_id) DO NOTHING",sessionId,json.write(result),Timestamp.from(now));
        return result;
    }
}
