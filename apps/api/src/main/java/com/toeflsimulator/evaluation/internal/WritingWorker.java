package com.toeflsimulator.evaluation.internal;
import java.util.*;
import java.time.*;
import java.sql.Timestamp;
import java.net.URI;
import java.net.http.*;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import com.toeflsimulator.common.Json;
import com.fasterxml.jackson.databind.JsonNode;
@Component
public class WritingWorker {
    private final JdbcTemplate jdbc; private final Json json; private final TransactionTemplate transactions;
    private final String url,token; private final Clock clock;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public WritingWorker(JdbcTemplate jdbc,Json json,PlatformTransactionManager tx,Clock clock,@Value("${AI_SERVICE_URL:http://ai-service:8000}") String url,@Value("${AI_INTERNAL_TOKEN:}") String token) {
        this.jdbc=jdbc; this.json=json; this.transactions=new TransactionTemplate(tx); this.url=url; this.token=token; this.clock=clock;
    }
    @Scheduled(fixedDelayString="${WRITING_WORKER_DELAY_MS:1000}")
    public void run() {
        Map<String,Object> job=transactions.execute(status->{
            var rows=jdbc.queryForList("SELECT id FROM simulator.evaluation_jobs WHERE (status='PENDING' AND next_attempt_at<=?) OR (status='PROCESSING' AND lease_until<=?) ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1",ts(clock.instant()),ts(clock.instant()));
            if(rows.isEmpty()) return null;
            var id=rows.getFirst().get("id"); var lease=clock.instant().plusSeconds(65);
            jdbc.update("UPDATE simulator.evaluation_jobs SET status='PROCESSING',attempts=attempts+1,lease_until=? WHERE id=?",ts(lease),id);
            var value=jdbc.queryForMap("SELECT j.*,i.content_snapshot,r.response,b.definition,b.prompt_version FROM simulator.evaluation_jobs j JOIN simulator.session_items i ON i.id=j.item_id JOIN simulator.responses r ON r.item_id=i.id JOIN simulator.evaluation_rubrics b ON b.id=j.rubric_id WHERE j.id=?",id);
            value.put("lease",lease); return value;
        });
        if(job==null) return;
        try {
            if(token.isBlank()) { fail(job,"UNAVAILABLE","INTERNAL_AUTH_UNCONFIGURED"); return; }
            var content=json.read(job.get("content_snapshot").toString()); String text=json.read(job.get("response").toString()).path("text").asText();
            var request=Map.of("evaluationId",job.get("id"),"taskType",content.path("taskType").asText(),"question",content,"response",text,"rubric",json.read(job.get("definition").toString()),"promptVersion",job.get("prompt_version"));
            var response=client.send(HttpRequest.newBuilder(URI.create(url+"/api/v1/ai/evaluations/writing")).timeout(Duration.ofSeconds(45)).header("Authorization","Bearer "+token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.write(request))).build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()==503 || response.statusCode()==401) { fail(job,"UNAVAILABLE","EVALUATION_UNAVAILABLE"); return; }
            if(response.statusCode()!=200) { fail(job,"RETRY","PROVIDER_FAILURE"); return; }
            if(response.body().length()>262144) throw new IllegalStateException("Output size limit");
            var result=json.read(response.body()); validate(result,text,job);
            transactions.executeWithoutResult(status->{
                // Lease fencing prevents a stale worker from overwriting a recovered job.
                int count=jdbc.update("UPDATE simulator.evaluation_jobs SET status='COMPLETED',lease_until=null,last_error=null WHERE id=? AND status='PROCESSING' AND lease_until=?",job.get("id"),ts((Instant)job.get("lease")));
                if(count==1) jdbc.update("INSERT INTO simulator.writing_evaluations(job_id,result,provider,model,prompt_version,evaluated_at,usage) VALUES (?,?::jsonb,?,?,?,?,?::jsonb) ON CONFLICT(job_id) DO NOTHING",job.get("id"),json.write(result.path("feedback")),result.path("provider").asText(),result.path("model").asText(),job.get("prompt_version"),ts(clock.instant()),json.write(result.path("usage")));
            });
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); fail(job,"RETRY","INTERRUPTED"); }
        catch(Exception e) { fail(job,"RETRY","INVALID_OR_UNREACHABLE_EVALUATION"); }
    }
    private void validate(JsonNode result,String text,Map<String,Object> job) {
        var feedback=result.path("feedback"); var expected=json.read(job.get("definition").toString());
        if(!result.path("evaluationId").asText().equals(job.get("id").toString()) || !result.path("rubricVersion").asText().equals(job.get("rubric_id")) || !result.path("promptVersion").asText().equals(job.get("prompt_version")) || !feedback.path("estimated").asBoolean() || !feedback.path("internalScore").isNumber() || feedback.path("internalScore").asDouble()<0 || feedback.path("internalScore").asDouble()>5 || !result.path("model").isTextual() || !result.path("provider").isTextual()) throw new IllegalArgumentException("Invalid evaluation metadata");
        Set<String> ids=new HashSet<>(); expected.path("criteria").forEach(c->ids.add(c.path("id").asText()));
        if(feedback.path("criteria").size()!=ids.size()) throw new IllegalArgumentException("Invalid criteria");
        for(var c:feedback.path("criteria")) { if(!ids.remove(c.path("id").asText()) || !c.path("score").isNumber() || c.path("score").asDouble()<0 || c.path("score").asDouble()>5 || c.path("evidence").isEmpty()) throw new IllegalArgumentException("Invalid criterion"); for(var span:c.path("evidence")) requireSpan(text,span.asText()); }
        for(var f:feedback.path("findings")) requireSpan(text,f.path("originalSpan").asText());
        for(var r:feedback.path("recommendations")) requireSpan(text,r.path("evidence").asText());
    }
    private void requireSpan(String text,String span) { if(span.isBlank() || !text.contains(span)) throw new IllegalArgumentException("Unsupported evidence"); }
    private void fail(Map<String,Object> job,String state,String error) {
        int attempts=((Number)job.get("attempts")).intValue();
        String target=state.equals("RETRY")?(attempts>=3?"FAILED":"PENDING"):state;
        jdbc.update("UPDATE simulator.evaluation_jobs SET status=?,last_error=?,lease_until=null,next_attempt_at=? WHERE id=? AND status='PROCESSING' AND lease_until=?",target,error,ts(clock.instant().plusSeconds(Math.min(60,attempts*2L))),job.get("id"),ts((Instant)job.get("lease")));
    }
    private Timestamp ts(Instant now) { return Timestamp.from(now); }
}
