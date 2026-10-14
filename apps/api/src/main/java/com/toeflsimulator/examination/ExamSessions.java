package com.toeflsimulator.examination;
import java.util.*;
import java.time.*;
import java.sql.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.toeflsimulator.common.*;
import com.toeflsimulator.content.QuestionCatalog;
import com.toeflsimulator.scoring.SessionScorer;
@Service
public class ExamSessions {
    private final JdbcTemplate jdbc; private final Json json; private final Clock clock;
    private final QuestionCatalog catalog; private final SessionScorer scorer;
    public ExamSessions(JdbcTemplate jdbc, Json json, Clock clock, QuestionCatalog catalog, SessionScorer scorer) {
        this.jdbc=jdbc; this.json=json; this.clock=clock; this.catalog=catalog; this.scorer=scorer;
    }
    public record Item(UUID id, JsonNode content, JsonNode response, Instant savedAt, boolean available) {}
    public record SessionView(UUID id,String state,int version,UUID blueprintVersionId,Instant startedAt,Instant expiresAt,Instant serverTime,List<Item> items,String section,String mode,JsonNode playbackPolicy,String navigationPolicy,int currentPosition) {}
    public record Saved(int version,Instant savedAt,Instant serverTime) {}
    public record Closed(UUID id,String state,int version,Instant submittedAt) {}
    private record Session(UUID id,UUID blueprint,String state,int version,Instant started,Instant expires,Instant submitted,String section,String mode,JsonNode policy,String navigation,int position) {}
    public List<Map<String,Object>> blueprints(String section,String mode) {
        String filter=section.equals("READING")?" AND name='Reading practice'":"";
        return jdbc.query("SELECT DISTINCT ON(name) id,name,version,duration_seconds,verification_status,section,mode,playback_policy,navigation_policy FROM simulator.blueprint_versions WHERE section=? AND mode=?"+filter+" ORDER BY name,version DESC",(rs,n)->Map.of("id",rs.getObject("id",UUID.class),"name",rs.getString("name"),"version",rs.getInt("version"),"durationSeconds",rs.getInt("duration_seconds"),"verificationStatus",rs.getString("verification_status"),"section",rs.getString("section"),"mode",rs.getString("mode"),"playbackPolicy",json.read(rs.getString("playback_policy")),"navigationPolicy",rs.getString("navigation_policy")),section,mode);
    }
    @Transactional(noRollbackFor=ApiException.class)
    public SessionView create(UUID owner,UUID blueprint,String mode,String key) {
        checkKey(key);
        if(!Set.of("PRACTICE","SECTION_TEST").contains(mode)) throw new ApiException(422,"INVALID_MODE","Invalid session mode.");
        // Serialize creations per owner so concurrent retries create exactly one session.
        jdbc.queryForObject("SELECT id FROM simulator.users WHERE id=? FOR UPDATE", UUID.class,owner);
        var existing=jdbc.query("SELECT id,blueprint_version_id,mode FROM simulator.exam_sessions WHERE owner_id=? AND creation_key=?",(rs,n)->Map.of("id",rs.getObject(1,UUID.class),"blueprint",rs.getObject(2,UUID.class),"mode",rs.getString(3)),owner,key);
        if(!existing.isEmpty()) {
            if(!existing.getFirst().get("blueprint").equals(blueprint) || !existing.getFirst().get("mode").equals(mode)) throw new ApiException(409,"IDEMPOTENCY_CONFLICT","Key already used with a different request.");
            return view(expire(lock(owner,(UUID)existing.getFirst().get("id"))));
        }
        var rows=jdbc.queryForList("SELECT * FROM simulator.blueprint_versions WHERE id=?",blueprint);
        if(rows.isEmpty()) throw missing();
        var row=rows.getFirst(); if(!mode.equals(row.get("mode"))) throw new ApiException(422,"INVALID_MODE","Mode must match the blueprint."); var ids=json.read(row.get("question_version_ids").toString());
        if(ids.isEmpty()) throw new ApiException(422,"EMPTY_BLUEPRINT","Blueprint contains no questions.");
        var contents=new ArrayList<JsonNode>(); var versions=new ArrayList<UUID>();
        for(var id:ids) { UUID version=UUID.fromString(id.asText()); versions.add(version); contents.add(catalog.approvedContent(version)); }
        UUID session=UUID.randomUUID(); Instant now=clock.instant(); int duration=(int)row.get("duration_seconds");
        jdbc.update("INSERT INTO simulator.exam_sessions(id,owner_id,blueprint_version_id,mode,state,started_at,expires_at,creation_key,section,playback_policy,navigation_policy) VALUES (?,?,?,?,'ACTIVE',?,?,?,?,?::jsonb,?)",session,owner,blueprint,mode,ts(now),duration==0?null:ts(now.plusSeconds(duration)),key,row.get("section"),row.get("playback_policy").toString(),row.get("navigation_policy"));
        for(int i=0;i<contents.size();i++) jdbc.update("INSERT INTO simulator.session_items(id,session_id,question_version_id,position,content_snapshot) VALUES (?,?,?,?,?::jsonb)",UUID.randomUUID(),session,versions.get(i),i,json.write(contents.get(i)));
        return view(lock(owner,session));
    }
    @Transactional(noRollbackFor=ApiException.class)
    public SessionView get(UUID owner,UUID id) { return view(expire(lock(owner,id))); }
    @Transactional(noRollbackFor=ApiException.class)
    public JsonNode save(UUID owner,UUID sessionId,UUID itemId,int expectedVersion,String key,JsonNode response) {
        checkKey(key); Session session=lock(owner,sessionId);
        JsonNode duplicate=receipt(sessionId,key,"save:"+itemId,response);
        if(duplicate!=null) return duplicate;
        session=expire(session); requireActive(session); requireVersion(session,expectedVersion);
        var contents=jdbc.query("SELECT content_snapshot FROM simulator.session_items WHERE id=? AND session_id=?",(rs,n)->json.read(rs.getString(1)),itemId,sessionId);
        if(contents.isEmpty()) throw missing(); requireNavigation(session,itemId);
        if(session.section().equals("LISTENING") && !jdbc.queryForObject("SELECT coalesce(bool_or(completed),false) FROM simulator.playback_state WHERE item_id=?",Boolean.class,itemId)) throw new ApiException(409,"AUDIO_REQUIRED","Finish listening before saving answers.");
        validateResponse(contents.getFirst(),response);
        Instant now=clock.instant();
        int revision=jdbc.queryForObject("SELECT coalesce(max(revision),0)+1 FROM simulator.responses WHERE item_id=?",Integer.class,itemId);
        jdbc.update("INSERT INTO simulator.responses(item_id,response,revision,saved_at) VALUES (?,?::jsonb,?,?) ON CONFLICT(item_id) DO UPDATE SET response=EXCLUDED.response,revision=EXCLUDED.revision,saved_at=EXCLUDED.saved_at",itemId,json.write(response),revision,ts(now));
        jdbc.update("INSERT INTO simulator.response_events(id,item_id,response,revision,saved_at) VALUES (?,?,?::jsonb,?,?)",UUID.randomUUID(),itemId,json.write(response),revision,ts(now));
        jdbc.update("UPDATE simulator.exam_sessions SET version=version+1 WHERE id=?",sessionId);
        JsonNode result=json.tree(new Saved(session.version()+1,now,now)); storeReceipt(sessionId,key,"save:"+itemId,response,result); return result;
    }
    @Transactional(noRollbackFor=ApiException.class)
    public JsonNode submit(UUID owner,UUID id,int expectedVersion,String key) {
        checkKey(key); Session session=lock(owner,id); var payload=json.tree(Map.of());
        JsonNode duplicate=receipt(id,key,"submit",payload); if(duplicate!=null) return duplicate;
        session=expire(session); requireActive(session); requireVersion(session,expectedVersion);
        Instant now=clock.instant(); jdbc.update("UPDATE simulator.exam_sessions SET state='SUBMITTED',submitted_at=?,version=version+1 WHERE id=?",ts(now),id);
        scorer.evaluate(id,now); JsonNode result=json.tree(new Closed(id,"SUBMITTED",session.version()+1,now)); storeReceipt(id,key,"submit",payload,result); return result;
    }
    @Transactional(noRollbackFor=ApiException.class)
    public JsonNode results(UUID owner,UUID id) {
        Session session=expire(lock(owner,id));
        if(session.state().equals("ACTIVE")) throw new ApiException(409,"SESSION_ACTIVE","Submit the session before viewing results.");
        return json.read(jdbc.queryForObject("SELECT result FROM simulator.evaluations WHERE session_id=?",String.class,id));
    }
    @Transactional(noRollbackFor=ApiException.class)
    public Map<String,Object> dashboard(UUID owner) {
        var pending=jdbc.query("SELECT id FROM simulator.exam_sessions WHERE owner_id=? AND state='ACTIVE' AND expires_at<=? ORDER BY id FOR UPDATE SKIP LOCKED",(rs,n)->rs.getObject(1,UUID.class),owner,ts(clock.instant()));
        pending.forEach(id->expire(lock(owner,id)));
        var metrics=jdbc.query("SELECT s.section,count(*) attempts,sum((e.result->'objectiveScore'->>'correct')::int) correct,sum((e.result->'objectiveScore'->>'total')::int) total FROM simulator.exam_sessions s JOIN simulator.evaluations e ON e.session_id=s.id WHERE s.owner_id=? GROUP BY s.section ORDER BY s.section DESC",(rs,n)->Map.<String,Object>of("section",rs.getString("section"),"attemptCount",rs.getLong("attempts"),"objectiveAccuracy",rs.getInt("total")==0?0d:(double)rs.getInt("correct")/rs.getInt("total")),owner);
        long completed=metrics.stream().mapToLong(m->((Number)m.get("attemptCount")).longValue()).sum();
        var recent=jdbc.query("SELECT s.id,s.section,s.mode,s.state,s.version,s.started_at,s.submitted_at,e.result FROM simulator.exam_sessions s LEFT JOIN simulator.evaluations e ON e.session_id=s.id WHERE s.owner_id=? ORDER BY s.started_at DESC,s.id LIMIT 20",(rs,n)->{
            var value=new LinkedHashMap<String,Object>(); value.put("sessionId",rs.getObject("id",UUID.class)); value.put("section",rs.getString("section")); value.put("mode",rs.getString("mode")); value.put("state",rs.getString("state")); value.put("startedAt",rs.getTimestamp("started_at").toInstant()); value.put("completedAt",instant(rs,"submitted_at")); value.put("objectiveScore",rs.getString("result")==null?null:json.read(rs.getString("result")).path("objectiveScore")); return value;
        },owner);
        return Map.of("completedSessions",completed,"sectionMetrics",metrics,"recentAttempts",recent);
    }
    private Session lock(UUID owner,UUID id) {
        return jdbc.query("SELECT * FROM simulator.exam_sessions WHERE id=? AND owner_id=? FOR UPDATE",(rs,n)->new Session(id,rs.getObject("blueprint_version_id",UUID.class),rs.getString("state"),rs.getInt("version"),rs.getTimestamp("started_at").toInstant(),instant(rs,"expires_at"),instant(rs,"submitted_at"),rs.getString("section"),rs.getString("mode"),json.read(rs.getString("playback_policy")),rs.getString("navigation_policy"),rs.getInt("current_position")),id,owner).stream().findFirst().orElseThrow(ExamSessions::missing);
    }
    private Session expire(Session session) {
        Instant now=clock.instant();
        if(session.state().equals("ACTIVE") && session.expires()!=null && !now.isBefore(session.expires())) {
            jdbc.update("UPDATE simulator.exam_sessions SET state='EXPIRED',submitted_at=?,version=version+1 WHERE id=?",ts(session.expires()),session.id()); scorer.evaluate(session.id(),now);
            return new Session(session.id(),session.blueprint(),"EXPIRED",session.version()+1,session.started(),session.expires(),session.expires(),session.section(),session.mode(),session.policy(),session.navigation(),session.position());
        }
        return session;
    }
    private SessionView view(Session session) {
        var items=jdbc.query("SELECT i.id,i.position,i.content_snapshot,r.response,r.saved_at FROM simulator.session_items i LEFT JOIN simulator.responses r ON r.item_id=i.id WHERE i.session_id=? ORDER BY i.position",(rs,n)->{
            boolean available=!session.state().equals("ACTIVE") || session.navigation().equals("FREE") || rs.getInt("position")==session.position();
            var content=json.read(rs.getString("content_snapshot"));
            if(!available) content=json.tree(Map.of("taskType",content.path("taskType").asText(),"title",content.path("title").asText(),"instructions","This item is locked by the navigation policy."));
            return new Item(rs.getObject("id",UUID.class),content,rs.getString("response")==null?json.tree(Map.of("answers",Map.of())):json.read(rs.getString("response")),instant(rs,"saved_at"),available);
        },session.id());
        return new SessionView(session.id(),session.state(),session.version(),session.blueprint(),session.started(),session.expires(),clock.instant(),items,session.section(),session.mode(),session.policy(),session.navigation(),session.position());
    }
    public record ItemAccess(UUID questionVersionId,JsonNode content,String state,String mode,JsonNode policy,Instant expiresAt) {}
    @Transactional(noRollbackFor=ApiException.class)
    public ItemAccess accessItem(UUID owner,UUID id,UUID itemId) {
        Session session=expire(lock(owner,id)); requireNavigation(session,itemId);
        var rows=jdbc.queryForList("SELECT question_version_id,content_snapshot FROM simulator.session_items WHERE id=? AND session_id=?",itemId,id);
        if(rows.isEmpty()) throw missing(); var row=rows.getFirst();
        if(!session.section().equals("LISTENING")) throw new ApiException(422,"NOT_LISTENING","Listening audio required.");
        return new ItemAccess((UUID)row.get("question_version_id"),json.read(row.get("content_snapshot").toString()),session.state(),session.mode(),session.policy(),session.expires());
    }
    @Transactional(noRollbackFor=ApiException.class)
    public SessionView navigate(UUID owner,UUID id,int target) {
        Session session=expire(lock(owner,id)); requireActive(session);
        int count=jdbc.queryForObject("SELECT count(*) FROM simulator.session_items WHERE session_id=?",Integer.class,id);
        if(target<0 || target>=count) throw new ApiException(422,"INVALID_POSITION","Invalid item position.");
        if(session.navigation().equals("FORWARD_ONLY") && target!=session.position()) {
            boolean complete=jdbc.queryForObject("SELECT coalesce(bool_or(p.completed),false) FROM simulator.session_items i LEFT JOIN simulator.playback_state p ON p.item_id=i.id WHERE i.session_id=? AND i.position=?",Boolean.class,id,session.position());
            if(target!=session.position()+1 || !complete) throw new ApiException(409,"NAVIGATION_RESTRICTED","Complete the current audio before advancing; backward navigation is disabled.");
        }
        if(target!=session.position()) jdbc.update("UPDATE simulator.exam_sessions SET current_position=?,version=version+1 WHERE id=?",target,id);
        return view(lock(owner,id));
    }
    @Transactional
    public void expireDueSessions() {
        var rows=jdbc.query("SELECT id,owner_id FROM simulator.exam_sessions WHERE state='ACTIVE' AND expires_at<=? ORDER BY id FOR UPDATE SKIP LOCKED",(rs,n)->Map.of("id",rs.getObject(1,UUID.class),"owner",rs.getObject(2,UUID.class)),ts(clock.instant()));
        rows.forEach(row->expire(lock(row.get("owner"),row.get("id"))));
    }
    private void requireNavigation(Session session,UUID itemId) {
        var positions=jdbc.queryForList("SELECT position FROM simulator.session_items WHERE id=? AND session_id=?",itemId,session.id());
        if(positions.isEmpty()) throw missing();
        if(session.state().equals("ACTIVE") && session.navigation().equals("FORWARD_ONLY") && (int)positions.getFirst().get("position")!=session.position()) throw new ApiException(409,"NAVIGATION_RESTRICTED","This audio group is not currently available.");
    }
    private JsonNode receipt(UUID id,String key,String operation,JsonNode payload) {
        var rows=jdbc.queryForList("SELECT operation,payload,response FROM simulator.request_receipts WHERE session_id=? AND idempotency_key=?",id,key);
        if(rows.isEmpty()) return null; var row=rows.getFirst();
        if(!operation.equals(row.get("operation")) || !payload.equals(json.read(row.get("payload").toString()))) throw new ApiException(409,"IDEMPOTENCY_CONFLICT","Key already used with a different request.");
        return json.read(row.get("response").toString());
    }
    private void storeReceipt(UUID id,String key,String operation,JsonNode payload,JsonNode response) { jdbc.update("INSERT INTO simulator.request_receipts(session_id,idempotency_key,operation,payload,response) VALUES (?,?,?,?::jsonb,?::jsonb)",id,key,operation,json.write(payload),json.write(response)); }
    private void validateResponse(JsonNode content,JsonNode response) {
        if(response==null || !response.isObject() || response.size()!=1 || !response.path("answers").isObject()) throw invalid();
        Set<String> allowed=new HashSet<>();
        if(content.path("taskType").asText().equals("COMPLETE_THE_WORDS")) content.path("segments").forEach(segment->{ if(segment.has("gapId")) allowed.add(segment.path("gapId").asText()); }); else if(content.has("questions")) content.path("questions").forEach(q->allowed.add(q.path("id").asText())); else allowed.add("choice");
        var fields=response.path("answers").fields();
        while(fields.hasNext()) {
            var entry=fields.next(); if(!allowed.contains(entry.getKey()) || !entry.getValue().isTextual() || entry.getValue().asText().length()>200) throw invalid();
            if(!content.path("taskType").asText().equals("COMPLETE_THE_WORDS") && !entry.getValue().asText().isEmpty()) {
                JsonNode options=content.path("options"); if(content.has("questions")) for(var q:content.path("questions")) if(q.path("id").asText().equals(entry.getKey())) options=q.path("options");
                boolean found=false; for(var option:options) if(option.path("id").asText().equals(entry.getValue().asText())) found=true;
                if(!found) throw invalid();
            }
        }
    }
    private void requireVersion(Session s,int version) { if(s.version()!=version) throw new ApiException(409,"VERSION_CONFLICT","Reload the session before changing answers."); }
    private void requireActive(Session s) { if(s.state().equals("EXPIRED")) throw new ApiException(410,"SESSION_EXPIRED","The server deadline has passed."); if(!s.state().equals("ACTIVE")) throw new ApiException(409,"SESSION_CLOSED","The session is closed."); }
    private void checkKey(String key) { if(key==null || !key.matches("[A-Za-z0-9_-]{8,128}")) throw new ApiException(422,"INVALID_IDEMPOTENCY_KEY","Provide an idempotency key of 8 to 128 safe characters."); }
    private static ApiException missing() { return new ApiException(404,"NOT_FOUND","Resource not found."); }
    private static ApiException invalid() { return new ApiException(422,"INVALID_RESPONSE","Response does not match the question format."); }
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
    private static Instant instant(ResultSet rs,String column) throws SQLException { Timestamp value=rs.getTimestamp(column); return value==null?null:value.toInstant(); }
}
