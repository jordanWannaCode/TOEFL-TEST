package com.toeflsimulator.evaluation.internal;
import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.toeflsimulator.common.*;
import com.toeflsimulator.evaluation.WritingEvaluation;
import com.toeflsimulator.scoring.SentenceScore;
@Service
public class JdbcWritingEvaluation implements WritingEvaluation {
    private final JdbcTemplate jdbc; private final Json json;
    public JdbcWritingEvaluation(JdbcTemplate jdbc,Json json) { this.jdbc=jdbc; this.json=json; }
    public JsonNode close(UUID session,Instant now) {
        var items=jdbc.queryForList("SELECT i.id,i.content_snapshot FROM simulator.session_items i WHERE i.session_id=? ORDER BY position",session);
        for(var row:items) {
            var content=json.read(row.get("content_snapshot").toString());
            if(!content.path("taskType").asText().equals("BUILD_A_SENTENCE")) {
                var responses=jdbc.queryForList("SELECT response FROM simulator.responses WHERE item_id=?",row.get("id"));
                String text=responses.isEmpty()?"":json.read(responses.getFirst().get("response").toString()).path("text").asText();
                jdbc.update("INSERT INTO simulator.evaluation_jobs(id,item_id,rubric_id,status,last_error) VALUES (?,?,?, ?,?) ON CONFLICT(item_id) DO NOTHING",UUID.randomUUID(),row.get("id"),content.path("rubricRef").asText(),text.isBlank()?"UNAVAILABLE":"PENDING",text.isBlank()?"EMPTY_RESPONSE":null);
            }
        }
        return report(session);
    }
    public JsonNode report(UUID session) {
        var items=jdbc.query("SELECT i.id,i.content_snapshot,coalesce(r.response,'{}'::jsonb) response,k.answers,k.explanation,j.id job_id,j.status,j.last_error,j.attempts,e.result,e.provider,e.model,e.prompt_version,e.evaluated_at,e.usage FROM simulator.session_items i LEFT JOIN simulator.responses r ON r.item_id=i.id LEFT JOIN simulator.answer_keys k ON k.question_version_id=i.question_version_id LEFT JOIN simulator.evaluation_jobs j ON j.item_id=i.id LEFT JOIN simulator.writing_evaluations e ON e.job_id=j.id WHERE i.session_id=? ORDER BY i.position",(rs,n)->{
            var item=new LinkedHashMap<String,Object>(); var content=json.read(rs.getString("content_snapshot")); var response=json.read(rs.getString("response"));
            item.put("itemId",rs.getObject("id",UUID.class)); item.put("content",content); item.put("response",response);
            if(content.path("taskType").asText().equals("BUILD_A_SENTENCE")) {
                var keys=json.read(rs.getString("answers")); int score=SentenceScore.calculate(keys,response);
                item.put("objectiveScore",Map.of("correct",score,"total",1)); item.put("acceptableVariants",keys.path("variants")); item.put("explanation",rs.getString("explanation"));
            } else {
                item.put("jobId",rs.getObject("job_id",UUID.class)); item.put("status",rs.getString("status")); item.put("errorCode",rs.getString("last_error")); item.put("attempts",rs.getInt("attempts"));
                item.put("feedback",rs.getString("result")==null?null:json.read(rs.getString("result")));
                if(rs.getString("result")!=null) { item.put("provider",rs.getString("provider")); item.put("model",rs.getString("model")); item.put("promptVersion",rs.getString("prompt_version")); item.put("evaluatedAt",rs.getTimestamp("evaluated_at").toInstant()); item.put("usage",json.read(rs.getString("usage"))); }
            }
            return item;
        },session);
        return json.tree(Map.of("sessionId",session,"section","WRITING","estimated",true,"disclaimer","AI feedback is an internal estimate, not an official ETS score.","items",items));
    }
    public JsonNode progress(UUID owner) {
        var rows=jdbc.queryForList("SELECT i.content_snapshot,e.result,e.evaluated_at,k.answers,r.response FROM simulator.exam_sessions s JOIN simulator.session_items i ON i.session_id=s.id LEFT JOIN simulator.answer_keys k ON k.question_version_id=i.question_version_id LEFT JOIN simulator.responses r ON r.item_id=i.id LEFT JOIN simulator.evaluation_jobs j ON j.item_id=i.id LEFT JOIN simulator.writing_evaluations e ON e.job_id=j.id WHERE s.owner_id=? AND s.section='WRITING' AND s.state<>'ACTIVE' ORDER BY s.submitted_at",owner);
        var types=new LinkedHashMap<String,Map<String,Object>>(); var categories=new TreeMap<String,Integer>(); var recommendations=new LinkedHashSet<String>(); var trends=new ArrayList<Object>();
        for(var row:rows) {
            String type=json.read(row.get("content_snapshot").toString()).path("taskType").asText();
            var metric=types.computeIfAbsent(type,k->new LinkedHashMap<>(Map.of("taskType",k,"attemptCount",0,"evaluatedCount",0,"scoreSum",0d,"objectiveCorrect",0,"objectiveTotal",0)));
            metric.put("attemptCount",(int)metric.get("attemptCount")+1);
            if(type.equals("BUILD_A_SENTENCE")) {
                int score=SentenceScore.calculate(json.read(row.get("answers").toString()),row.get("response")==null?json.tree(Map.of()):json.read(row.get("response").toString()));
                metric.put("objectiveCorrect",(int)metric.get("objectiveCorrect")+score);metric.put("objectiveTotal",(int)metric.get("objectiveTotal")+1);
            }
            if(row.get("result")!=null) {
                var result=json.read(row.get("result").toString()); metric.put("evaluatedCount",(int)metric.get("evaluatedCount")+1); metric.put("scoreSum",(double)metric.get("scoreSum")+result.path("internalScore").asDouble());
                result.path("findings").forEach(f->categories.merge(f.path("category").asText(),1,Integer::sum));
                result.path("recommendations").forEach(r->recommendations.add(r.path("text").asText()));
                trends.add(Map.of("taskType",type,"internalScore",result.path("internalScore").asDouble(),"proficiencyLevel",result.path("proficiencyLevel").asText(),"evaluatedAt",row.get("evaluated_at").toString()));
            }
        }
        types.values().forEach(m->{ int count=(int)m.get("evaluatedCount"); m.put("averageEstimatedScore",count==0?null:(double)m.get("scoreSum")/count); m.remove("scoreSum"); });
        var next=jdbc.queryForList("SELECT id,content->>'title' title FROM simulator.question_versions WHERE section='WRITING' AND status='APPROVED' AND NOT EXISTS (SELECT 1 FROM simulator.session_items i JOIN simulator.exam_sessions s ON s.id=i.session_id WHERE s.owner_id=? AND s.state<>'ACTIVE' AND i.question_version_id=simulator.question_versions.id) ORDER BY id LIMIT 3",owner);
        return json.tree(Map.of("attemptCount",rows.size(),"byTaskType",types.values(),"languageErrorFrequency",categories,"recommendations",recommendations,"proficiencyTrend",trends,"recommendedExercises",next));
    }
    public void retry(UUID session) {
        jdbc.update("UPDATE simulator.evaluation_jobs j SET status='PENDING',attempts=0,next_attempt_at=now(),lease_until=null,last_error=null FROM simulator.session_items i JOIN simulator.responses r ON r.item_id=i.id WHERE j.item_id=i.id AND i.session_id=? AND j.status IN ('FAILED','UNAVAILABLE') AND length(trim(r.response->>'text'))>0",session);
    }
}
