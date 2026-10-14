package com.toeflsimulator.examination;
import java.util.*;
import java.time.*;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.toeflsimulator.common.*;
import com.toeflsimulator.media.ObjectStorage;
import com.toeflsimulator.content.ListeningImporter;
@Service
public class ListeningPlayback {
    private final JdbcTemplate jdbc; private final ExamSessions sessions; private final Json json; private final Clock clock; private final ObjectStorage storage;
    public ListeningPlayback(JdbcTemplate jdbc,ExamSessions sessions,Json json,Clock clock,ObjectStorage storage) { this.jdbc=jdbc; this.sessions=sessions; this.json=json; this.clock=clock; this.storage=storage; }
    @Transactional(noRollbackFor=ApiException.class)
    public Map<String,Object> state(UUID owner,UUID session,UUID item) {
        var access=sessions.accessItem(owner,session,item); ensureState(item);
        var state=jdbc.queryForMap("SELECT * FROM simulator.playback_state WHERE item_id=?",item);
        var result=new LinkedHashMap<String,Object>(); result.put("plays",state.get("plays")); result.put("positionMs",state.get("position_ms")); result.put("completed",state.get("completed")); result.put("policy",policy(access)); result.put("durationMs",access.content().path("stimulus").path("durationMs").asInt());
        var grants=jdbc.queryForList("SELECT id,created_at FROM simulator.playback_grants WHERE item_id=? AND review=? AND expires_at>? ORDER BY created_at DESC LIMIT 1",item,!access.state().equals("ACTIVE"),ts(clock.instant()));
        if(!grants.isEmpty() && policy(access).path("resumeAllowed").asBoolean()) {
            if(access.state().equals("ACTIVE") && !access.policy().path("allowPause").asBoolean()) {
                long elapsed=Duration.between(((Timestamp)grants.getFirst().get("created_at")).toInstant(),clock.instant()).toMillis();
                int position=(int)Math.min(access.content().path("stimulus").path("durationMs").asInt(),Math.max(((Number)state.get("position_ms")).intValue(),elapsed));
                result.put("positionMs",position);
            }
            result.putAll(grantView(session,item,(UUID)grants.getFirst().get("id")));
        } return result;
    }
    @Transactional(noRollbackFor=ApiException.class)
    public Map<String,Object> start(UUID owner,UUID session,UUID item,String key) {
        if(key==null || !key.matches("[A-Za-z0-9_-]{8,128}")) throw new ApiException(422,"INVALID_IDEMPOTENCY_KEY","A valid playback request key is required.");
        var access=sessions.accessItem(owner,session,item); ensureState(item);
        var duplicate=jdbc.queryForList("SELECT id,review FROM simulator.playback_grants WHERE item_id=? AND request_key=?",item,key);
        if(!duplicate.isEmpty() && (boolean)duplicate.getFirst().get("review")!=!access.state().equals("ACTIVE")) throw new ApiException(409,"PLAYBACK_PHASE_CHANGED","Use a new playback key for review.");
        if(!duplicate.isEmpty()) return grantView(session,item,(UUID)duplicate.getFirst().get("id"));
        JsonNode policy=policy(access); var state=jdbc.queryForMap("SELECT * FROM simulator.playback_state WHERE item_id=? FOR UPDATE",item);
        int plays=((Number)state.get("plays")).intValue(); int max=policy.path("maxPlays").asInt();
        if(access.state().equals("ACTIVE") && max>0 && plays>=max) throw new ApiException(409,"REPLAY_LIMIT","Replay is disabled by this blueprint. Resume the existing playback when allowed.");
        UUID grant=UUID.randomUUID(); Instant now=clock.instant(); Instant expires=now.plusSeconds(3600);
        if(access.state().equals("ACTIVE") && access.expiresAt()!=null && access.expiresAt().isBefore(expires)) expires=access.expiresAt();
        jdbc.update("INSERT INTO simulator.playback_grants(id,item_id,request_key,created_at,expires_at,review) VALUES (?,?,?,?,?,?)",grant,item,key,ts(now),ts(expires),!access.state().equals("ACTIVE"));
        if(access.state().equals("ACTIVE")) jdbc.update("UPDATE simulator.playback_state SET plays=plays+1,position_ms=0,completed=false WHERE item_id=?",item);
        return grantView(session,item,grant);
    }
    @Transactional(noRollbackFor=ApiException.class)
    public Map<String,Object> progress(UUID owner,UUID session,UUID item,UUID grantId,int positionMs,boolean completed) {
        var access=sessions.accessItem(owner,session,item); var grant=grant(item,grantId,access);
        if(!access.state().equals("ACTIVE")) return state(owner,session,item);
        int duration=access.content().path("stimulus").path("durationMs").asInt();
        long elapsed=Math.max(0,Duration.between(((Timestamp)grant.get("created_at")).toInstant(),clock.instant()).toMillis());
        if(positionMs<0 || positionMs>duration+1000) throw new ApiException(422,"INVALID_PROGRESS","Invalid audio position.");
        if(!access.policy().path("allowSeek").asBoolean() && positionMs>elapsed+1500) throw new ApiException(409,"SKIP_RESTRICTED","Audio skipping is disabled.");
        int previous=jdbc.queryForObject("SELECT position_ms FROM simulator.playback_state WHERE item_id=?",Integer.class,item);
        if(!access.policy().path("allowSeek").asBoolean() && positionMs+500<previous) throw new ApiException(409,"REWIND_RESTRICTED","Audio rewind is disabled.");
        if(completed && (positionMs<duration-500 || (!access.policy().path("allowSeek").asBoolean() && elapsed<duration-500))) throw new ApiException(409,"PLAYBACK_NOT_FINISHED","The audio has not finished according to the server clock.");
        jdbc.update("UPDATE simulator.playback_state SET position_ms=?,completed=completed OR ? WHERE item_id=?",Math.min(positionMs,duration),completed,item);
        return state(owner,session,item);
    }
    @Transactional(noRollbackFor=ApiException.class)
    public byte[] audio(UUID owner,UUID session,UUID item,UUID grantId) {
        var access=sessions.accessItem(owner,session,item); grant(item,grantId,access);
        var asset=jdbc.queryForMap("SELECT a.object_key,a.sha256 FROM simulator.listening_stimuli s JOIN simulator.audio_assets a ON a.id=s.audio_asset_id WHERE s.question_version_id=?",access.questionVersionId());
        byte[] bytes=storage.get(asset.get("object_key").toString()); if(!ListeningImporter.sha(bytes).equals(asset.get("sha256"))) throw new ApiException(503,"AUDIO_INTEGRITY","Audio integrity check failed."); return bytes;
    }
    @Transactional(noRollbackFor=ApiException.class)
    public JsonNode transcript(UUID owner,UUID session,UUID item) {
        var access=sessions.accessItem(owner,session,item);
        if(access.state().equals("ACTIVE")) throw new ApiException(409,"TRANSCRIPT_LOCKED","Submit the exercise before reviewing its transcript.");
        return json.read(jdbc.queryForObject("SELECT transcript FROM simulator.listening_stimuli WHERE question_version_id=?",String.class,access.questionVersionId()));
    }
    private JsonNode policy(ExamSessions.ItemAccess access) { return access.state().equals("ACTIVE")?access.policy():json.tree(Map.of("maxPlays",0,"allowPause",true,"allowSeek",true,"resumeAllowed",true)); }
    private Map<String,Object> grant(UUID item,UUID id,ExamSessions.ItemAccess access) {
        var rows=jdbc.queryForList("SELECT * FROM simulator.playback_grants WHERE id=? AND item_id=?",id,item);
        if(rows.isEmpty()) throw new ApiException(404,"NOT_FOUND","Audio not found."); var row=rows.getFirst();
        if(!((Timestamp)row.get("expires_at")).toInstant().isAfter(clock.instant()) || (boolean)row.get("review")!=!access.state().equals("ACTIVE")) throw new ApiException(410,"AUDIO_GRANT_EXPIRED","Audio authorization expired.");
        var current=jdbc.queryForObject("SELECT id FROM simulator.playback_grants WHERE item_id=? AND review=? ORDER BY created_at DESC LIMIT 1",UUID.class,item,row.get("review"));
        if(!current.equals(id)) throw new ApiException(409,"AUDIO_GRANT_REPLACED","Use the current audio authorization."); return row;
    }
    private void ensureState(UUID item) { jdbc.update("INSERT INTO simulator.playback_state(item_id) VALUES (?) ON CONFLICT DO NOTHING",item); }
    private Map<String,Object> grantView(UUID session,UUID item,UUID grant) { return Map.of("grantId",grant,"audioUrl","/api/v1/sessions/"+session+"/items/"+item+"/audio/"+grant); }
    private static Timestamp ts(Instant value) { return Timestamp.from(value); }
}
